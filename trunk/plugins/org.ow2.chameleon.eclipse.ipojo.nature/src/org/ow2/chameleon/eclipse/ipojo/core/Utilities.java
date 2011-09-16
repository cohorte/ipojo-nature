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
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.TreeMap;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.JavaCore;
import org.ow2.chameleon.eclipse.ipojo.Activator;

/**
 * Utility methods for the iPOJO Builder
 * 
 * @author Thomas Calmant
 */
public final class Utilities {

	/** Singleton */
	public static final Utilities INSTANCE = new Utilities();

	/** Default manifest file name (MANIFEST.MF) */
	public static final String MANIFEST_NAME = "MANIFEST.MF";

	/** Default manifest parent folder (META-INF) */
	public static final String META_INF_FOLDER = "META-INF";

	/** Default metadata file name */
	public static final String METADATA_FILE = "metadata.xml";

	/** Manifest.attr field */
	private static Field sAttrField;

	/** Manifest.map field */
	private static Field sEntriesField;

	/**
	 * Hidden constructor
	 */
	private Utilities() {
		// Hidden constructor
	}

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
			Activator.logWarning(aProject,
					"Unable to prepare the new Manifest content", ex);
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
	protected IFile findFile(final IContainer aRoot, final String aFileName) {

		if (aRoot == null || aFileName == null || aFileName.isEmpty()) {
			return null;
		}

		IResource[] members;
		try {
			members = aRoot.members();

		} catch (CoreException ex) {
			Activator.logError(aRoot.getProject(), "Error searching for file '"
					+ aFileName + "'", ex);
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
	 * Reads the manifest file content and retrieves it as a Manifest object
	 * 
	 * @param aProject
	 *            Current manipulated project
	 * @return The manifest object
	 * @throws CoreException
	 *             An error occurred while reading the file
	 */
	public Manifest getManifestContent(final IProject aProject)
			throws CoreException {

		final IFile manifestFile = getManifestFile(aProject, true);

		try {
			return new Manifest(manifestFile.getContents(true));

		} catch (IOException e) {
			// Propagate the error
			IStatus exceptionStatus = new Status(IStatus.WARNING,
					Activator.PLUGIN_ID, "Couldn't read the manifest content",
					e);
			throw new CoreException(exceptionStatus);
		}
	}

	/**
	 * Retrieves a reference to the manifest file. Try to create if needed.
	 * 
	 * @param aProject
	 *            Current manipulated project
	 * @param aForce
	 *            Creates the Manifest file if it doesn't exist yet
	 * @return A reference to the manifest file, null if the file doesn't exist
	 *         and aForce is false
	 */
	public IFile getManifestFile(final IProject aProject, final boolean aForce)
			throws CoreException {

		// Search for the Manifest file
		final IFile manifestFile = findFile(aProject, MANIFEST_NAME);
		if (manifestFile == null && aForce) {
			Activator.logInfo(aProject,
					"Manifest file not found. Creating one.");

			// Try to create a brand new one
			return createDefaultManifest(aProject);
		}

		return manifestFile;
	}

	/**
	 * Retrieves an input stream from the metadata file, null on error
	 * 
	 * @param aProject
	 *            Current manipulated project
	 * @return An input stream to read the metadata file, null on error
	 */
	protected InputStream getMetadataStream(final IProject aProject) {

		final IFile metadataFile = findFile(aProject, METADATA_FILE);

		if (metadataFile == null) {
			// No metadata.xml file
			Activator
					.logInfo(aProject,
							"No metadata.xml file found (only annotations will be parsed)");

			return null;
		}

		// Try to get an input stream
		try {
			return metadataFile.getContents(true);

		} catch (CoreException ex) {
			// Error opening file
			Activator.logError(aProject,
					"Can't read the contents of the metadata file", ex);
		}

		return null;
	}

	/**
	 * Reads the given input stream and returns its content as a byte array
	 * 
	 * @param aInputStream
	 *            Input stream
	 * @return
	 * @throws IOException
	 */
	public byte[] inputStreamToBytes(final InputStream aInputStream)
			throws IOException {

		final ArrayList<Byte> fileBytes = new ArrayList<Byte>();

		byte[] buffer = new byte[8192];
		int readBytes = -1;
		do {
			// Fill the buffer
			readBytes = aInputStream.read(buffer);

			// Add what has been read
			for (int i = 0; i < readBytes; i++) {
				fileBytes.add(buffer[i]);
			}

		} while (readBytes != -1);

		// Convert the array
		byte[] result = new byte[fileBytes.size()];
		int i = 0;
		for (Byte readByte : fileBytes) {
			result[i++] = readByte;
		}

		return result;
	}

	/**
	 * Verifies the Nature of the given project
	 * 
	 * @param aProject
	 *            Project to be tested
	 * @return True if the project is of JavaCore nature
	 */
	public boolean isJavaProject(final IProject aProject) {

		try {
			return aProject.hasNature(JavaCore.NATURE_ID);
		} catch (Exception ex) {
			Activator.logError(aProject, "Error retrieving project nature", ex);
			return false;
		}
	}

	/**
	 * Modifies the given Manifest object to use sorted entries and attributes.
	 * Uses reflection to do it, so it may not work in some cases (security
	 * accesses, ...)
	 * 
	 * @param aManifest
	 *            The manifest object to be modified
	 */
	public void makeSortedManifest(final IProject aProject,
			final Manifest aManifest) {

		synchronized (aManifest) {

			// Prepare the sorted attributes object
			SortedAttributes sortedAttributes = new SortedAttributes(
					aManifest.getMainAttributes());

			// Prepare the sorted entry
			TreeMap<String, Attributes> sortedEntries = new TreeMap<String, Attributes>(
					aManifest.getEntries());

			try {
				// Get the map field
				if (sEntriesField == null) {
					sEntriesField = Manifest.class.getDeclaredField("entries");
					sEntriesField.setAccessible(true);
				}

				// Get the attr field
				if (sAttrField == null) {
					sAttrField = Manifest.class.getDeclaredField("attr");
					sAttrField.setAccessible(true);
				}
			} catch (NoSuchFieldException ex) {
				Activator.logError(aProject,
						"Can't find the Manifest attribute", ex);
				return;
			}

			// Change fields
			try {
				sEntriesField.set(aManifest, sortedEntries);
				sAttrField.set(aManifest, sortedAttributes);

			} catch (IllegalArgumentException e) {
				Activator.logError(aProject, "Bad type of Manifest attribute",
						e);

			} catch (IllegalAccessException e) {
				Activator.logError(aProject,
						"Bad access to Manifest attribute", e);
			}
		}
	}

	/**
	 * Creates the given folder and its parents in the current project
	 * 
	 * @param aContainer
	 *            Folder to be created
	 * @throws CoreException
	 *             An error occurred during folder creation
	 */
	public void mkdirs(final IContainer aContainer) throws CoreException {

		IContainer parent = aContainer.getParent();
		if (parent instanceof IFolder) {
			mkdirs(parent);
		}

		if (aContainer instanceof IFolder && !aContainer.exists()) {
			((IFolder) aContainer).create(true, true, null);
		}
	}

	/**
	 * Sets the project manifest file content
	 * 
	 * @param aProject
	 *            Project currently modified
	 * @param aManifest
	 *            The new manifest content
	 * @throws CoreException
	 *             An error occurred while writing down the file
	 */
	public void setManifestContent(final IProject aProject,
			final Manifest aManifest) throws CoreException {

		final IFile manifestFile = getManifestFile(aProject, true);

		System.out.println("Compo : "
				+ aManifest.getMainAttributes().getValue("iPOJO-Components"));

		// Write the manifest in memory
		final ByteArrayOutputStream byteOutStream = new ByteArrayOutputStream();
		try {
			aManifest.write(byteOutStream);

		} catch (IOException e) {
			// Should never happen
			throw new CoreException(new Status(IStatus.ERROR,
					Activator.PLUGIN_ID, "Can't write the manifest in memory",
					e));
		}

		// Convert to an input stream
		final ByteArrayInputStream byteInStream = new ByteArrayInputStream(
				byteOutStream.toByteArray());

		// Update the file content
		manifestFile.setContents(byteInStream, IResource.FORCE, null);
	}
}
