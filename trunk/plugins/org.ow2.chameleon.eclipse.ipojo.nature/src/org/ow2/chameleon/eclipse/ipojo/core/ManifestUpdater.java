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

import java.io.IOException;
import java.io.InputStream;
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
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
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
	 * Prepares an iPOJO {@link Classpath} object
	 * 
	 * @param aProject
	 *            An Eclipse project
	 * @return The iPOJO classpath, or null if the class path can't be resolved
	 * @throws JavaModelException
	 *             Error reading project classpath
	 */
	protected Classpath prepareClasspath(final IProject aProject)
			throws JavaModelException {

		// Get the Java nature of the project
		final IJavaProject javaProject = Utilities.INSTANCE
				.getJavaProject(aProject);

		// Convert Eclipse classpath to iPOJO ones
		return new Classpath(new ClasspathResolver().getClasspath(javaProject));
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
		final Classpath ipojoClasspath;
		try {
			ipojoClasspath = prepareClasspath(aProject);

		} catch (final JavaModelException ex) {
			return new Status(IStatus.ERROR, Activator.PLUGIN_ID,
					"Project classpath can't be computed", ex);
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
