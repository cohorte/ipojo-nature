/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 * Copyright (C) 2006-2010 Adele Team/LIG/Grenoble University, France
 * Copyright (C) 2010 isandlaTech, France
 */
package fr.imag.adele.cadse.builder.iPojo;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
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
import org.psem2m.eclipse.ipojo.Activator;
import org.psem2m.eclipse.ipojo.core.CompositeFile;

/**
 * Source from the CADSE project, modified to remove some CADSE internal
 * dependencies and to use a patched version of Pojoization
 * 
 * {@link http://code.google.com/a/eclipselabs.org/p/cadse/}
 * 
 * @author Stéphane Chomat
 * @author Thomas Calmant - Transformation with a patched Pojoization class
 */
public class EclipsePojoization extends Pojoization {

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

	public EclipsePojoization() {
		super();
		pResources = null;
	}

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
	 * @return True if the pojoization was done without error
	 * @throws CoreException
	 *             an error occurred retrieving project informations
	 */
	public boolean directoryPojoization(final IProject aProject,
			final CompositeFile aMetadataFile, final CompositeFile aManifestFile)
			throws CoreException {

		pErrorCaught = false;
		pProject = aProject;
		pManifestFile = aManifestFile;

		// Can throw an exception
		pJavaProject = (IJavaProject) aProject.getNature(JavaCore.NATURE_ID);

		// Get output path, in an Eclipse behavior
		final CompositeFile classesOutputFolder = new CompositeFile(aProject
				.getWorkspace().getRoot(), pJavaProject.getOutputLocation());

		// Do the "pojoization"
		super.directoryPojoization(classesOutputFolder, aMetadataFile,
				aManifestFile);

		return !pErrorCaught;
	}

	@Override
	protected void error(final String mes) {
		super.error(mes);
		Activator.logError("iPOJO Manipulator error : " + mes, null);
		pErrorCaught = true;
	}

	/**
	 * Return a byte array that contains the byte code of the given class name.
	 * 
	 * @param classname
	 *            name of a class to be read
	 * @return a byte array
	 * @throws IOException
	 *             if the class file cannot be read
	 */
	@Override
	protected byte[] getBytecode(String aClassName) throws IOException {

		if (!aClassName.startsWith("/")) {
			try {
				aClassName = pJavaProject.getOutputLocation()
						.append(aClassName).toPortableString();
			} catch (JavaModelException e) {
				Activator.logError("Can't get Java ouput folder", e);
				e.printStackTrace();
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

		// Read the file
		InputStream inputStream = null;
		try {
			inputStream = origF.getContents();
			byte[] bytes = new byte[inputStream.available()];

			if (bytes.length == 0)
				throw new IOException("Empty class");

			inputStream.read(bytes);
			return bytes;
		} catch (IOException e) {
			Activator.logError("Error reading class file", e);
			throw e;
		} catch (CoreException e) {
			Activator.logError("Error reading class file", e);
			throw new IOException(e.getMessage(), e);

		} finally {
			try {
				if (inputStream != null)
					inputStream.close();
			} catch (Exception e) {
				// Do nothing
			}
		}
	}

	@Override
	protected InputStream getManifestInputStream() throws IOException {
		try {
			return pManifestFile.getInputStream();
		} catch (CoreException ex) {
			Activator.logError("Error reading Manifest", ex);
			return null;
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	protected void searchClassFiles(final File aDirectory,
			final List aClassNameList) {

		if (aClassNameList == null) {
			Activator.logError("[searchClassFiles] Null output class list",
					null);
			return;
		}

		if (pResources != null) {

			// Only manipulate indicated files
			for (IResource resource : pResources) {

				// Only handle files
				if (resource.getType() == IResource.FILE) {
					aClassNameList.add(resource.getFullPath()
							.toPortableString());
				}
			}
		} else {

			IContainer directory;

			if (aDirectory == null) {
				try {
					directory = (IContainer) pProject.getWorkspace().getRoot()
							.findMember(pJavaProject.getOutputLocation());
				} catch (JavaModelException e) {
					Activator.logError(
							"Can't retrieve Java project output location", e);
					e.printStackTrace();
					return;
				}
			} else {

				// Find all .class files
				directory = (IContainer) pProject.getWorkspace().getRoot()
						.findMember(aDirectory.getPath());
			}

			if (directory == null) {
				Activator.logError("No Java project output location found...",
						null);
				return;
			}

			searchClassFiles(directory, aClassNameList);
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void searchClassFiles(final IContainer aContainer,
			final List aClassNameList) {

		IResource[] members;

		try {
			members = aContainer.members();
		} catch (CoreException e) {
			Activator.logError("Error listing members in : "
					+ aContainer.getFullPath().toOSString(), e);
			e.printStackTrace();
			return;
		}

		for (IResource member : members) {

			if (member.getType() == IResource.FILE
					&& member.getName().endsWith(".class")) {
				aClassNameList.add(member.getFullPath().toPortableString());

			} else if (member instanceof IContainer) {
				searchClassFiles((IContainer) member, aClassNameList);
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.felix.ipojo.manipulator.Pojoization#setBytecode(java.io.File,
	 * byte[])
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
			}

		} catch (CoreException ex) {
			Activator.logError("Error writing new .class file", ex);
		}
	}

	@Override
	protected void writeManifest(final Manifest mf) throws IOException {
		ByteArrayOutputStream outStream = new ByteArrayOutputStream();
		mf.write(outStream);

		try {
			pManifestFile.setContent(new ByteArrayInputStream(outStream
					.toByteArray()));
		} catch (CoreException ex) {
			Activator.logError("Error writing Manifest file", ex);
			ex.printStackTrace();
		}
	}
}
