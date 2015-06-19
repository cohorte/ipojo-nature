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

import java.util.Set;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.ui.statushandlers.StatusManager;
import org.ow2.chameleon.eclipse.ipojo.Activator;
import org.ow2.chameleon.eclipse.ipojo.core.ManifestUpdater;

/**
 * "Update Manifest" action in MANIFEST.MF file context menu, in the navigator
 * view
 * 
 * @author Thomas Calmant
 */
public class UpdateManifestHandler extends AbstractProjectActionHandler {

	/** iPOJO Manifest updater */
	private final ManifestUpdater pManifestUpdater = new ManifestUpdater();

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

		for (final IProject project : selectedProjects) {
			try {
				// Update projects manifests
				final IStatus result = pManifestUpdater.updateManifest(project,
						null);
				if (!result.isOK()) {
					// Errors have already been logged, so just pop a dialog
					StatusManager.getManager().handle(result,
							StatusManager.SHOW);
				}

			} catch (final CoreException ex) {
				// Error manipulating file
				Activator.logError(project, "Error updating Manifest file", ex);
			}
		}

		return null;
	}
}
