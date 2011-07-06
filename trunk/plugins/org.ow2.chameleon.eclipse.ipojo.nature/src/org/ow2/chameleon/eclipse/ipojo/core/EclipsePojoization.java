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
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.List;
import java.util.TreeMap;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.apache.felix.ipojo.manipulator.Pojoization;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.ow2.chameleon.eclipse.ipojo.Activator;

/**
 * Pojoization bridge between iPOJO and Eclipse worlds.
 * 
 * Based on fr.imag.adele.cadse.builder.iPojo.EclipsePojoization from the CADSE
 * project {@link http://code.google.com/a/eclipselabs.org/p/cadse/}, by the
 * Adele Team/LIG/Grenoble University, France {@link http://www-adele.imag.fr}
 * 
 * @author Stéphane Chomat
 * @author Thomas Calmant - Transformation with a patched Pojoization class
 */
public class EclipsePojoization extends Pojoization {

	/** Manifest.map field */
	private static Field sEntriesField;

	/** Manifest.attr field */
	private static Field sAttrField;

	/** True if an error occurred */
	private boolean pErrorCaught;

	/** Java Nature of the current project */
	private IJavaProject pJavaProject;

	/** Manifest file */
	private CompositeFile pManifestFile;

	/** Current working project */
	private IProject pProject;

	/** Handled files */
	private List<IResource> pResources;

	/**
	 * Prepares the builder, without specified handled resources
	 */
	public EclipsePojoization() {
		super();
		pResources = null;
	}

	/**
	 * Prepares the builder with the specified resource handling list
	 * 
	 * @param aResourceList
	 *            Handled resources list
	 */
	public EclipsePojoization(final List<IResource> aResourceList) {
		super();
		pResources = aResourceList;
	}

	/**
	 * Eclipse version of
	 * {@link Pojoization#directoryPojoization(File, File, File)}
	 * 
	 * @param aOutClassesFolder
	 *            Project binary output directory (mandatory)
	 * @param aMetadataFile
	 *            Metadata.xml file (can be null)
	 * @param aManifestFile
	 *            Manifest.mf file (can't be null, created if inexistent)
	 * @return True if the pojoization was done without error, False on error
	 */
	public boolean directoryPojoization(final IProject aProject,
			final CompositeFile aMetadataFile, final CompositeFile aManifestFile) {

		pErrorCaught = false;
		pProject = aProject;
		pManifestFile = aManifestFile;

		IPath outputLocation;
		try {
			pJavaProject = (IJavaProject) aProject
					.getNature(JavaCore.NATURE_ID);
			outputLocation = pJavaProject.getOutputLocation();
		} catch (CoreException ex) {
			error("Error retrieving java project nature : " + ex);

			pJavaProject = null;
			return false;
		}

		// Get output path, in an Eclipse behavior
		final CompositeFile classesOutputFolder = new CompositeFile(aProject
				.getWorkspace().getRoot(), outputLocation);

		// Do the "pojoization"
		super.directoryPojoization(classesOutputFolder, aMetadataFile,
				aManifestFile);

		return !pErrorCaught;
	}

	/**
	 * Prints out an error message
	 * 
	 * @param aErrorMessage
	 *            The error message
	 * 
	 * @see org.apache.felix.ipojo.manipulator.Pojoization#error(java.lang.String)
	 */
	@Override
	protected void error(final String aErrorMessage) {
		super.error(aErrorMessage);
		Activator.logError("iPOJO Manipulator error : " + aErrorMessage, null);
		pErrorCaught = true;
	}

	/**
	 * Prints out an error message
	 * 
	 * @param aErrorMessage
	 *            The error message
	 * 
	 * @param aException
	 *            The associated exception
	 * 
	 * @see org.apache.felix.ipojo.manipulator.Pojoization#error(java.lang.String)
	 */
	protected void error(final String aErrorMessage, final Exception aException) {
		super.error(aErrorMessage);

		Activator.logError("iPOJO Manipulator error : " + aErrorMessage,
				aException);
		aException.printStackTrace();
		pErrorCaught = true;
	}

	/**
	 * Return a byte array that contains the byte code of the given class name.
	 * 
	 * @param classname
	 *            name of a class to be read
	 * @return a byte array
	 * @throws IOException
	 *             if the class file can't be read
	 */
	@Override
	protected byte[] getBytecode(String aClassName) throws IOException {

		if (!aClassName.startsWith("/")) {
			try {
				aClassName = pJavaProject.getOutputLocation()
						.append(aClassName).toPortableString();
			} catch (JavaModelException e) {
				error("Can't get Java ouput folder", e);
				throw new IOException("Can't find folder", e);
			}
		}

		// Workspace relative full path of the .class file
		final IPath classpath = Path.fromPortableString(aClassName);

		// Real file (Workspace path + file path)
		IFile origF = pProject.getWorkspace().getRoot().getFile(classpath);

		if (!origF.exists()) {
			String message = "The component " + aClassName
					+ " is declared but not in directory here : "
					+ origF.getFullPath().toOSString();

			Activator.logError(message, null);
			throw new IOException(message);
		}

		try {
			// Seems necessary to avoid retrieving an empty file content
			origF.refreshLocal(IResource.DEPTH_ZERO, null);

		} catch (CoreException e1) {
			Activator.logError("Can't refresh file '" + origF.getName() + "'",
					e1);
		}

		// Read the file
		InputStream inputStream = null;
		try {
			inputStream = origF.getContents();

			byte[] bytes = new byte[inputStream.available()];

			if (bytes.length == 0) {
				error("Empty class !");
				throw new IOException("Empty class");
			}

			inputStream.read(bytes);
			return bytes;

		} catch (IOException e) {
			error("Error reading class file", e);
			throw e;

		} catch (CoreException e) {
			error("Error reading class file", e);
			throw new IOException(e.getMessage(), e);

		} finally {
			try {
				if (inputStream != null) {
					inputStream.close();
				}
			} catch (Exception e) {
				// Do nothing
			}
		}
	}

	/**
	 * Retrieves the Manifest file of the project
	 * 
	 * @return The manifest file input stream, null on opening error
	 * @throws IOException
	 *             Error while reading the file
	 * @see org.apache.felix.ipojo.manipulator.Pojoization#getManifestInputStream()
	 */
	@Override
	protected InputStream getManifestInputStream() throws IOException {
		try {
			return pManifestFile.getInputStream();

		} catch (CoreException ex) {
			error("Error reading Manifest", ex);
			return null;
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
	protected void makeSortedManifest(final Manifest aManifest) {

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
				Activator.logError("Can't find the Manifest attribute", ex);
				return;
			}

			// Change fields
			try {
				sEntriesField.set(aManifest, sortedEntries);
				sAttrField.set(aManifest, sortedAttributes);

			} catch (IllegalArgumentException e) {
				Activator.logError("Bad type of Manifest attribute", e);
			} catch (IllegalAccessException e) {
				Activator.logError("Bad access to Manifest attribute", e);
			}
		}
	}

	/**
	 * Searches for .class files in the given directory (recursive). If no
	 * directory is given, then tries to use the Java project output folder.
	 * 
	 * @param aDirectory
	 *            Directory to read (can be null)
	 * @param aClassNameList
	 *            A list that will contain all class files names
	 * 
	 * @see org.apache.felix.ipojo.manipulator.Pojoization#searchClassFiles(File,List)
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	protected void searchClassFiles(final File aDirectory,
			final List aClassNameList) {

		if (aClassNameList == null) {
			error("[searchClassFiles] Null output class list");
			return;
		}

		if (pResources != null) {
			// Only manipulate indicated files
			for (IResource resource : pResources) {

				// Only handle accessible files
				if (resource.getType() == IResource.FILE
						&& resource.isAccessible()) {

					aClassNameList.add(resource.getFullPath()
							.toPortableString());
				}
			}

		} else {
			// Work on any class file
			IContainer directory;

			if (aDirectory == null) {
				// No specified directory : try the output one
				try {
					directory = (IContainer) pProject.getWorkspace().getRoot()
							.findMember(pJavaProject.getOutputLocation());

				} catch (JavaModelException e) {
					error("Can't retrieve Java project output location", e);
					return;
				}

			} else {

				// Find all .class files
				directory = (IContainer) pProject.getWorkspace().getRoot()
						.findMember(aDirectory.getPath());
			}

			if (directory == null) {
				error("No Java project output location found...");
				return;
			}

			searchClassFiles(directory, aClassNameList);
		}
	}

	/**
	 * Eclipse version of
	 * {@link EclipsePojoization#searchClassFiles(File, List)}
	 * 
	 * @param aContainer
	 *            The resource container (directory)
	 * @param aClassNameList
	 *            The list to be filled with found class files names
	 * 
	 * @see EclipsePojoization#searchClassFiles(File, List)
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void searchClassFiles(final IContainer aContainer,
			final List aClassNameList) {

		IResource[] members;

		try {
			members = aContainer.members();

		} catch (CoreException e) {
			error("Error listing members in : "
					+ aContainer.getFullPath().toOSString(), e);
			return;
		}

		for (IResource member : members) {

			if (member.getType() == IResource.FILE && member.isAccessible()
					&& member.getName().endsWith(".class")) {

				aClassNameList.add(member.getFullPath().toPortableString());

			} else if (member instanceof IContainer) {

				searchClassFiles((IContainer) member, aClassNameList);
			}
		}
	}

	/**
	 * Writes the .class raw data to the given file.
	 * 
	 * @param classFile
	 *            Output .class file
	 * @param rawClass
	 *            Raw class representation
	 * 
	 * @throws An
	 *             error occurred while using a temporary
	 *             {@link ByteArrayInputStream}
	 * 
	 * @see org.apache.felix.ipojo.manipulator.Pojoization#setBytecode(java.io.File,
	 *      byte[])
	 */
	@Override
	protected void setBytecode(final File classFile, final byte[] rawClass)
			throws IOException {

		IFile pojoizedFile = pProject.getWorkspace().getRoot()
				.getFile(new Path(classFile.getPath()));

		ByteArrayInputStream rawClassStream = new ByteArrayInputStream(rawClass);

		try {
			if (pojoizedFile.exists()) {
				pojoizedFile.setContents(rawClassStream, true, false, null);

			} else {
				pojoizedFile.create(rawClassStream, true, null);

				// Set the pojoized file as derived resource if we created it
				pojoizedFile.setDerived(true, null);
			}

		} catch (CoreException ex) {
			error("Error writing new .class file", ex);
		}

		rawClassStream.close();
	}

	/**
	 * Writes the manifest file content.
	 * 
	 * @param aManifest
	 *            Manifest file representation
	 * 
	 * @throws IOException
	 *             An error occurred while writing the Manifest file
	 * 
	 * @see org.apache.felix.ipojo.manipulator.Pojoization#writeManifest(java.util.jar.Manifest)
	 */
	@Override
	protected void writeManifest(final Manifest aManifest) throws IOException {

		// Sort the manifest
		makeSortedManifest(aManifest);

		// Transform it into a byte array
		ByteArrayOutputStream outStream = new ByteArrayOutputStream();
		aManifest.write(outStream);

		try {
			if (!pManifestFile.exists()) {
				// Don't set the derived flag : this file should have been there
				// before
				pManifestFile.createNewFile();
			}

			pManifestFile.setContent(new ByteArrayInputStream(outStream
					.toByteArray()));

		} catch (CoreException ex) {
			error("Error writing Manifest file", ex);
		}

		outStream.close();
	}
}
