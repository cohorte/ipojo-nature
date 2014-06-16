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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.apache.felix.ipojo.manipulator.ManipulationVisitor;
import org.apache.felix.ipojo.manipulator.MetadataProvider;
import org.apache.felix.ipojo.manipulator.Pojoization;
import org.apache.felix.ipojo.manipulator.Reporter;
import org.apache.felix.ipojo.manipulator.ResourceStore;
import org.apache.felix.ipojo.manipulator.metadata.EmptyMetadataProvider;
import org.apache.felix.ipojo.manipulator.metadata.StreamMetadataProvider;
import org.apache.felix.ipojo.manipulator.render.MetadataRenderer;
import org.apache.felix.ipojo.manipulator.util.Classpath;
import org.apache.felix.ipojo.manipulator.visitor.check.CheckFieldConsistencyVisitor;
import org.apache.felix.ipojo.manipulator.visitor.writer.ManipulatedResourcesWriter;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;
import org.ow2.chameleon.eclipse.ipojo.Activator;

/**
 * New implementation of the manifest updater, using the new Manipulator
 * interfaces
 * 
 * @author Thomas Calmant
 */
public class ManifestUpdater {

	/** iPOJO Manifest entry */
	public static final String IPOJO_HEADER = "iPOJO-Components";

	/**
	 * Finds the project matching the given entry path
	 * 
	 * @param aRoot
	 *            Workspace root
	 * @param aPath
	 *            Workspace-relative path
	 * @return The Java project object or null
	 */
	private IJavaProject findProject(final IWorkspaceRoot aRoot,
			final IPath aPath) {
		final IResource resource = aRoot.findMember(aPath);
		if (resource instanceof IProject) {
			final IProject project = (IProject) resource;
			return Utilities.INSTANCE.getJavaProject(project);
		}

		// Not a project
		return null;
	}

	/**
	 * Returns the file-system path to the output directory of the given project
	 * 
	 * @param aRoot
	 *            Workspace root
	 * @param aJavaProject
	 *            Java project
	 * @return The absolute path to the output directory, or null
	 */
	private String getProjectOutput(final IWorkspaceRoot aRoot,
			final IJavaProject aJavaProject) {

		try {
			return aRoot.getFile(aJavaProject.getOutputLocation())
					.getRawLocation().toOSString();

		} catch (final JavaModelException e) {
			// Something went wrong
			return null;
		}
	}

	/**
	 * Prepares an iPOJO {@link Classpath} object
	 * 
	 * @param aProject
	 *            An Eclipse project
	 * @return The iPOJO classpath, or null if the class path can't be resolved
	 */
	protected Classpath prepareClasspath(final IProject aProject) {

		// Get workspace root
		final IWorkspaceRoot root = aProject.getWorkspace().getRoot();

		// Get the Java nature of the project
		final IJavaProject javaProject = Utilities.INSTANCE
				.getJavaProject(aProject);

		// Class path URLs for iPOJO
		final Collection<String> paths = new LinkedHashSet<String>();

		// Add the current project output directory
		final String projectOutput = getProjectOutput(root, javaProject);
		if (projectOutput != null) {
			paths.add(projectOutput);
		}

		// Get Java project classpath entries, resolved by Eclipse
		IClasspathEntry[] classpathEntries;
		try {
			classpathEntries = javaProject.getResolvedClasspath(false);

		} catch (final JavaModelException ex) {
			Activator.logError(aProject, "Unresolved classpath: " + ex, ex);
			return null;
		}

		/*
		 * FIXME: Either use the raw classpath entries and filter CPE_CONTAINER
		 * in order to ignore the JRE/JDK installation, or look for this
		 * container to remove its path in the end.
		 */

		// Convert them into absolute paths
		for (final IClasspathEntry entry : classpathEntries) {
			String path = null;

			switch (entry.getEntryKind()) {
			case IClasspathEntry.CPE_CONTAINER:
			case IClasspathEntry.CPE_SOURCE:
				// Silently ignore
				continue;

			case IClasspathEntry.CPE_PROJECT:
				// Refers to another project
				final IJavaProject depProject = findProject(root,
						entry.getPath());
				if (depProject != null) {
					path = getProjectOutput(root, depProject);
					if (path == null) {
						// Something went wrong
						Activator.logError(aProject, "Bad Java project", null);
					}
				}
				break;

			case IClasspathEntry.CPE_LIBRARY:
				// Refers to a library
				final File classpathFile = entry.getPath().toFile();
				if (classpathFile.exists()) {
					if (!classpathFile.getName().equals("rt.jar")) {
						// Accept everything except the JRE/JDK rt.jar file
						path = classpathFile.getAbsolutePath();
					}
				}
				break;

			default:
				// Ignore other kinds of entry
				break;
			}

			if (path != null) {
				// Avoid null pointer exception...
				paths.add(path);
			}
		}

		// iPOJO Classpath utility
		return new Classpath(paths);
	}

	/**
	 * Prepares the manipulation visitor. Based on
	 * org.apache.felix.ipojo.manipulator.Pojoization.createDefaultVisitorChain
	 * (ManifestProvider,ResourceStore).
	 * 
	 * @param aReporter
	 *            Status reporter
	 * @param aResourceStore
	 *            Resource store
	 * @return The manipulation visitor
	 */
	protected ManipulationVisitor prepareManipulationVisitor(
			final Reporter aReporter, final ResourceStore aResourceStore) {

		final ManipulatedResourcesWriter resourcesWriter = new ManipulatedResourcesWriter();
		resourcesWriter.setResourceStore(aResourceStore);
		resourcesWriter.setReporter(aReporter);

		// Finish with this one, as in default Pojoization implementation
		final CheckFieldConsistencyVisitor checkConsistencyVisitor = new CheckFieldConsistencyVisitor(
				resourcesWriter);
		checkConsistencyVisitor.setReporter(aReporter);

		return checkConsistencyVisitor;
	}

	/**
	 * Prepares a composite meta data provider (XML file if found, else an empty
	 * provider). iPOJO automatically adds the annotation meta data provider.
	 * 
	 * @param aProject
	 *            Currently modified project
	 * @param aReporter
	 *            Status reporter
	 * @param aResourceStore
	 *            Resource store
	 * @return A composite meta data provider.
	 */
	protected MetadataProvider prepareMetadataProvider(final IProject aProject,
			final Reporter aReporter, final ResourceStore aResourceStore) {

		// Find the metadata.xml file
		final InputStream metadataStream = Utilities.INSTANCE
				.getMetadataStream(aProject);
		if (metadataStream != null) {

			// Return the found meta data
			final StreamMetadataProvider provider = new StreamMetadataProvider(
					metadataStream, aReporter);

			// Use local schemas, to avoid Internet connections
			provider.setValidateUsingLocalSchemas(true);
			return provider;
		}

		// Return an empty provider if no meta data is found
		return new EmptyMetadataProvider();
	}

	/**
	 * Prepares the resource store
	 * 
	 * @param aProject
	 *            Currently modified project
	 * @return The resource store
	 * @throws CoreException
	 *             An error occurred while preparing the resource store
	 */
	protected ResourceStore prepareResourceStore(final IProject aProject)
			throws CoreException {

		// Manifest builder (default one)
		final MetadataRenderer metadataRenderer = new MetadataRenderer();
		metadataRenderer.addMetadataFilter(new MetadataIpojoElementFilter());

		final SortedManifestBuilder manifestBuilder = new SortedManifestBuilder();
		manifestBuilder.setMetadataRenderer(metadataRenderer);

		// Resource store
		final EclipseResourceStore resourceStore = new EclipseResourceStore(
				aProject);
		resourceStore.setManifest(Utilities.INSTANCE
				.getManifestContent(aProject));
		resourceStore.setManifestBuilder(manifestBuilder);

		return resourceStore;
	}

	/**
	 * Removes the iPOJO-Component entry from the manifest file
	 * 
	 * @param aProject
	 *            Currently modified project
	 * @throws CoreException
	 *             An error occurred clearing the Manifest file
	 */
	public void removeManifestEntry(final IProject aProject)
			throws CoreException {

		// Get the file
		final IFile manifestFile = Utilities.INSTANCE.getManifestFile(aProject,
				false);
		if (!manifestFile.exists()) {
			// No manifest, do nothing
			return;
		}

		// Read the current manifest content
		final Manifest manifestContent;
		try {
			manifestContent = new Manifest(manifestFile.getContents(true));

		} catch (final IOException ex) {
			throw new CoreException(new Status(IStatus.WARNING,
					Activator.PLUGIN_ID, aProject.getName()
							+ " : Can't read the project's manifest file", ex));
		}

		// Remove the iPOJO-Component entry
		final Attributes.Name entryName = new Attributes.Name(IPOJO_HEADER);
		final Object previousValue = manifestContent.getMainAttributes()
				.remove(entryName);

		if (previousValue != null) {
			// Use a sorted manifest object first
			Utilities.INSTANCE.makeSortedManifest(aProject, manifestContent);

			// There was something before, so write the new manifest
			Utilities.INSTANCE.setManifestContent(aProject, manifestContent);
		}
	}

	/**
	 * Applies a full iPOJO update on the project Manifest. Returns an IStatus
	 * representing the result.
	 * 
	 * @param aProject
	 *            Eclipse Java project containing the Manifest
	 * @param aMonitor
	 *            Progress monitor
	 * 
	 * @return Returns an Eclipse IStatus
	 * 
	 * @throws CoreException
	 *             An error occurred during file treatments
	 */
	public IStatus updateManifest(final IProject aProject,
			final IProgressMonitor aMonitor) throws CoreException {

		if (!Utilities.INSTANCE.isJavaProject(aProject)) {
			Activator.logWarning(aProject, "Not a Java project");
			return new Status(IStatus.ERROR, Activator.PLUGIN_ID,
					aProject.getName() + " is not a Java Project");
		}

		// Prepare a sub monitor
		final SubMonitor subMonitor = SubMonitor.convert(aMonitor, 100);
		final IProgressMonitor preparationMonitor = subMonitor.newChild(4);
		preparationMonitor.setTaskName("Manipulation preparation");

		// Error reporter for Eclipse
		final EclipseReporter reporter = new EclipseReporter(aProject);
		preparationMonitor.worked(1);

		// Prepare the resource store
		final ResourceStore resourceStore = prepareResourceStore(aProject);
		preparationMonitor.worked(1);

		// Prepare the meta data provider
		final MetadataProvider metadataProvider = prepareMetadataProvider(
				aProject, reporter, resourceStore);
		preparationMonitor.worked(1);

		// Manipulation visitor
		final ManipulationVisitor manipulationVisitor = prepareManipulationVisitor(
				reporter, resourceStore);
		preparationMonitor.worked(1);

		// Test cancellation
		if (preparationMonitor.isCanceled()) {
			return new Status(IStatus.OK, Activator.PLUGIN_ID,
					"Manipulation cancelled");
		}

		// New progression
		final IProgressMonitor pojoizationMonitor = subMonitor.newChild(96);
		pojoizationMonitor.setTaskName("Manipulation");

		// Set the resource store progress monitor
		((EclipseResourceStore) resourceStore)
				.setProgressMonitor(pojoizationMonitor);

		// Get the project class path
		final Classpath ipojoClasspath = prepareClasspath(aProject);
		if (ipojoClasspath == null) {
			return new Status(IStatus.ERROR, Activator.PLUGIN_ID,
					"Project classpath can't be computed");
		}

		// Pojoization API
		final Pojoization pojoization = new Pojoization(reporter);
		pojoization.setUseLocalXSD();
		pojoization.pojoization(resourceStore, metadataProvider,
				manipulationVisitor, ipojoClasspath.createClassLoader());

		// Update progress monitor
		if (aMonitor != null) {
			aMonitor.done();
		}

		return reporter.getEclipseStatus();
	}
}
