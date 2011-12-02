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
package org.ow2.chameleon.eclipse.ipojo.builder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.ow2.chameleon.eclipse.ipojo.Activator;
import org.ow2.chameleon.eclipse.ipojo.actions.NatureConfigurationDialog;

/**
 * Add/Remove iPOJO nature popup menu action handler
 * 
 * @author Thomas Calmant
 */
public class ToggleNatureAction implements IObjectActionDelegate {

	/** iPOJO Nature handler */
	private final IPojoNature pNature = new IPojoNature();

	/** Current selection */
	private ISelection pSelection;

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
	protected boolean getProjectsToToggle(
			final Collection<IProject> aSelectedProjects,
			final Collection<IProject> aToConfigure,
			final Collection<IProject> aToDeconfigure) {

		boolean atLeastOneValid = false;

		for (IProject project : aSelectedProjects) {

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

			} catch (CoreException ex) {
				// Just log the error...
				Activator.logError(project, "Error testing nature", ex);
			}
		}

		return atLeastOneValid;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
	 */
	@Override
	public void run(final IAction aAction) {

		// Prepare selection list
		final List<IProject> selectedProjects = new ArrayList<IProject>();

		if (pSelection instanceof IStructuredSelection) {

			for (Iterator<?> it = ((IStructuredSelection) pSelection)
					.iterator(); it.hasNext();) {

				final Object element = it.next();
				IProject project = null;

				if (element instanceof IProject) {
					// Is the element a project ?
					project = (IProject) element;

				} else if (element instanceof IAdaptable) {
					// Is the element adaptable to a project ?
					project = (IProject) ((IAdaptable) element)
							.getAdapter(IProject.class);
				}

				if (project != null && project.isOpen()) {
					// Add the found project, if it's opened
					selectedProjects.add(project);
				}
			}
		}

		if (selectedProjects.isEmpty()) {
			// No valid project found, do nothing
			return;
		}

		// Prepare configuration list
		final List<IProject> toConfigure = new ArrayList<IProject>();
		final List<IProject> toDeconfigure = new ArrayList<IProject>();

		if (!getProjectsToToggle(selectedProjects, toConfigure, toDeconfigure)) {
			// No projects to modify...
			return;
		}

		System.out.println("Selected : " + selectedProjects);
		System.out.println("ToConf : " + toConfigure);
		System.out.println("ToUnconf: " + toDeconfigure);

		boolean setNature = toConfigure.size() >= toDeconfigure.size();

		if (setNature) {
			// More projects to configure than to deconfigure
			toggleProjectsNature(setNature, toConfigure);

		} else {
			// More projects to deconfigure
			toggleProjectsNature(setNature, toDeconfigure);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ui.IActionDelegate#selectionChanged(org.eclipse.jface.action
	 * .IAction, org.eclipse.jface.viewers.ISelection)
	 */
	@Override
	public void selectionChanged(final IAction aAction,
			final ISelection aSelection) {

		pSelection = aSelection;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ui.IObjectActionDelegate#setActivePart(org.eclipse.jface.
	 * action.IAction, org.eclipse.ui.IWorkbenchPart)
	 */
	@Override
	public void setActivePart(final IAction aAction,
			final IWorkbenchPart aTargetPart) {
		// Do nothing
	}

	/**
	 * Pops up the iPOJO nature configuration dialog and sets the nature to
	 * projects
	 * 
	 * @param aProjects
	 *            Projects to configure.
	 */
	protected void toggleProjectsNature(final boolean aSetNature,
			final Collection<IProject> aProjects) {

		// Default options
		boolean addAnnotations = false;
		boolean createMetadataTemplate = false;

		if (aSetNature) {
			// Get the shell
			final Shell shell = Activator.getPluginInstance().getWorkbench()
					.getActiveWorkbenchWindow().getShell();

			// Open the configuration dialog
			final NatureConfigurationDialog configDialog = new NatureConfigurationDialog(
					shell, aProjects);

			if (configDialog.open() != Window.OK) {
				// User click CANCEL : do nothing
				return;
			}

			// Set up flags
			addAnnotations = configDialog.isAddAnnotationsBoxChecked();
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

		for (IProject project : aProjects) {

			try {
				// Set the nature
				pNature.setProject(project);

				if (aSetNature) {
					pNature.configure();
					// TODO add annotations and metadata, if needed

					if (addAnnotations) {
						// TODO
					}

					if (createMetadataTemplate) {
						// TODO
					}

				} else {
					// Remove the nature
					pNature.deconfigure();
				}

			} catch (CoreException e) {
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
		for (IProject project : aProjects) {
			try {
				project.refreshLocal(IResource.DEPTH_ONE, null);

			} catch (CoreException e) {
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
