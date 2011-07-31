/*
 * Copyright 2009 OW2 Chameleon
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
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.JavaCore;
import org.ow2.chameleon.eclipse.ipojo.Activator;

/**
 * Utility class applying iPOJO configuration to the Manifest file
 * 
 * @author Thomas Calmant
 */
public class ManifestUpdater {

	/** Default manifest file name (MANIFEST.MF) */
	public static final String MANIFEST_NAME = "MANIFEST.MF";

	/** Default manifest parent folder (META-INF) */
	public static final String META_INF_FOLDER = "META-INF";

	/** Default metadata file name */
	public static final String METADATA_FILE = "metadata.xml";

	/**
	 * Creates an empty manifest file, to allow manifest result output
	 * 
	 * @param aProject
	 *            Current manipulated project
	 * @return A valid manifest IFile reference
	 * @throws CoreException
	 *             An error occurred while creating the manifest file or parent
	 *             folder
	 */
	protected IFile createDefaultManifest(final IProject aProject)
			throws CoreException {

		// Create the folder
		IFolder metaInf = aProject.getFolder(META_INF_FOLDER);
		if (!metaInf.exists()) {
			metaInf.create(true, false, null);
		}

		// Prepare the input
		ByteArrayOutputStream manifestOutstream = new ByteArrayOutputStream();
		Manifest emptyManifest = new Manifest();

		// To be valid, a manifest must contain a Manifest-Version attribute
		emptyManifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION,
				"1.0");

		try {
			emptyManifest.write(manifestOutstream);

		} catch (IOException ex) {
			// Ignore, this may never happen
			Activator.logWarning("Unable to prepare the new Manifest content",
					ex);
		}

		// Create the file
		IFile manifestIFile = metaInf.getFile(MANIFEST_NAME);
		manifestIFile.create(
				new ByteArrayInputStream(manifestOutstream.toByteArray()),
				true, null);

		return manifestIFile;
	}

	/**
	 * Search for the given file
	 * 
	 * @param aRoot
	 *            Root container to look in
	 * @param aFileName
	 *            File to look for
	 * @return The File, or null if not found
	 * @throws CoreException
	 *             An error occurred while reading members list (mainly on
	 *             remote files)
	 */
	private IFile findFile(final IContainer aRoot, final String aFileName) {

		if (aRoot == null || aFileName == null || aFileName.isEmpty()) {
			return null;
		}

		IResource[] members;
		try {
			members = aRoot.members();
		} catch (CoreException ex) {
			Activator.logError("Error searching for file '" + aFileName + "'",
					ex);
			return null;
		}

		if (members == null) {
			return null;
		}

		for (IResource resource : members) {

			if (resource.getName().equalsIgnoreCase(aFileName)) {
				if (resource.getType() == IResource.FILE) {
					return (IFile) resource;
				}

			} else if (resource.getType() == IResource.FOLDER) {
				IFile found = findFile((IContainer) resource, aFileName);
				if (found != null) {
					return found;
				}
			}
		}

		return null;
	}

	/**
	 * Verifies the Nature of the given project
	 * 
	 * @param aProject
	 *            Project to be tested
	 * @return True if the project is of JavaCore nature
	 */
	private boolean isJavaProject(final IProject aProject) {
		try {
			return aProject.hasNature(JavaCore.NATURE_ID);
		} catch (Exception ex) {
			Activator.logError("Error retrieving project nature", ex);
			return false;
		}
	}

	/**
	 * Applies a full iPOJO update on the project Manifest.
	 * 
	 * @param aProject
	 *            Eclipse Java project containing the Manifest
	 * @throws FileNotFoundException
	 *             No manifest found
	 */
	public void updateManifest(final IProject aProject)
			throws FileNotFoundException {
		updateManifest(aProject, null);
	}

	/**
	 * Applies iPOJO update on the project Manifest. Manifest file must be
	 * refreshed after calling this method
	 * 
	 * @param aProject
	 *            Parent project of the Manifest
	 * @param delta
	 * @throws FileNotFoundException
	 *             No Manifest or no source path found
	 */
	public void updateManifest(final IProject aProject,
			final List<IResource> delta) throws FileNotFoundException {

		if (!isJavaProject(aProject)) {
			Activator.logInfo("Not a Java project");
			return;
		}

		IWorkspaceRoot workspaceRoot = aProject.getWorkspace().getRoot();

		// Search for the Manifest
		IFile manifestIFile = findFile(aProject, MANIFEST_NAME);
		if (manifestIFile == null) {
			Activator.logInfo("Manifest file not found. Creating one.");

			// Try to create a brand new one
			try {
				manifestIFile = createDefaultManifest(aProject);

			} catch (CoreException ex) {
				Activator.logError("Can't create a manifest file", ex);
				return;
			}
		}

		// Conversion to iPOJO understandable file
		CompositeFile manifestFile = new CompositeFile(workspaceRoot,
				manifestIFile);

		// Search for Metadata.xml file
		CompositeFile metadataFile = null;
		IFile metadataIFile = findFile(aProject, METADATA_FILE);
		if (metadataIFile == null) {
			Activator
					.logInfo("No metadata.xml file found (only annotations will be parsed)");

		} else {

			// Convert the IFile to an iPOJO understandable File
			metadataFile = new CompositeFile(workspaceRoot, metadataIFile);
		}

		// iPOJO application
		try {
			EclipsePojoization pojo = new EclipsePojoization(delta);
			if (pojo.directoryPojoization(aProject, metadataFile, manifestFile)) {
				Activator.logInfo("iPOJO transformation done ("
						+ aProject.getName() + ")");
			} else {
				Activator.logError(
						"iPOJO transformation failed (" + aProject.getName()
								+ ")", null);
			}
		} catch (Exception ex) {
			Activator.logError("iPOJO manipulation error", ex);
		}

		// Refresh UI
		try {
			aProject.refreshLocal(IResource.DEPTH_INFINITE, null);

		} catch (CoreException ex) {
			Activator.logWarning("Project refresh error", ex);
		}
	}
}
