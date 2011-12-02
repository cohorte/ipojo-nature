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
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

/**
 * Pops up an iPOJO nature configuration dialog
 * 
 * @author Thomas Calmant
 */
public class NatureConfigurationDialog extends TitleAreaDialog {

	/** The "Add annotations" box */
	private Button pAnnotationsBox;

	/** The "Create metadata.xml" box */
	private Button pMetadataBox;

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
		pAnnotationsBox.setText("Add annotations to build path");

		// Prepare the metadata template box
		pMetadataBox = new Button(composite, SWT.CHECK);
		pMetadataBox.setLayoutData(gridData);
		pMetadataBox.setText("Create a metadata.xml template.");

		// Tests available if only one project is selected
		if (pProjects.size() == 1) {
			// The template can be create only if there is no existing metadata
			// file
			pMetadataBox.setEnabled((pProjects.get(0).findMember(
					"/metadata.xml") == null));

			// TODO check annotation box if there is already an annotation
			// library
			// in the build path of the project
		}

		return composite;
	}

	/**
	 * Tests if the "Add annotations library" box is checked
	 * 
	 * @return True if the box is checked
	 */
	public boolean isAddAnnotationsBoxChecked() {
		return pAnnotationsBox.getSelection();
	}

	/**
	 * Tests if the "Create metadata.xml template" box is checked
	 * 
	 * @return True if the box is checked
	 */
	public boolean isCreateMetadataBoxChecked() {
		return pMetadataBox.getSelection();
	}
}
