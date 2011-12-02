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

import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.runtime.CoreException;

/**
 * iPOJO project nature life cycle handler
 * 
 * @author Thomas Calmant
 */
public class IPojoNature implements IProjectNature {

	/** ID of this project nature */
	public static final String NATURE_ID = "org.ow2.chameleon.eclipse.ipojo.iPojoNature";

	/** The associated project */
	private IProject pProject;

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.resources.IProjectNature#configure()
	 */
	@Override
	public void configure() throws CoreException {

		final IProjectDescription description = pProject.getDescription();

		// Look for the builder
		boolean builderFound = false;
		final ICommand[] commands = description.getBuildSpec();
		for (ICommand command : commands) {
			if (IPojoBuilder.BUILDER_ID.equals(command.getBuilderName())) {
				builderFound = true;
			}
		}

		if (!builderFound) {
			// Builder not found : add it
			ICommand[] newCommands = new ICommand[commands.length + 1];
			System.arraycopy(commands, 0, newCommands, 0, commands.length);
			ICommand command = description.newCommand();
			command.setBuilderName(IPojoBuilder.BUILDER_ID);
			newCommands[newCommands.length - 1] = command;
			description.setBuildSpec(newCommands);
		}

		// Add the nature at the top position : the project image is the one
		// of the first nature with an icon
		boolean natureFound = false;
		final String[] natures = description.getNatureIds();

		for (String nature : natures) {
			if (NATURE_ID.equals(nature)) {
				natureFound = true;
			}
		}

		if (!natureFound) {
			// Nature not found : add it
			String[] newNatures = new String[natures.length + 1];
			newNatures[0] = NATURE_ID;
			System.arraycopy(natures, 0, newNatures, 1, natures.length);
			description.setNatureIds(newNatures);
		}

		// Update the project description
		pProject.setDescription(description, null);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.resources.IProjectNature#deconfigure()
	 */
	@Override
	public void deconfigure() throws CoreException {

		final IProjectDescription description = getProject().getDescription();

		// Remove the builder from the description
		final ICommand[] commands = description.getBuildSpec();
		for (int i = 0; i < commands.length; ++i) {

			if (IPojoBuilder.BUILDER_ID.equals(commands[i].getBuilderName())) {
				// Remove the builder
				ICommand[] newCommands = new ICommand[commands.length - 1];
				System.arraycopy(commands, 0, newCommands, 0, i);
				System.arraycopy(commands, i + 1, newCommands, i,
						commands.length - i - 1);
				description.setBuildSpec(newCommands);
				break;
			}
		}

		// Remove the nature from the description
		final String[] natures = description.getNatureIds();
		for (int i = 0; i < natures.length; ++i) {

			if (NATURE_ID.equals(natures[i])) {
				// Remove the nature
				String[] newNatures = new String[natures.length - 1];
				System.arraycopy(natures, 0, newNatures, 0, i);
				System.arraycopy(natures, i + 1, newNatures, i, natures.length
						- i - 1);
				description.setNatureIds(newNatures);
				break;
			}
		}

		// Update the project description at once
		pProject.setDescription(description, null);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.resources.IProjectNature#getProject()
	 */
	@Override
	public IProject getProject() {
		return pProject;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.core.resources.IProjectNature#setProject(org.eclipse.core
	 * .resources.IProject)
	 */
	@Override
	public void setProject(final IProject project) {
		this.pProject = project;
	}
}
