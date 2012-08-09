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
package org.ow2.chameleon.eclipse.ipojo.ui.properties;

import org.eclipse.core.resources.IResource;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.PropertyPage;
import org.ow2.chameleon.eclipse.ipojo.core.Utilities;

/**
 * Project property page for iPOJO projects.
 * 
 * Allows to change the metadata.xml file to use for manipulation.
 * 
 * @author Thomas Calmant
 */
public class ProjectPropertyPage extends PropertyPage {

	/** Metadata file path field */
	private Text pMetadataPath;

	/**
	 * Creates the content of the property page
	 * 
	 * @see org.eclipse.jface.preference.PreferencePage#createContents(org.eclipse
	 *      .swt.widgets.Composite)
	 */
	@Override
	protected Control createContents(final Composite aParent) {

		// Get the associated resource (could be null)
		final IResource resource = (IResource) getElement().getAdapter(
				IResource.class);

		// Set up the root container
		final Composite pageRoot = new Composite(aParent, SWT.NULL);

		final GridLayout layout = new GridLayout(1, false);
		layout.verticalSpacing = 10;
		pageRoot.setLayout(layout);

		// Use a group (better visual aspect)
		final Group metadataGroup = new Group(pageRoot, SWT.BORDER);
		metadataGroup.setText("Metadata XML file");
		metadataGroup
				.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		metadataGroup.setLayout(new GridLayout(1, false));

		// Add the text element
		pMetadataPath = new Text(metadataGroup, SWT.BORDER);
		pMetadataPath
				.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

		// Get the stored value, if any
		pMetadataPath.setText(Utilities.INSTANCE
				.getMetadataFileProperty(resource));

		// Add the path selector
		createPathSelectionGroup(metadataGroup, pMetadataPath, resource);

		return pageRoot;
	}

	/**
	 * Create a 3-button path selection group : workspace, file system,
	 * variables
	 * 
	 * @param aParent
	 *            Parent element
	 */
	protected void createPathSelectionGroup(final Composite aParent,
			final Text aPathWidget, final IResource aSelectedResource) {

		// Listener preparation
		final FileSelectorGroupListener listener = new FileSelectorGroupListener(
				getShell(), aPathWidget);

		if (aSelectedResource != null) {
			listener.setDefaultBaseFilePath(aSelectedResource.getLocation()
					.toString());
		}

		// 3-buttons group
		final Composite buttonComposite = new Composite(aParent, SWT.NONE);
		buttonComposite.setLayout(new GridLayout(3, true));
		buttonComposite.setLayoutData(new GridData(SWT.END, SWT.TOP, true,
				false));

		// Workspace folder selection
		final Button workspaceLocationButton = new Button(buttonComposite,
				SWT.PUSH);
		workspaceLocationButton.setText("Workspace...");
		workspaceLocationButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL,
				true, false));
		listener.setWorkspaceButton(workspaceLocationButton);
		workspaceLocationButton.addSelectionListener(listener);

		// File system folder selection
		final Button fileSystemLocationButton = new Button(buttonComposite,
				SWT.PUSH);
		fileSystemLocationButton.setText("File System...");
		fileSystemLocationButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL,
				true, false));
		listener.setFileSystemButton(fileSystemLocationButton);
		fileSystemLocationButton.addSelectionListener(listener);

		// Variables injection
		final Button variablesLocationButton = new Button(buttonComposite,
				SWT.PUSH);
		variablesLocationButton.setText("Variables");
		variablesLocationButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL,
				true, false));
		listener.setVariablesButton(variablesLocationButton);
		variablesLocationButton.addSelectionListener(listener);
	}

	/**
	 * Called when the properties must be reset to default values
	 * 
	 * @see org.eclipse.jface.preference.PreferencePage#performDefaults()
	 */
	@Override
	protected void performDefaults() {

		// Reset file path
		pMetadataPath.setText("");
		super.performDefaults();
	}

	/**
	 * Called when the modifications must be applied
	 * 
	 * @see org.eclipse.jface.preference.PreferencePage#performOk()
	 */
	@Override
	public boolean performOk() {

		// Get the selected resource
		final IResource resource = (IResource) getElement().getAdapter(
				IResource.class);
		if (resource == null) {
			return false;
		}

		// Store the persistent property
		if (!Utilities.INSTANCE.setMetadataFileProperty(resource,
				pMetadataPath.getText())) {
			return false;
		}

		return super.performOk();
	}
}
