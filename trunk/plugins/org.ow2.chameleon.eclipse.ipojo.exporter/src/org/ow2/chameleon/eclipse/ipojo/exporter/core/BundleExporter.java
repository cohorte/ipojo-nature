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
package org.ow2.chameleon.eclipse.ipojo.exporter.core;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.osgi.framework.Constants;
import org.ow2.chameleon.eclipse.ipojo.Activator;
import org.ow2.chameleon.eclipse.ipojo.core.Utilities;
import org.ow2.chameleon.eclipse.ipojo.exporter.IPojoExporterPlugin;

/**
 * Bundle export logic
 * 
 * @author Thomas Calmant
 */
public class BundleExporter {

	/** Bundle export folder */
	private String pJarOutputFolder;

	/** Use the build.properties file, if present */
	private boolean pUseBuildProperties;

	/** Workspace root */
	private IWorkspaceRoot pWorkspaceRoot;

	/**
	 * Adds the given source file to the JAR stream using the given entry name
	 * 
	 * @param aJarStream
	 *            Output JAR stream
	 * @param aSourceFile
	 *            Source file
	 * @param aEntryName
	 *            File name in the output JAR
	 * @throws IOException
	 *             An error occurred while working on the JAR stream
	 * @throws CoreException
	 *             An error occurred while reading the source file
	 */
	protected void addFile(final JarOutputStream aJarStream,
			final IFile aSourceFile, final String aEntryName)
			throws IOException, CoreException {

		// Ignore the manifest file (already included)
		if (aSourceFile.getName().equalsIgnoreCase(Utilities.MANIFEST_NAME)) {
			return;
		}

		// Open the source file
		final BufferedInputStream sourceStream = new BufferedInputStream(
				aSourceFile.getContents());

		// Prepare the entry
		final JarEntry entry = new JarEntry(aEntryName);
		aJarStream.putNextEntry(entry);

		// 8 kb buffer
		final byte[] buffer = new byte[8192];
		while (true) {
			final int read = sourceStream.read(buffer);
			if (read <= 0) {
				// EOF (or error, ...)
				break;
			}

			// Be careful on read length...
			aJarStream.write(buffer, 0, read);
		}

		// Release streams
		sourceStream.close();
		aJarStream.closeEntry();
	}

	/**
	 * Exports the given project as an iPOJO bundle
	 * 
	 * @param aProject
	 *            Project to be exported
	 * @param aMonitor
	 *            A progress monitor
	 */
	public void exportBundle(final IProject aProject,
			final IProgressMonitor aMonitor) throws CoreException {

		// Store the workspace root (should'nt move...)
		pWorkspaceRoot = aProject.getWorkspace().getRoot();

		// Rebuild the project (incremental build)
		aProject.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, aMonitor);

		// Get the project manifest
		final Manifest projectManifest = Utilities.INSTANCE
				.getManifestContent(aProject);

		// Get the Java project nature
		final IJavaProject javaProject = (IJavaProject) aProject
				.getNature(JavaCore.NATURE_ID);

		// Prepare the File -> Entry map
		final Map<IFile, String> jarEntriesMapping = new HashMap<IFile, String>();

		if (pUseBuildProperties) {
			// Try to use the build.properties file
			boolean buildPropertiesFound = false;
			final IFile buildProperties = aProject
					.getFile(IExporterConstants.BUILD_PROPERTIES_PATH);

			if (buildProperties.exists()) {
				try {
					// Try to read it
					buildPropertiesFound = readBuildProperties(aProject,
							jarEntriesMapping);

				} catch (final IOException e) {
					IPojoExporterPlugin.logWarning(MessageFormat.format(
							"Can't read the build.properties file of : {0}",
							aProject.getName()), e);
				}

			}

			if (!buildPropertiesFound) {
				// No build.properties file available
				prepareProjectFilesList(javaProject, jarEntriesMapping);
			}

		} else {
			// Don't use the build.properties file
			prepareProjectFilesList(javaProject, jarEntriesMapping);
		}

		// Make the JAR file
		try {
			final byte[] jarContent = makeJar(projectManifest,
					jarEntriesMapping);

			// Prepare the output file
			final File outputJarFile = new File(pJarOutputFolder,
					getJarFileName(aProject, projectManifest));

			// Make parent directories if needed
			outputJarFile.getParentFile().mkdirs();

			// Write it down
			final FileOutputStream outStream = new FileOutputStream(
					outputJarFile);
			outStream.write(jarContent);
			outStream.close();

			// Update Eclipse resource, if the JAR is visible from the IDE
			final IFile[] eclipseFiles = pWorkspaceRoot
					.findFilesForLocationURI(outputJarFile.toURI());
			if (eclipseFiles != null) {
				for (final IFile eclipseFile : eclipseFiles) {
					eclipseFile.refreshLocal(IResource.DEPTH_ONE, aMonitor);
				}
			}

		} catch (final IOException e) {

			throw new CoreException(new Status(IStatus.ERROR,
					IPojoExporterPlugin.PLUGIN_ID, MessageFormat.format(
							"Error writing JAR file for {0}",
							aProject.getName()), e));
		}
	}

	/**
	 * Generates the JAR file name (without path), based on the bundle symbolic
	 * name or the project name
	 * 
	 * @param aProject
	 *            Exported project
	 * @param aManifest
	 *            Project manifest content
	 * @return The JAR file name
	 */
	public String getJarFileName(final IProject aProject,
			final Manifest aManifest) {

		// Try with the manifest content ...
		String fileName = aManifest.getMainAttributes().getValue(
				Constants.BUNDLE_SYMBOLICNAME);

		if (fileName == null) {
			// ... else, use the project
			fileName = aProject.getName();
		}

		// Append the jar extension
		return fileName + ".jar";
	}

	/**
	 * Makes the JAR file in memory (using a ByteOutputStream object)
	 * 
	 * @param aManifest
	 *            The project Manifest
	 * @param aJarContentsMapping
	 *            Source file -> JAR Entry mapping
	 * @return The JAR file content
	 * @throws CoreException
	 *             An error occurred working with an Eclipse resource
	 * @throws IOException
	 *             An error occurred generating the JAR file content
	 */
	protected byte[] makeJar(final Manifest aManifest,
			final Map<IFile, String> aJarContentsMapping) throws CoreException,
			IOException {

		final ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
		final JarOutputStream jarStream = new JarOutputStream(byteStream,
				aManifest);

		// Add all found files
		for (final Entry<IFile, String> jarEntry : aJarContentsMapping
				.entrySet()) {
			addFile(jarStream, jarEntry.getKey(), jarEntry.getValue());
		}

		jarStream.close();
		return byteStream.toByteArray();
	}

	/**
	 * Prepares the project output files mapping for a JAR output, based on the
	 * Java project output folder
	 * 
	 * @param aJavaProject
	 *            A Java project (JDT)
	 * @param aJarEntriesMapping
	 *            A map to populate with found files
	 * @throws CoreException
	 */
	protected void prepareProjectFilesList(final IJavaProject aJavaProject,
			final Map<IFile, String> aJarEntriesMapping) throws CoreException {

		try {
			// Get the project output folder
			final IPath outputLocation = aJavaProject.getOutputLocation();
			final IFolder outputFolder = pWorkspaceRoot
					.getFolder(outputLocation);

			// Look for any file in this folder
			visitFolder(outputLocation, outputFolder, aJarEntriesMapping);

		} catch (final JavaModelException e) {
			IPojoExporterPlugin.logWarning(MessageFormat.format(
					"Error reading the project model of {0}", aJavaProject
							.getProject().getName()), e);
		}
	}

	/**
	 * Reads the build.properties file, if any
	 * 
	 * @param aProject
	 *            Project to look in
	 * @param aJarEntriesMapping
	 *            File -> JAR entry map
	 * @return True if the build.properties contains all what we need
	 * @throws IOException
	 *             An error occurred while reading the properties file
	 * @throws CoreException
	 *             An error occurred while accessible the file
	 */
	protected boolean readBuildProperties(final IProject aProject,
			final Map<IFile, String> aJarEntriesMapping) throws IOException,
			CoreException {

		// Get the file
		final IFile buildPropertiesFile = aProject
				.getFile(IExporterConstants.BUILD_PROPERTIES_PATH);

		// Load properties
		final Properties buildProperties = new Properties();
		buildProperties.load(buildPropertiesFile.getContents());

		/* Output folder */
		final String outputFolderStr = buildProperties
				.getProperty(IExporterConstants.BUILD_PROPERTIES_KEY_OUTPUT);
		if (outputFolderStr == null || outputFolderStr.isEmpty()) {
			// No output folder given...
			Activator.logWarning(aProject,
					"No output folder indicated in 'build.properties'.", null);

		} else {
			// Visit the indicated folder
			final IFolder outputFolder = aProject.getFolder(outputFolderStr);
			final IPath outputFolderPath = outputFolder.getFullPath();

			// Visit the output folder
			visitFolder(outputFolderPath, outputFolder, aJarEntriesMapping);
		}

		/* Included files / folders */
		final String[] binaryIncludes = buildProperties.getProperty(
				IExporterConstants.BUILD_PROPERTIES_KEY_BIN_INCLUDES, "")
				.split(",");

		// Project path
		final IPath projectPath = aProject.getFullPath();

		for (final String includedStr : binaryIncludes) {

			if (includedStr.equals(".")) {
				// Ignore project folder import
				continue;
			}

			// Find the resource
			final IResource includedResource = aProject.findMember(includedStr);

			if (includedResource instanceof IFolder) {
				// Folder included : visit it
				visitFolder(projectPath, (IFolder) includedResource,
						aJarEntriesMapping);

			} else if (includedResource instanceof IFile) {

				// Only add the file if it has not been computed yet
				final String jarEntry = includedResource
						.getProjectRelativePath().toString();

				if (!aJarEntriesMapping.containsValue(jarEntry)) {
					// Map the file directly
					aJarEntriesMapping.put((IFile) includedResource, jarEntry);

				} else {
					// Find the previous file definition
					IFile previous = null;
					for (final Entry<IFile, String> entry : aJarEntriesMapping
							.entrySet()) {

						if (jarEntry.equals(entry.getValue())) {
							// Previous file found
							previous = entry.getKey();
							break;
						}
					}

					if (previous != null) {
						// Log warning
						final String previousStr = previous.getFullPath()
								.toString();
						final String currentStr = includedResource
								.getFullPath().toString();

						IPojoExporterPlugin
								.logWarning(
										MessageFormat
												.format("JAR file entry '{0}' defined twice, in '{1}' and '{2}'.\nUsing '{3}'.",
														jarEntry, previousStr,
														currentStr, previousStr),
										null);
					}
				}
			}
		}

		return true;
	}

	/**
	 * Sets the bundle output folder
	 * 
	 * @param aFolder
	 *            The output folder
	 */
	public void setOutputFolder(final String aFolder) {

		pJarOutputFolder = aFolder;
	}

	/**
	 * Use the build.properties file ?
	 * 
	 * @param aUseBuildProperties
	 *            True to use it, else false
	 */
	public void setUseBuildProperties(final boolean aUseBuildProperties) {

		pUseBuildProperties = aUseBuildProperties;
	}

	/**
	 * Prepares the list of files in the given folder to be stored in the output
	 * JAR file
	 * 
	 * The given base path must be a full path, i.e. relative to the workspace.
	 * 
	 * @param aBasePath
	 *            All found files paths will be relative to this path
	 * @param aFolder
	 *            Folder to look into
	 * @param aJarEntriesMapping
	 *            Jar entries map
	 * @throws CoreException
	 *             An error occurred while listing members of a resource
	 */
	protected void visitFolder(final IPath aBasePath, final IFolder aFolder,
			final Map<IFile, String> aJarEntriesMapping) throws CoreException {

		if (!aFolder.exists()) {
			// Nothing to do here...
			IPojoExporterPlugin.logWarning(MessageFormat.format(
					"Folder '{0}' doesn't exist.", aFolder.getFullPath()
							.toOSString()), null);
			return;
		}

		for (final IResource resource : aFolder.members()) {

			if (resource instanceof IFolder) {
				// Recursively visit the folder
				visitFolder(aBasePath, (IFolder) resource, aJarEntriesMapping);

			} else if (resource instanceof IFile) {
				/*
				 * Only store files. Entry names are relative to the root of the
				 * resulting JAR.
				 */
				aJarEntriesMapping.put((IFile) resource, resource.getFullPath()
						.makeRelativeTo(aBasePath).toString());
			}
		}
	}
}
