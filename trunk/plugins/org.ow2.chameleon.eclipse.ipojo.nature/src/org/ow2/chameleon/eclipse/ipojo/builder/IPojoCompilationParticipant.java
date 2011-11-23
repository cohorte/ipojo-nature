/*
 * Copyright 2011 OW2 Chameleon
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

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
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

	/** Last project built */
	private IProject pLastProject;

	/** iPOJO Manifest updater */
	private final ManifestUpdater pManifestUpdater = new ManifestUpdater();

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.jdt.core.compiler.CompilationParticipant#aboutToBuild(org
	 * .eclipse.jdt.core.IJavaProject)
	 */
	@Override
	public int aboutToBuild(final IJavaProject aProject) {

		pLastProject = aProject.getProject();
		return READY_FOR_BUILD;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.jdt.core.compiler.CompilationParticipant#buildFinished(org
	 * .eclipse.jdt.core.IJavaProject)
	 */
	@Override
	public void buildFinished(final IJavaProject aProject) {

		// Forget the last project
		pLastProject = null;
	}

	/**
	 * Removes the iPOJO entry in the project Manifest.
	 * 
	 * @see org.eclipse.jdt.core.compiler.CompilationParticipant#cleanStarting(org.eclipse.jdt.core.IJavaProject)
	 */
	@Override
	public void cleanStarting(final IJavaProject aProject) {

		final IProject project = aProject.getProject();

		try {
			pManifestUpdater.removeManifestEntry(project);

		} catch (CoreException ex) {
			Activator.logError(project, "Error cleaning project", ex);
		}
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
	public boolean isActive(final IJavaProject aProject) {

		return IPojoNature.isIPojoProject(aProject.getProject());
	}

	/**
	 * Defines this participant as an annotation processor
	 * 
	 * @return Always true
	 * 
	 * @see org.eclipse.jdt.core.compiler.CompilationParticipant#isAnnotationProcessor()
	 */
	@Override
	public boolean isAnnotationProcessor() {
		return true;
	}

	/**
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jdt.core.compiler.CompilationParticipant#processAnnotations(org.eclipse.jdt.core.compiler.BuildContext[])
	 */
	@Override
	public void processAnnotations(final BuildContext[] aFiles) {

		final IProject project;

		if (aFiles.length == 0) {
			// No files ? Special case : work on the last seen project
			project = pLastProject;

		} else {
			// Use the project containing the first file in build context
			project = aFiles[0].getFile().getProject();
		}

		try {
			// Manipulation
			updateManifest(project);

		} catch (CoreException ex) {
			Activator.logError(project, "Error manipulating project", ex);
		}
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

		IProgressMonitor monitor = new NullProgressMonitor();

		// Do the job
		final IStatus result = pManifestUpdater.updateManifest(aProject,
				monitor);

		// Store the manipulation time
		aProject.setSessionProperty(IPojoBuilder.PROJECT_LAST_MANIPULATION,
				Long.valueOf(System.currentTimeMillis()));

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
