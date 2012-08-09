/*
 * Copyright 2012 OW2 Chameleon
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

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.actions.ActionDelegate;
import org.eclipse.ui.statushandlers.StatusManager;
import org.ow2.chameleon.eclipse.ipojo.Activator;
import org.ow2.chameleon.eclipse.ipojo.core.ManifestUpdater;

/**
 * "Update Manifest" action in MANIFEST.MF file contextual menu, in the
 * navigator view
 * 
 * @author Thomas Calmant
 */
public class UpdateMF extends ActionDelegate implements IObjectActionDelegate {

	/** iPOJO Manifest updater */
	private final ManifestUpdater pManifestUpdater;

	/** Current file selection */
	private TreeSelection pSelection;

	/** Parent pShell (for dialogs) */
	private Shell pShell;

	/**
	 * Constructor for Manifest Update action.
	 */
	public UpdateMF() {
		super();
		pManifestUpdater = new ManifestUpdater();
	}

	/**
	 * Performs the given action
	 * 
	 * @see org.eclipse.ui.IActionDelegate#run(IAction)
	 */
	@Override
	public void run(final IAction action) {

		if (pSelection.size() == 0
				|| !(pSelection.getFirstElement() instanceof IFile)) {
			Activator.logInfo(null, "No file selected");
			return;
		}

		// Search for manifest
		final IFile manifestIFile = (IFile) pSelection.getFirstElement();
		final IProject project = manifestIFile.getProject();

		try {
			final IStatus result = pManifestUpdater.updateManifest(project,
					null);

			if (!result.isOK()) {
				// Errors have already been logged, so just pop a dialog
				StatusManager.getManager().handle(result, StatusManager.SHOW);
			}

		} catch (final CoreException ex) {
			MessageDialog.openError(pShell, "iPOJO Updater Error",
					"Error while updating the Manifest : " + ex);

			Activator.logError(project, "iPOJO update Manifest action error",
					ex);
		}
	}

	/**
	 * Called when a selection has been changed
	 * 
	 * @see ActionDelegate#selectionChanged(IAction, ISelection)
	 */
	@Override
	public void selectionChanged(final IAction action,
			final ISelection selection) {
		super.selectionChanged(action, selection);

		if (selection instanceof TreeSelection) {
			pSelection = (TreeSelection) selection;

		} else {
			pSelection = null;
		}
	}

	/**
	 * @see IObjectActionDelegate#setActivePart(IAction, IWorkbenchPart)
	 */
	@Override
	public void setActivePart(final IAction action,
			final IWorkbenchPart targetPart) {
		pShell = targetPart.getSite().getShell();
	}
}
