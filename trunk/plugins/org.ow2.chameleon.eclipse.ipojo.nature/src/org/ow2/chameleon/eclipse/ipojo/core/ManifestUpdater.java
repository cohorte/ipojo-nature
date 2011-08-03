/**
 * 
 */
package org.ow2.chameleon.eclipse.ipojo.core;

import java.io.InputStream;
import java.util.jar.Manifest;

import org.apache.felix.ipojo.manipulator.DefaultManifestBuilder;
import org.apache.felix.ipojo.manipulator.ManifestProvider;
import org.apache.felix.ipojo.manipulator.Pojoization;
import org.apache.felix.ipojo.manipulator.manifest.DirectManifestProvider;
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

		// Resource store
		EclipseResourceStore resourceStore = new EclipseResourceStore(aProject);

		// Metadata provider
		StreamMetadataProvider metadataProvider = null;
		final InputStream metadataStream = Utilities.INSTANCE
				.getMetadataStream(aProject);
		if (metadataStream != null) {
			metadataProvider = new StreamMetadataProvider(metadataStream,
					reporter);
		}

		// Manifest provider
		Manifest manifestContent = Utilities.INSTANCE
				.getManifestContent(aProject);

		ManifestProvider manifestProvider = new DirectManifestProvider(
				manifestContent);

		// Manifest builder (default one)
		DefaultManifestBuilder manifestBuilder = new DefaultManifestBuilder();
		manifestBuilder.setMetadataRenderer(new MetadataRenderer());

		/*
		 * Manipulation visitor, converted version of
		 * org.apache.felix.ipojo.manipulator
		 * .Pojoization.createDefaultVisitorChain(ManifestProvider,
		 * ResourceStore)
		 */
		ManipulatedResourcesWriter resourcesWriter = new ManipulatedResourcesWriter();
		resourcesWriter.setManifestBuilder(manifestBuilder);
		resourcesWriter.setManifestProvider(manifestProvider);
		resourcesWriter.setResourceStore(resourceStore);
		resourcesWriter.setReporter(reporter);

		// Finish with this one, as in default Pojoization implementation
		CheckFieldConsistencyVisitor checkConsistencyVisitor = new CheckFieldConsistencyVisitor(
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
