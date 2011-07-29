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

import java.util.Iterator;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.ow2.chameleon.eclipse.ipojo.Activator;

/**
 * Add/Remove iPOJO nature popup menu action handler
 * 
 * @author Thomas Calmant
 */
public class ToggleNatureAction implements IObjectActionDelegate {

	/** Current selection */
	private ISelection pSelection;

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
	 */
	@Override
	@SuppressWarnings("rawtypes")
	public void run(final IAction aAction) {

		if (pSelection instanceof IStructuredSelection) {

			for (Iterator it = ((IStructuredSelection) pSelection).iterator(); it
					.hasNext();) {

				Object element = it.next();
				IProject project = null;

				if (element instanceof IProject) {
					project = (IProject) element;

				} else if (element instanceof IAdaptable) {
					project = (IProject) ((IAdaptable) element)
							.getAdapter(IProject.class);
				}

				if (project != null) {
					toggleNature(project);
				}
			}
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
	 * Toggles sample nature on a project
	 * 
	 * @param aProject
	 *            The project where to add or remove the iPOJO nature
	 */
	private void toggleNature(final IProject aProject) {

		try {
			IProjectDescription description = aProject.getDescription();
			String[] natures = description.getNatureIds();

			for (int i = 0; i < natures.length; ++i) {

				if (IPojoNature.NATURE_ID.equals(natures[i])) {
					// Remove the nature
					String[] newNatures = new String[natures.length - 1];
					System.arraycopy(natures, 0, newNatures, 0, i);
					System.arraycopy(natures, i + 1, newNatures, i,
							natures.length - i - 1);
					description.setNatureIds(newNatures);
					aProject.setDescription(description, null);
					return;
				}
			}

			// Add the nature at the top position : the project image is the one
			// of the first nature with an icon
			String[] newNatures = new String[natures.length + 1];
			newNatures[0] = IPojoNature.NATURE_ID;
			System.arraycopy(natures, 0, newNatures, 1, natures.length);
			description.setNatureIds(newNatures);
			aProject.setDescription(description, null);

		} catch (CoreException e) {
			Activator.logError("Error setting iPOJO nature", e);
		}
	}

}
