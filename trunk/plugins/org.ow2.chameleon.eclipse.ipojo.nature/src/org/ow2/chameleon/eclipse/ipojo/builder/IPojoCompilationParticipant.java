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
package org.ow2.chameleon.eclipse.ipojo.builder;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IJavaModelMarker;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.compiler.BuildContext;
import org.eclipse.jdt.core.compiler.CompilationParticipant;
import org.eclipse.ui.statushandlers.StatusManager;
import org.ow2.chameleon.eclipse.ipojo.Activator;
import org.ow2.chameleon.eclipse.ipojo.core.ManifestUpdater;

/**
 * iPOJO Manifest Updater, as a JDT compilation participant.
 * 
 * This participant is considered as an annotation processor. It <b>seems</b>
 * that the {@link #processAnnotations(BuildContext[])} method is called in any
 * case, i.e. even if no file in the project contains annotations.
 * 
 * This call <b>seems</b> made by JDT after class files creation and before the
 * end of the compilation process. Therefore, we can easily apply the iPOJO
 * manipulation after the compilation and before JDT looks for modifications.
 * 
 * This way to work is under testing.
 * 
 * @author Thomas Calmant
 */
public class IPojoCompilationParticipant extends CompilationParticipant {

	/** iPOJO Manifest updater */
	private final ManifestUpdater pManifestUpdater = new ManifestUpdater();

	/** Projects to be compiled */
	private final Set<IProject> pProjectsToCompile = new HashSet<IProject>();

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.jdt.core.compiler.CompilationParticipant#buildFinished(org
	 * .eclipse.jdt.core.IJavaProject)
	 */
	@Override
	public void buildFinished(final IJavaProject aProject) {

		synchronized (pProjectsToCompile) {

			for (final IProject project : pProjectsToCompile) {

				if (!hasCompilationErrorMarkers(project)) {
					try {
						// Manipulate the project
						updateManifest(project);

					} catch (final CoreException ex) {
						Activator.logError(project,
								"Error manipulating the project", ex);
					}

				} else {
					// Error marker found: avoid working on it
					Activator.logWarning(project,
							"Project manipulation canceled: "
									+ "project has errors");
				}
			}

			// Clear the projects list
			pProjectsToCompile.clear();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.jdt.core.compiler.CompilationParticipant#buildStarting(org
	 * .eclipse.jdt.core.compiler.BuildContext[], boolean)
	 */
	@Override
	public void buildStarting(final BuildContext[] aFiles,
			final boolean aIsBatch) {

		synchronized (pProjectsToCompile) {

			for (final BuildContext file : aFiles) {
				// Prepare the list of projects to compile
				pProjectsToCompile.add(file.getFile().getProject());
			}
		}
	}

	/**
	 * Removes the iPOJO entry in the project Manifest.
	 * 
	 * @see org.eclipse.jdt.core.compiler.CompilationParticipant#cleanStarting(org
	 *      .eclipse.jdt.core.IJavaProject)
	 */
	@Override
	public void cleanStarting(final IJavaProject aProject) {

		final IProject project = aProject.getProject();

		try {
			pManifestUpdater.removeManifestEntry(project);

		} catch (final CoreException ex) {
			Activator.logError(project, "Error cleaning project", ex);
		}
	}

	/**
	 * Look for error markers in the given project.
	 * 
	 * Inspired from
	 * http://stackoverflow.com/questions/10944487/finding-number-of
	 * -errors-in-an-eclipse-project
	 * 
	 * @param aProject
	 *            Project to check for error markers
	 * @return True if the project has at least one error marker
	 */
	protected boolean hasCompilationErrorMarkers(final IProject aProject) {

		try {
			// Find JDT markers
			final IMarker[] markers = aProject.findMarkers(
					IJavaModelMarker.JAVA_MODEL_PROBLEM_MARKER, true,
					IResource.DEPTH_INFINITE);

			for (final IMarker marker : markers) {
				final Integer severityType = (Integer) marker
						.getAttribute(IMarker.SEVERITY);
				if (severityType.intValue() == IMarker.SEVERITY_ERROR) {
					return true;
				}
			}
		} catch (final CoreException ex) {
			// Error looking for... errors
		}

		// No marker found
		return false;
	}

	/**
	 * Activates the participant if the built project has the IPojo nature
	 * 
	 * @see org.eclipse.jdt.core.compiler.CompilationParticipant#isActive(org.eclipse
	 *      .jdt.core.IJavaProject)
	 * 
	 * @return True if aProject has the IPojo nature
	 */
	@Override
	public boolean isActive(final IJavaProject aJavaProject) {

		final IProject project = aJavaProject.getProject();
		if (!project.isAccessible()) {
			// Project not open
			return false;
		}

		try {
			return project.hasNature(IPojoNature.NATURE_ID);

		} catch (final CoreException e) {
			// Error ?
			Activator.logError(project,
					"Error testing nature of " + project.getName(), e);
		}

		return false;
	}

	/**
	 * Calls {@link ManifestUpdater#updateManifest(IProject)} on the current
	 * project
	 * 
	 * @param aProject
	 *            Project to manipulate
	 * 
	 * @throws CoreException
	 *             An error occurred during manipulation
	 */
	protected void updateManifest(final IProject aProject) throws CoreException {

		final IProgressMonitor monitor = new NullProgressMonitor();

		// Do the job
		final IStatus result = pManifestUpdater.updateManifest(aProject,
				monitor);

		// Log the result
		if (result.isOK()) {
			// No problem : full success
			Activator.logInfo(aProject, "Manipulation done");

		} else {
			// Errors have already been logged, so just pop a dialog
			StatusManager.getManager().handle(result, StatusManager.SHOW);
		}
	}
}
