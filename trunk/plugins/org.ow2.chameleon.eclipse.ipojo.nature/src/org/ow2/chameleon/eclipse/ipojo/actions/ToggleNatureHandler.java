/*
 * Copyright 2015 OW2 Chameleon
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
package org.ow2.chameleon.eclipse.ipojo.actions;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.ow2.chameleon.eclipse.ipojo.Activator;
import org.ow2.chameleon.eclipse.ipojo.builder.IPojoNature;
import org.ow2.chameleon.eclipse.ipojo.core.ManifestUpdater;
import org.ow2.chameleon.eclipse.ipojo.core.ProjectUtilities;

/**
 * Add/Remove iPOJO nature popup menu action handler
 * 
 * @author Thomas Calmant
 */
public class ToggleNatureHandler extends AbstractProjectActionHandler {

	/** iPOJO Nature handler */
	private final IPojoNature pNature = new IPojoNature();

	/**
	 * Creates the template metadata.xml file
	 * 
	 * @param aProject
	 *            Project where the metadata.xml file must be created
	 */
	private void createMetadataTemplate(final IProject aProject) {

		// Get the metadata.xml Eclipse file
		final IFile metadataFile = aProject.getFile("/metadata.xml");
		if (metadataFile.exists()) {
			// Do nothing if the file already exists
			return;
		}

		// Get the metadata.xml template
		final InputStream inStream = getClass().getResourceAsStream(
				"/templates/metadata.xml");

		// Set the file content
		try {
			metadataFile.create(inStream, true, null);

		} catch (final CoreException e) {
			Activator.logError(aProject,
					"Error creating the metadata.xml file", e);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.core.commands.IHandler#execute(org.eclipse.core.commands.
	 * ExecutionEvent)
	 */
	@Override
	public Object execute(final ExecutionEvent aEvent)
			throws ExecutionException {

		// Get the selected projects
		final Set<IProject> selectedProjects = getSelectedProjects(aEvent);

		// Prepare configuration list
		final List<IProject> toConfigure = new ArrayList<IProject>();
		final List<IProject> toDeconfigure = new ArrayList<IProject>();

		if (!getProjectsToToggle(selectedProjects, toConfigure, toDeconfigure)) {
			// No projects to modify...
			return null;
		}

		final boolean setNature = toConfigure.size() >= toDeconfigure.size();
		if (setNature) {
			// More projects to configure than to deconfigure
			toggleProjectsNature(setNature, toConfigure);

		} else {
			// More projects to deconfigure
			toggleProjectsNature(setNature, toDeconfigure);
		}

		return null;
	}

	/**
	 * Separates the selected projects in two parts : those to deconfigure
	 * (remove the nature) and those to configure (add the nature).
	 * 
	 * @param aSelectedProjects
	 *            Projects to work on
	 * @param aToConfigure
	 *            Result list : projects to configure
	 * @param aToDeconfigure
	 *            Result list : projects to deconfigure
	 * 
	 * @return True if the selection returned at least one project
	 */
	private boolean getProjectsToToggle(
			final Collection<IProject> aSelectedProjects,
			final Collection<IProject> aToConfigure,
			final Collection<IProject> aToDeconfigure) {

		boolean atLeastOneValid = false;

		for (final IProject project : aSelectedProjects) {

			try {
				if (project.hasNature(IPojoNature.NATURE_ID)) {
					// The project has the iPOJO nature : we want to deconfigure
					// it
					aToDeconfigure.add(project);
					atLeastOneValid = true;

				} else if (project.hasNature(JavaCore.NATURE_ID)) {
					// Project has not the iPOJO nature but is a Java project :
					// we want to configure it
					aToConfigure.add(project);
					atLeastOneValid = true;
				}

			} catch (final CoreException ex) {
				// Just log the error...
				Activator.logError(project, "Error testing nature", ex);
			}
		}

		return atLeastOneValid;
	}

	/**
	 * Pops up the iPOJO nature configuration dialog and sets the nature to
	 * projects
	 * 
	 * @param aProjects
	 *            Projects to configure.
	 */
	private void toggleProjectsNature(final boolean aSetNature,
			final Collection<IProject> aProjects) {

		// Default options
		boolean addAnnotations = false;
		boolean createMetadataTemplate = false;

		// Manifest updater, to clean the manifest on nature removal
		final ManifestUpdater manifestUpdater;
		if (!aSetNature) {
			// Only instantiate the updater on removal
			manifestUpdater = new ManifestUpdater();

		} else {
			manifestUpdater = null;
		}

		if (aSetNature) {
			// Get the shell
			final Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();

			// Open the configuration dialog
			final NatureConfigurationDialog configDialog = new NatureConfigurationDialog(
					shell, aProjects);

			if (configDialog.open() != Window.OK) {
				// User click CANCEL : do nothing
				return;
			}

			// Set up flags
			addAnnotations = configDialog.isAnnotationsBoxChecked();
			createMetadataTemplate = configDialog.isCreateMetadataBoxChecked();
		}

		// Prepare the error message, just in case
		boolean hasError = false;

		final StringBuilder errorBuilder = new StringBuilder("Error ");
		if (aSetNature) {
			errorBuilder.append("setting");

		} else {
			errorBuilder.append("removing");
		}
		errorBuilder.append(" iPOJO nature to :\n");

		// Utility class
		final ProjectUtilities utilities = new ProjectUtilities();

		for (final IProject project : aProjects) {

			try {
				// Set the nature
				pNature.setProject(project);

				if (aSetNature) {
					// Set the nature
					pNature.configure();

					if (addAnnotations) {
						// Adds the annotation library
						utilities.addAnnotations(project);
					}

					if (createMetadataTemplate) {
						// Add the metadata.xml file template
						createMetadataTemplate(project);
					}

				} else {
					// Remove the nature
					pNature.deconfigure();

					// Remove the annotation container
					utilities.removeAnnotations(project);

					// Clean up the manifest
					manifestUpdater.removeManifestEntry(project);
				}

			} catch (final CoreException e) {
				// Log the error
				final StringBuilder builder = new StringBuilder("Can't ");
				if (aSetNature) {
					builder.append("set");

				} else {
					builder.append("remove");
				}
				builder.append(" the project nature");

				Activator.logError(project, builder.toString(), e);

				// Add it to the message
				errorBuilder.append("\t").append(project.getName())
						.append("\n");

				hasError = true;
			}
		}

		// Refresh modified projects
		for (final IProject project : aProjects) {
			try {
				project.refreshLocal(IResource.DEPTH_ONE, null);

			} catch (final CoreException e) {
				// Just log...
				Activator.logWarning(project, "Error refreshing project.", e);
			}
		}

		// Last word, if necessary
		if (hasError) {
			errorBuilder.append("\n(See the logs)");

			// Show the error
			Activator.showError(null, errorBuilder.toString(), null);
		}
	}

}
