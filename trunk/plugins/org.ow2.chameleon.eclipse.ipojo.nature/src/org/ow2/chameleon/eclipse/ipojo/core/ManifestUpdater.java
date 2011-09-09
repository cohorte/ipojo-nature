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

import java.io.InputStream;

import org.apache.felix.ipojo.manipulator.Pojoization;
import org.apache.felix.ipojo.manipulator.metadata.StreamMetadataProvider;
import org.apache.felix.ipojo.manipulator.render.MetadataRenderer;
import org.apache.felix.ipojo.manipulator.visitor.check.CheckFieldConsistencyVisitor;
import org.apache.felix.ipojo.manipulator.visitor.writer.ManipulatedResourcesWriter;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.ow2.chameleon.eclipse.ipojo.Activator;

/**
 * New implementation of the manifest updater, using the new Manipulator
 * interfaces
 * 
 * @author Thomas Calmant
 */
public class ManifestUpdater {

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
		final SortedManifestBuilder manifestBuilder = new SortedManifestBuilder();
		manifestBuilder.setMetadataRenderer(new MetadataRenderer());

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
