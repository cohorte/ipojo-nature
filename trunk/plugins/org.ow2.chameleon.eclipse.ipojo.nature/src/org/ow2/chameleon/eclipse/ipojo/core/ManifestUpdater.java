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

import java.io.IOException;
import java.io.InputStream;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.apache.felix.ipojo.manipulator.Pojoization;
import org.apache.felix.ipojo.manipulator.metadata.StreamMetadataProvider;
import org.apache.felix.ipojo.manipulator.render.MetadataRenderer;
import org.apache.felix.ipojo.manipulator.visitor.check.CheckFieldConsistencyVisitor;
import org.apache.felix.ipojo.manipulator.visitor.writer.ManipulatedResourcesWriter;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
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

		} catch (IOException ex) {
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
	 * Applies a full iPOJO update on the project Manifest.
	 * 
	 * @param aProject
	 *            Eclipse Java project containing the Manifest
	 * @throws CoreException
	 *             An error occurred during file treatments
	 */
	public void updateManifest(final IProject aProject) throws CoreException {

		if (!Utilities.INSTANCE.isJavaProject(aProject)) {
			Activator.logInfo(aProject, "Not a Java project");
			return;
		}

		// Error reporter for Eclipse
		final EclipseReporter reporter = new EclipseReporter(aProject);

		// Pojoization API
		final Pojoization pojoization = new Pojoization(reporter);

		// Metadata provider
		StreamMetadataProvider metadataProvider = null;
		final InputStream metadataStream = Utilities.INSTANCE
				.getMetadataStream(aProject);
		if (metadataStream != null) {
			metadataProvider = new StreamMetadataProvider(metadataStream,
					reporter);
		}

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

		/*
		 * Manipulation visitor, converted version of
		 * org.apache.felix.ipojo.manipulator
		 * .Pojoization.createDefaultVisitorChain(ManifestProvider,
		 * ResourceStore)
		 */
		final ManipulatedResourcesWriter resourcesWriter = new ManipulatedResourcesWriter();
		resourcesWriter.setResourceStore(resourceStore);
		resourcesWriter.setReporter(reporter);

		// Finish with this one, as in default Pojoization implementation
		final CheckFieldConsistencyVisitor checkConsistencyVisitor = new CheckFieldConsistencyVisitor(
				resourcesWriter);
		checkConsistencyVisitor.setReporter(reporter);

		pojoization.pojoization(resourceStore, metadataProvider,
				checkConsistencyVisitor);

		if (reporter.getErrors().isEmpty() && reporter.getWarnings().isEmpty()) {
			// No problem : full success
			Activator.logInfo(aProject, "Manipulation done");
		}

		// Errors have already been logged.
		// TODO pop up an error box with all warnings & errors
	}
}
