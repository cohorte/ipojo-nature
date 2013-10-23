/*
 * Copyright 2013 OW2 Chameleon
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ow2.chameleon.eclipse.ipojo.core;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.jar.Manifest;

import org.apache.felix.ipojo.manipulator.ResourceStore;
import org.apache.felix.ipojo.manipulator.ResourceVisitor;
import org.apache.felix.ipojo.manipulator.store.ManifestBuilder;
import org.apache.felix.ipojo.manipulator.util.Metadatas;
import org.apache.felix.ipojo.metadata.Element;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.ow2.chameleon.eclipse.ipojo.Activator;

/**
 * Eclipse resource store interface for the iPOJO Manipulator
 * 
 * @author Thomas Calmant
 */
public class EclipseResourceStore implements ResourceStore {

	/** Base progress monitor */
	private SubMonitor pBaseMonitor;

	/** Current progress monitor */
	private IProgressMonitor pCurrentMonitor;

	/** The bundle manifest */
	private Manifest pManifest;

	/** The manifest builder */
	private ManifestBuilder pManifestBuilder;

	/** Number of {@link #writeMetadata(Element)} calls, for progress monitor */
	private int pNbStoredMetadata;

	/** Project output directory, relative to the project's workspace */
	private final IPath pOutputLocation;

	/** Current manipulated project */
	private final IProject pProject;

	/** Current project workspace root */
	private final IWorkspaceRoot pWorkspaceRoot;

	/**
	 * Prepares the resource store
	 * 
	 * @param aProject
	 *            An iPOJO Java project
	 * @throws CoreException
	 *             The project is not of Java nature
	 */
	public EclipseResourceStore(final IProject aProject) throws CoreException {

		pProject = aProject;
		pWorkspaceRoot = pProject.getWorkspace().getRoot();

		// Get the output location
		final IJavaProject pJavaProject = (IJavaProject) aProject
				.getNature(JavaCore.NATURE_ID);

		pOutputLocation = pJavaProject.getOutputLocation();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.felix.ipojo.manipulator.ResourceStore#accept(org.apache.felix
	 * .ipojo.manipulator.ResourceVisitor)
	 */
	@Override
	public void accept(final ResourceVisitor aVisitor) {

		try {
			final IFolder outputFolder = pWorkspaceRoot
					.getFolder(pOutputLocation);

			// Count files
			final int nbFiles = countFiles(outputFolder);

			// Prepare the read monitor
			pNbStoredMetadata = 0;
			pCurrentMonitor = SubMonitor.convert(pBaseMonitor.newChild(1),
					nbFiles);
			pCurrentMonitor.setTaskName("Read class files");

			// Visit the folder
			visitFolder(outputFolder, aVisitor);

		} catch (final CoreException e) {
			Activator.logError(pProject,
					"Can't visit the binary output folder", e);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.felix.ipojo.manipulator.ResourceStore#close()
	 */
	@Override
	public void close() throws IOException {
		// Nothing to release / to do
	}

	/**
	 * Recursively count the number of files in a container
	 * 
	 * @param aContainer
	 *            A container
	 * @return The number of files in the container
	 */
	protected int countFiles(final IContainer aContainer) {

		int nbMembers = 0;

		try {
			for (final IResource member : aContainer.members()) {

				if (member instanceof IContainer) {
					nbMembers += countFiles((IContainer) member);

				} else if (member instanceof IFile) {
					nbMembers++;
				}
			}

		} catch (final CoreException e) {
			Activator.logError(pProject, "Error counting members", e);
		}

		return nbMembers;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.felix.ipojo.manipulator.ResourceStore#open()
	 */
	@Override
	public void open() throws IOException {

		if (pCurrentMonitor.isCanceled()) {
			// Test cancellation
			return;
		}

		// Prepare the write part monitor
		pCurrentMonitor = SubMonitor.convert(pBaseMonitor.newChild(1),
				pNbStoredMetadata);
		pCurrentMonitor.setTaskName("Write manipulated files");

		// Update manifest
		final Manifest updateManifest = pManifestBuilder.build(pManifest);

		// Sort the manifest keys
		Utilities.INSTANCE.makeSortedManifest(pProject, updateManifest);

		// Write the file
		try {
			Utilities.INSTANCE.setManifestContent(pProject, updateManifest);

		} catch (final CoreException ex) {
			Activator.logError(pProject, "Error writing the manifest file", ex);
			throw new IOException("Can't write the manifest file", ex);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.felix.ipojo.manipulator.ResourceStore#read(java.lang.String)
	 */
	@Override
	public byte[] read(final String aPath) throws IOException {

		// Compute the file path
		final IFile file = pWorkspaceRoot
				.getFile(pOutputLocation.append(aPath));

		try {
			// Return its content
			return Utilities.INSTANCE.inputStreamToBytes(file.getContents());

		} catch (final CoreException e) {
			throw new IOException("An error occurred while reading the file '"
					+ file + "'", e);
		}
	}

	/**
	 * Sets the initial bundle manifest content
	 * 
	 * @param aManifest
	 *            The bundle manifest
	 */
	public void setManifest(final Manifest aManifest) {
		pManifest = aManifest;
	}

	/**
	 * Sets the manifest builder to use
	 * 
	 * @param aBuilder
	 *            A manifest builder
	 */
	public void setManifestBuilder(final ManifestBuilder aBuilder) {
		pManifestBuilder = aBuilder;
	}

	/**
	 * Sets the base progress monitor
	 * 
	 * @param aMonitor
	 *            Base progress monitor
	 */
	public void setProgressMonitor(final IProgressMonitor aMonitor) {
		pBaseMonitor = SubMonitor.convert(aMonitor, 2);
	}

	/**
	 * Recursively visits the project binary output folder
	 * 
	 * @param aContainer
	 *            Container to visit
	 * @param aVisitor
	 *            Manipulator resource visitor
	 * @throws CoreException
	 *             An error occurred while retrieving a folder content
	 */
	protected void visitFolder(final IContainer aContainer,
			final ResourceVisitor aVisitor) throws CoreException {

		if (pCurrentMonitor.isCanceled()) {
			// Test cancellation
			return;
		}

		for (final IResource resource : aContainer.members()) {

			if (resource instanceof IContainer) {
				// Recursive visit
				visitFolder((IContainer) resource, aVisitor);

			} else if (resource instanceof IFile) {
				// Make a relative path
				final IPath path = resource.getFullPath();
				aVisitor.visit(path.makeRelativeTo(pOutputLocation).toString());

				// File handled
				pCurrentMonitor.worked(1);
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.felix.ipojo.manipulator.ResourceStore#write(java.lang.String,
	 * byte[])
	 */
	@Override
	public void write(final String aPath, final byte[] aResourceContent)
			throws IOException {

		if (pCurrentMonitor.isCanceled()) {
			// Test cancellation
			return;
		}

		// Compute the file path
		final IFile file = pWorkspaceRoot
				.getFile(pOutputLocation.append(aPath));

		// Prepare the input stream
		final ByteArrayInputStream byteStream = new ByteArrayInputStream(
				aResourceContent);

		if (!file.exists()) {
			// Create the file
			try {
				Utilities.INSTANCE.mkdirs(file.getParent());
				file.create(byteStream, true, null);

			} catch (final CoreException e) {
				throw new IOException("Could not create the file '" + file
						+ "'", e);
			}

		} else {
			// Set the file content
			try {
				file.setContents(byteStream, IResource.FORCE, null);

			} catch (final CoreException e) {
				throw new IOException("Couldn't set the file content '" + file
						+ "'", e);
			}
		}

		// Done
		pCurrentMonitor.worked(1);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.felix.ipojo.manipulator.ResourceStore#writeMetadata(org.apache
	 * .felix.ipojo.metadata.Element)
	 */
	@Override
	public void writeMetadata(final Element aMetadata) {

		pManifestBuilder.addMetada(Collections.singletonList(aMetadata));
		pManifestBuilder.addReferredPackage(Metadatas
				.findReferredPackages(aMetadata));

		pNbStoredMetadata++;
	}
}
