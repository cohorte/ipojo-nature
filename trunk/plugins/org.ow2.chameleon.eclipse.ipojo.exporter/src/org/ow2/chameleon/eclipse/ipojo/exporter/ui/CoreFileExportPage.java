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
package org.ow2.chameleon.eclipse.ipojo.exporter.ui;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.ow2.chameleon.eclipse.ipojo.Activator;
import org.ow2.chameleon.eclipse.ipojo.exporter.IPojoExporterPlugin;

/**
 * iPOJO Core Jar file export configuration page
 * 
 * @author Thomas Calmant
 */
public class CoreFileExportPage extends WizardPage {

	/**
	 * Listener that updates the wizard buttons
	 * 
	 * @author Thomas Calmant
	 */
	protected class EventListener implements Listener {

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.eclipse.swt.widgets.Listener#handleEvent(org.eclipse.swt.widgets
		 * .Event)
		 */
		@Override
		public void handleEvent(final Event aEvent) {

			// Update navigation buttons state
			getWizard().getContainer().updateButtons();
		}
	}

	/** Flag indicating if Dialog Settings are actually set */
	public static final String SETTINGS_CORE_ARE_SET = "ipojo.core.settingsSet";

	/** Output folder setting */
	public static final String SETTINGS_CORE_OUTPUT_FOLDER = "ipojo.core.outputFolder";

	/** Output folder */
	private Text pOutputFolder;

	/** Wizard page root */
	private Composite pPageRoot;

	/** Dialog settings */
	private final IDialogSettings pSettings;

	/**
	 * Constructor
	 * 
	 * @param aPageName
	 *            Wizard page name
	 */
	protected CoreFileExportPage(final String aPageName) {

		super(aPageName, aPageName, ImageDescriptor.createFromFile(
				Activator.class, "/icons/ipojo-small.png"));
		pSettings = IPojoExporterPlugin.getDefault().getDialogSettings();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets
	 * .Composite)
	 */
	@Override
	public void createControl(final Composite aParent) {

		// Set up the page root
		pPageRoot = new Composite(aParent, SWT.NONE);

		final GridLayout layout = new GridLayout(1, false);
		layout.verticalSpacing = 10;
		pPageRoot.setLayout(layout);

		/* Export options */
		final Group exportGroup = new Group(pPageRoot, SWT.NONE);
		exportGroup.setText("Export options");
		exportGroup.setLayoutData(new GridData(SWT.FILL, SWT.BOTTOM, true,
				false));

		createExportFields(exportGroup);

		// Set the wizard page main control
		setControl(pPageRoot);
	}

	/**
	 * Prepares export options fields
	 * 
	 * @param aParent
	 *            Parent widget
	 */
	private void createExportFields(final Composite aParent) {

		aParent.setLayout(new GridLayout(2, false));

		// Output folder
		pOutputFolder = new Text(aParent, SWT.BORDER);
		pOutputFolder.setLayoutData(new GridData(SWT.FILL, SWT.BOTTOM, true,
				false));
		pOutputFolder.addListener(SWT.Modify, new EventListener());

		// Set the default text
		final String settingsOutputFolder = pSettings
				.get(SETTINGS_CORE_OUTPUT_FOLDER);
		if (settingsOutputFolder != null) {
			pOutputFolder.setText(settingsOutputFolder);
		}

		// Output folder button
		final Button chooseFolder = new Button(aParent, SWT.RIGHT);
		chooseFolder.setText("Choose a folder");
		chooseFolder.addSelectionListener(new FolderSelector(this,
				pOutputFolder));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.dialogs.DialogPage#getDescription()
	 */
	@Override
	public String getDescription() {

		return "iPOJO Core JAR file export configuration";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.dialogs.DialogPage#getMessage()
	 */
	@Override
	public String getMessage() {

		return "Select the output folder";
	}

	/**
	 * Retrieves the selected output folder
	 * 
	 * @return The output folder
	 */
	public String getOutputFolder() {

		return pOutputFolder.getText();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.wizard.WizardPage#isPageComplete()
	 */
	@Override
	public boolean isPageComplete() {

		return !pOutputFolder.getText().isEmpty();
	}

	/**
	 * Saves the page settings
	 */
	public void save() {

		// Store settings
		pSettings.put(SETTINGS_CORE_OUTPUT_FOLDER, getOutputFolder());

		// We now have user settings
		pSettings.put(SETTINGS_CORE_ARE_SET, true);
	}

}
