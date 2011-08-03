package org.ow2.chameleon.eclipse.ipojo.core;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.jar.Manifest;

import org.apache.felix.ipojo.manipulator.ResourceStore;
import org.apache.felix.ipojo.manipulator.ResourceVisitor;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.ow2.chameleon.eclipse.ipojo.Activator;

public class EclipseResourceStore implements ResourceStore {

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
		IJavaProject pJavaProject = (IJavaProject) aProject
				.getNature(JavaCore.NATURE_ID);

		pOutputLocation = pJavaProject.getOutputLocation();
	}

	@Override
	public void accept(final ResourceVisitor visitor) {

		try {
			IFolder outputFolder = pWorkspaceRoot.getFolder(pOutputLocation);
			visitFolder(outputFolder, visitor);

		} catch (CoreException e) {
			Activator.logError(pProject,
					"Can't visit the binary output folder", e);
		}
	}

	@Override
	public void close() throws IOException {
		// Nothing to release / to do
	}

	@Override
	public void open(final Manifest manifest) throws IOException {

		// Sort the manifest keys
		Utilities.INSTANCE.makeSortedManifest(pProject, manifest);

		// Write the file
		IFile manifestFile = Utilities.INSTANCE.findFile(pProject,
				Utilities.MANIFEST_NAME);
		if (manifestFile == null) {
			try {
				manifestFile = Utilities.INSTANCE
						.createDefaultManifest(pProject);

			} catch (CoreException e) {
				throw new IOException("Can't find the Manifest file", e);
			}
		}

		// Write the manifest in memory
		ByteArrayOutputStream byteOutStream = new ByteArrayOutputStream();
		manifest.write(byteOutStream);

		// Convert to an input stream
		ByteArrayInputStream byteInStream = new ByteArrayInputStream(
				byteOutStream.toByteArray());

		try {
			// Update the file content
			manifestFile.setContents(byteInStream, IResource.FORCE, null);

		} catch (CoreException e) {
			throw new IOException("Can't write in the Manifest file", e);
		}
	}

	@Override
	public byte[] read(final String path) throws IOException {

		// Compute the file path
		IFile file = pWorkspaceRoot.getFile(pOutputLocation.append(path));

		try {
			// Return its content
			return Utilities.INSTANCE.inputStreamToBytes(file.getContents());

		} catch (CoreException e) {
			throw new IOException("An error occurred while reading the file '"
					+ file + "'", e);
		}
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

		for (IResource resource : aContainer.members()) {

			if (resource instanceof IContainer) {
				// Recursive visit
				visitFolder((IContainer) resource, aVisitor);

			} else if (resource instanceof IFile) {
				// Make a relative path
				IPath path = resource.getFullPath();
				aVisitor.visit(path.makeRelativeTo(pOutputLocation).toString());
			}
		}
	}

	@Override
	public void write(final String resourcePath, final byte[] resource)
			throws IOException {

		// Compute the file path
		IFile file = pWorkspaceRoot.getFile(pOutputLocation
				.append(resourcePath));

		// Prepare the input stream
		ByteArrayInputStream byteStream = new ByteArrayInputStream(resource);

		if (!file.exists()) {
			// Create the file
			try {
				Utilities.INSTANCE.mkdirs(file.getParent());
				file.create(byteStream, true, null);

			} catch (CoreException e) {
				throw new IOException("Could not create the file '" + file
						+ "'", e);
			}

		} else {
			// Set the file content
			try {
				file.setContents(byteStream, IResource.FORCE, null);

			} catch (CoreException e) {
				throw new IOException("Couldn't set the file content '" + file
						+ "'", e);
			}
		}
	}
}
