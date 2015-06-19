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

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Text;

/**
 * Folder selector, storing the result in a Text field
 * 
 * @author Thomas Calmant
 */
class FolderSelector implements SelectionListener {

	/** The associated wizard page */
	private final WizardPage pPage;

	/** Target text container */
	private final Text pTarget;

	/**
	 * Sets up the folder selector
	 * 
	 * @param aWizard
	 *            Parent wizard page
	 * @param aTextWidget
	 *            Widget that will contain the selected path
	 */
	public FolderSelector(final WizardPage aWizard, final Text aTextWidget) {

		pPage = aWizard;
		pTarget = aTextWidget;
	}

	/**
	 * Updates the navigation buttons
	 */
	private void updateButtons() {

		pPage.getWizard().getContainer().updateButtons();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.swt.events.SelectionListener#widgetDefaultSelected(org.eclipse
	 * .swt.events.SelectionEvent)
	 */
	@Override
	public void widgetDefaultSelected(final SelectionEvent aEvent) {

		// Update navigation buttons state
		updateButtons();
	}

	@Override
	public void widgetSelected(final SelectionEvent aEvent) {

		// Pop up a directory selection dialog
		final DirectoryDialog dialog = new DirectoryDialog(pPage.getShell(),
				SWT.SAVE);
		dialog.setMessage("Choose a folder");
		dialog.setFilterPath(pTarget.getText());

		final String text = dialog.open();
		if (text != null) {
			pTarget.setText(text);

			// Update navigation buttons state
			updateButtons();
		}
	}
}
