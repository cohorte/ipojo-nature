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

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * Abstract class extending {@link AbstractHandler} to provide utility methods
 * 
 * @author Thomas Calmant
 */
public abstract class AbstractProjectActionHandler extends AbstractHandler {

	/**
	 * Returns the list of projects selected in the active UI window
	 * 
	 * @param aEvent
	 *            An execution event
	 * @return A list of selected projects
	 */
	public Set<IProject> getSelectedProjects(final ExecutionEvent aEvent) {

		// Get the selected projects
		final Set<IProject> selectedProjects = new LinkedHashSet<IProject>();

		final ISelection selection = HandlerUtil
				.getActiveWorkbenchWindow(aEvent).getActivePage()
				.getSelection();
		if (selection instanceof IStructuredSelection) {
			// Filter selection
			final Iterator<?> iterator = ((IStructuredSelection) selection)
					.iterator();
			while (iterator.hasNext()) {
				final Object element = iterator.next();
				final IResource resource;

				if (element instanceof IResource) {
					// Resource selected
					resource = (IResource) element;

				} else if (element instanceof IAdaptable) {
					// Adapatable item selected
					resource = (IResource) ((IAdaptable) element)
							.getAdapter(IResource.class);

				} else {
					// Unhandled type of selection
					continue;
				}

				if (resource instanceof IProject) {
					// A project is selected
					selectedProjects.add(((IProject) resource));

				} else if (resource instanceof IResource) {
					// A resource is selected: get its parent project
					selectedProjects.add(resource.getProject());
				}
			}
		}

		// Remove the "null" value
		selectedProjects.remove(null);

		return selectedProjects;
	}
}
