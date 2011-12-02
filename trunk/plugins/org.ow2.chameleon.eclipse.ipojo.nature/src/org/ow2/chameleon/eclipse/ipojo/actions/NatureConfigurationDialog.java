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
package org.ow2.chameleon.eclipse.ipojo.actions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.ow2.chameleon.eclipse.ipojo.Activator;
import org.ow2.chameleon.eclipse.ipojo.IClasspathConstants;
import org.ow2.chameleon.eclipse.ipojo.IImagesConstants;

/**
 * Pops up an iPOJO nature configuration dialog
 * 
 * @author Thomas Calmant
 */
public class NatureConfigurationDialog extends TitleAreaDialog {

	/** The "Use annotations" box */
	private Button pAnnotationsBox;

	/** Stores the "Use annotations box state */
	private boolean pAnnotationsBoxChecked;

	/** The "Create metadata.xml" box */
	private Button pMetadataBox;

	/** Stores the "Create metadata.xml" box state */
	private boolean pMetadataBoxChecked;

	/** The modified project */
	private final List<IProject> pProjects;

	/**
	 * Instantiate a new configuration dialog
	 * 
	 * @param aParentShell
	 *            The parent shell
	 * @param aProjects
	 *            Projects that will be configured
	 */
	public NatureConfigurationDialog(final Shell aParentShell,
			final Collection<IProject> aProjects) {

		super(aParentShell);

		// Copy the projects list
		pProjects = new ArrayList<IProject>(aProjects);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.jface.dialogs.TitleAreaDialog#createContents(org.eclipse.
	 * swt.widgets.Composite)
	 */
	@Override
	protected Control createContents(final Composite aParent) {

		// Call parent
		final Control content = super.createContents(aParent);

		// Set the title
		setTitle("iPOJO Nature configuration");

		// Set the image
		setTitleImage(Activator.getImageDescriptor(
				IImagesConstants.LOGO_IPOJO_SMALL).createImage());

		// Set the message
		final StringBuilder builder = new StringBuilder(
				"Set up the iPOJO nature for ");
		builder.append(pProjects.size()).append(" projects");
		setMessage(builder.toString());

		return content;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.jface.dialogs.TitleAreaDialog#createDialogArea(org.eclipse
	 * .swt.widgets.Composite)
	 */
	@Override
	protected Control createDialogArea(final Composite aParent) {

		// Call parent
		final Composite composite = (Composite) super.createDialogArea(aParent);

		final GridLayout layout = new GridLayout(1, false);
		composite.setLayout(layout);

		final GridData gridData = new GridData(GridData.FILL_HORIZONTAL);

		// Prepare the annotations box
		pAnnotationsBox = new Button(composite, SWT.CHECK);
		pAnnotationsBox.setLayoutData(gridData);
		pAnnotationsBox.setText("Use annotations");

		// Prepare the metadata template box
		pMetadataBox = new Button(composite, SWT.CHECK);
		pMetadataBox.setLayoutData(gridData);
		pMetadataBox.setText("Create a metadata.xml template.");

		// Tests available if only one project is selected
		if (pProjects.size() == 1) {

			final IProject project = pProjects.get(0);

			// The template can be create only if there is no existing metadata
			// file
			pMetadataBox
					.setEnabled((project.findMember("/metadata.xml") == null));

			// Check annotation box if there is already an annotation
			// library in the build path of the project
			pAnnotationsBox.setSelection(hasAnnotations(project));
		}

		return composite;
	}

	/**
	 * Tests if the iPOJO annotations class path container is already present in
	 * the project build path.
	 * 
	 * @param aProject
	 *            Project to be tested
	 * @return True on present, false if not or on error
	 */
	protected boolean hasAnnotations(final IProject aProject) {

		final IClasspathEntry[] classpath;

		try {
			// Get the class path
			final IJavaProject javaProject = (IJavaProject) aProject
					.getNature(JavaCore.NATURE_ID);

			classpath = javaProject.getRawClasspath();

		} catch (CoreException e) {
			Activator.logError(aProject, "Error retrieving classpath", e);
			return false;
		}

		// Get project class path entries
		for (IClasspathEntry entry : classpath) {

			// We're looking for a specific container
			if (entry.getEntryKind() == IClasspathEntry.CPE_CONTAINER
					&& IClasspathConstants.ANNOTATIONS_CONTAINER_PATH
							.equals(entry.getPath())) {

				// Found !
				return true;
			}
		}

		return false;
	}

	/**
	 * Tests if the "Use annotations library" box is checked
	 * 
	 * @return True if the box is checked
	 */
	public boolean isAnnotationsBoxChecked() {
		return pAnnotationsBoxChecked;
	}

	/**
	 * Tests if the "Create metadata.xml template" box is checked
	 * 
	 * @return True if the box is checked
	 */
	public boolean isCreateMetadataBoxChecked() {
		return pMetadataBoxChecked;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.dialogs.Dialog#okPressed()
	 */
	@Override
	protected void okPressed() {

		// Store check boxes state, before their disposal
		pAnnotationsBoxChecked = pAnnotationsBox.getSelection();
		pMetadataBoxChecked = pMetadataBox.getSelection();

		super.okPressed();
	}
}
