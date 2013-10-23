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
package org.ow2.chameleon.eclipse.ipojo.actions;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IAccessRule;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.ow2.chameleon.eclipse.ipojo.Activator;
import org.ow2.chameleon.eclipse.ipojo.IClasspathConstants;
import org.ow2.chameleon.eclipse.ipojo.builder.IPojoNature;
import org.ow2.chameleon.eclipse.ipojo.core.ManifestUpdater;

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
	 * Creates a copy of the given entry with a new access rule
	 * 
	 * @param aEntry
	 *            A class path entry (container)
	 * @param aNewAccessRule
	 *            A new access rule
	 * @return The new container entry
	 */
	private IClasspathEntry addAccessRule(final IClasspathEntry aEntry,
			final IAccessRule aNewAccessRule) {

		// Update access rules array
		final IAccessRule[] currentRules = aEntry.getAccessRules();
		final IAccessRule[] newRules = new IAccessRule[currentRules.length + 1];

		System.arraycopy(currentRules, 0, newRules, 0, currentRules.length);
		newRules[newRules.length - 1] = aNewAccessRule;

		// Create a new container entry
		return JavaCore.newContainerEntry(aEntry.getPath(), newRules,
				aEntry.getExtraAttributes(), aEntry.isExported());
	}

	/**
	 * Adds the annotations class path container to the project build path
	 * 
	 * @param aProject
	 *            Project to be modified
	 */
	private void addAnnotationsLibrary(final IProject aProject) {

		// Get the Java nature
		final IJavaProject javaProject;
		try {
			javaProject = (IJavaProject) aProject.getNature(JavaCore.NATURE_ID);

		} catch (final CoreException e) {
			Activator.logError(aProject, "Can't get the Java nature", e);
			return;
		}

		// Get current entries
		final IClasspathEntry[] currentEntries;
		try {
			currentEntries = javaProject.getRawClasspath();

		} catch (final JavaModelException e) {
			Activator.logError(aProject, "Error reading project classpath", e);
			return;
		}

		// Flag to indicate if the annotations container must be added
		boolean addAnnotations = true;

		// ID of the modified class path entry, if any
		int newPdeEntryIdx = -1;
		IClasspathEntry newPdeEntry = null;

		// Loop on project class path entries
		int idx = 0;
		for (final IClasspathEntry entry : currentEntries) {

			if (entry.getEntryKind() == IClasspathEntry.CPE_CONTAINER) {
				if (IClasspathConstants.ANNOTATIONS_CONTAINER_PATH.equals(entry
						.getPath())) {
					// The annotation container is already here.
					addAnnotations = false;

				} else if (IClasspathConstants.PDE_CONTAINER_PATH.equals(entry
						.getPath())) {
					// Found the PDE container

					// Flag to activate the access rule creation
					boolean needsRule = true;

					// Check if the access rule has already been set
					for (final IAccessRule accessRule : entry.getAccessRules()) {
						if (IClasspathConstants.ANNOTATIONS_ACCESS_PATTERN
								.equals(accessRule.getPattern())) {
							// iPOJO annotations access pattern already set
							needsRule = false;
							break;
						}
					}

					if (needsRule) {
						// Replace the container by a new one
						newPdeEntryIdx = idx;
						newPdeEntry = addAccessRule(
								entry,
								JavaCore.newAccessRule(
										IClasspathConstants.ANNOTATIONS_ACCESS_PATTERN,
										IAccessRule.K_ACCESSIBLE));
					}
				}
			}

			idx++;
		}

		if (!addAnnotations && newPdeEntry == null) {
			// Nothing to do
			return;
		}

		// Set up the new class path array
		final IClasspathEntry[] newEntries;

		if (addAnnotations) {
			newEntries = new IClasspathEntry[currentEntries.length + 1];

			// Add the new annotations at the tail
			newEntries[currentEntries.length] = JavaCore
					.newContainerEntry(IClasspathConstants.ANNOTATIONS_CONTAINER_PATH);

		} else {
			newEntries = new IClasspathEntry[currentEntries.length];
		}

		// Copy previous value
		System.arraycopy(currentEntries, 0, newEntries, 0,
				currentEntries.length);

		// Replace the PDE entry, if any
		if (newPdeEntry != null) {
			newEntries[newPdeEntryIdx] = newPdeEntry;
		}

		// Set the project class path
		try {
			javaProject.setRawClasspath(newEntries, null);

		} catch (final JavaModelException e) {
			Activator.logError(aProject,
					"Error setting up the new project class path", e);
		}
	}

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
	 * Creates a copy of the given entry without the given access rule
	 * 
	 * @param aEntry
	 *            A class path entry (container)
	 * @param aPath
	 *            An access rule path
	 * @return The new container entry
	 */
	private IClasspathEntry removeAccessRule(final IClasspathEntry aEntry,
			final IPath aPath) {

		// Update the access rules array
		final IAccessRule[] currentRules = aEntry.getAccessRules();
		final List<IAccessRule> newRules = new ArrayList<IAccessRule>(
				currentRules.length);

		// Filter access rules
		for (final IAccessRule rule : currentRules) {
			if (!aPath.equals(rule.getPattern())) {
				newRules.add(rule);
			}
		}

		// Convert the list to an array
		final IAccessRule[] newRulesArray = newRules
				.toArray(new IAccessRule[newRules.size()]);

		// Create a new container entry
		return JavaCore.newContainerEntry(aEntry.getPath(), newRulesArray,
				aEntry.getExtraAttributes(), aEntry.isExported());
	}

	/**
	 * Removes the annotations container from the project build path (opposite
	 * of {@link #addAnnotationsLibrary(IProject)}
	 * 
	 * @param aProject
	 *            A java project
	 */
	private void removeAnnotationsLibrary(final IProject aProject) {
		// Get the Java nature
		final IJavaProject javaProject;
		try {
			javaProject = (IJavaProject) aProject.getNature(JavaCore.NATURE_ID);

		} catch (final CoreException e) {
			Activator.logError(aProject, "Can't get the Java nature", e);
			return;
		}

		// Get current entries
		final IClasspathEntry[] currentEntries;
		try {
			currentEntries = javaProject.getRawClasspath();

		} catch (final JavaModelException e) {
			Activator.logError(aProject, "Error reading project classpath", e);
			return;
		}

		// Flag to indicate if the annotations container must be added
		boolean removeAnnotations = false;
		int annotationsEntryIdx = -1;

		// ID of the modified class path entry, if any
		int newPdeEntryIdx = -1;
		IClasspathEntry newPdeEntry = null;

		// Loop on project class path entries
		int idx = 0;
		for (final IClasspathEntry entry : currentEntries) {

			if (entry.getEntryKind() == IClasspathEntry.CPE_CONTAINER) {
				if (IClasspathConstants.ANNOTATIONS_CONTAINER_PATH.equals(entry
						.getPath())) {
					// The annotation container is here.
					removeAnnotations = true;
					annotationsEntryIdx = idx;

				} else if (IClasspathConstants.PDE_CONTAINER_PATH.equals(entry
						.getPath())) {
					// Found the PDE container

					// Flag to activate the access rule creation
					boolean removeRule = false;

					// Check if the access rule has already been set
					for (final IAccessRule accessRule : entry.getAccessRules()) {
						if (IClasspathConstants.ANNOTATIONS_ACCESS_PATTERN
								.equals(accessRule.getPattern())) {
							// iPOJO annotations access pattern is set
							removeRule = true;
							System.out.println("Found access rule");
							break;
						}
					}

					if (removeRule) {
						// Replace the container by a new one
						newPdeEntryIdx = idx;
						newPdeEntry = removeAccessRule(entry,
								IClasspathConstants.ANNOTATIONS_ACCESS_PATTERN);
					}
				}
			}

			idx++;
		}

		if (!removeAnnotations && newPdeEntry == null) {
			// Nothing to do
			return;
		}

		// Set up the new class path array
		final IClasspathEntry[] newEntries;

		if (removeAnnotations) {
			// Copy the array, ignoring the annotations container
			newEntries = new IClasspathEntry[currentEntries.length - 1];
			for (int i = 0, j = 0; i < currentEntries.length - 1; i++) {
				if (i != annotationsEntryIdx) {
					newEntries[j++] = currentEntries[i];
				}
			}

		} else {
			// Copy previous value
			newEntries = new IClasspathEntry[currentEntries.length];
			System.arraycopy(currentEntries, 0, newEntries, 0,
					currentEntries.length);
		}

		// Replace the PDE entry, if any
		if (newPdeEntry != null) {

			if (newPdeEntryIdx > annotationsEntryIdx) {
				// We removed the annotation entry...
				newPdeEntryIdx--;
			}

			newEntries[newPdeEntryIdx] = newPdeEntry;
		}

		// Set the project class path
		try {
			javaProject.setRawClasspath(newEntries, null);

		} catch (final JavaModelException e) {
			Activator.logError(aProject,
					"Error setting up the new project class path", e);
		}
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

			for (final Iterator<?> it = ((IStructuredSelection) pSelection)
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

		final boolean setNature = toConfigure.size() >= toDeconfigure.size();
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

		for (final IProject project : aProjects) {

			try {
				// Set the nature
				pNature.setProject(project);

				if (aSetNature) {
					// Set the nature
					pNature.configure();

					if (addAnnotations) {
						// Adds the annotation library
						addAnnotationsLibrary(project);
					}

					if (createMetadataTemplate) {
						// Add the metadata.xml file template
						createMetadataTemplate(project);
					}

				} else {
					// Remove the nature
					pNature.deconfigure();

					// Remove the annotation container
					removeAnnotationsLibrary(project);

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
