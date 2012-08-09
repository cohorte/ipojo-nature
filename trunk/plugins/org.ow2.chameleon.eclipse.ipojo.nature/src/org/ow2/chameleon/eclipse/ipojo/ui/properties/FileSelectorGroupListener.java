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
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.variables.VariablesPlugin;
import org.eclipse.debug.ui.StringVariableSelectionDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.ResourceListSelectionDialog;

/**
 * Handles file selection events
 * 
 * @author Thomas Calmant
 */
public class FileSelectorGroupListener implements SelectionListener {

	/** File system selection base path */
	private String pBasePath = "";

	/** File system button */
	private Button pFileSystemButton;

	/** Path text widget */
	private final Text pPathWidget;

	/** SWT shell */
	private final Shell pShell;

	/** Variables button */
	private Button pVariablesButton;

	/** Workspace button */
	private Button pWorkspaceButton;

	/**
	 * Prepares the listener
	 * 
	 * @param aTextWidget
	 *            Text widget that will contain the selected path
	 */
	public FileSelectorGroupListener(final Shell aShell, final Text aTextWidget) {

		pShell = aShell;
		pPathWidget = aTextWidget;
	}

	/**
	 * Prompts the user to choose a working directory from the file system.
	 */
	protected void handleFileSystemSelected() {

		final FileDialog dialog = new FileDialog(pShell, SWT.OPEN);
		dialog.setText("Choose a file");

		String basePath = pPathWidget.getText();
		if (basePath.isEmpty()) {
			basePath = pBasePath;
		}
		dialog.setFilterPath(basePath);

		final String text = dialog.open();
		if (text != null) {
			pPathWidget.setText(text);
		}
	}

	/**
	 * A variable entry button has been pressed for the given text field. Prompt
	 * the user for a variable and enter the result in the project field.
	 */
	private void handleVariablesSelected() {

		final StringVariableSelectionDialog dialog = new StringVariableSelectionDialog(
				pShell);
		dialog.open();

		final String variable = dialog.getVariableExpression();
		if (variable != null) {
			pPathWidget.insert(variable);
		}
	}

	/**
	 * Prompts the user for a working directory location within the workspace
	 * and sets the working directory as a String containing the workspace_loc
	 * variable or <code>null</code> if no location was obtained from the user.
	 */
	protected void handleWorkspaceSelected() {

		final ResourceListSelectionDialog containerDialog = new ResourceListSelectionDialog(
				pShell, ResourcesPlugin.getWorkspace().getRoot(),
				IResource.FILE);
		containerDialog.open();

		final Object[] resource = containerDialog.getResult();
		String text = null;
		if (resource != null && resource.length > 0) {
			text = newVariableExpression("workspace_loc",
					((IResource) resource[0]).getFullPath().toString());
		}

		if (text != null) {
			pPathWidget.setText(text);
		}
	}

	/**
	 * Returns a new variable expression with the given variable and the given
	 * argument.
	 * 
	 * @see org.eclipse.core.variables.IStringVariableManager#generateVariableExpression(String,
	 *      String)
	 */
	protected String newVariableExpression(final String aVarName,
			final String aArgument) {

		return VariablesPlugin.getDefault().getStringVariableManager()
				.generateVariableExpression(aVarName, aArgument);
	}

	/**
	 * Sets the default base path (if the text widget is empty) when using file
	 * system selection.
	 * 
	 * @param aBasePath
	 *            The default base path
	 */
	protected void setDefaultBaseFilePath(final String aBasePath) {
		pBasePath = aBasePath;
	}

	/**
	 * Sets the File System button to listen
	 * 
	 * @param aFileSystemButton
	 *            the File System button to listen
	 */
	public void setFileSystemButton(final Button aFileSystemButton) {
		pFileSystemButton = aFileSystemButton;
	}

	/**
	 * Sets the variables button to listen
	 * 
	 * @param aVariablesButton
	 *            the variables button to listen
	 */
	public void setVariablesButton(final Button aVariablesButton) {
		pVariablesButton = aVariablesButton;
	}

	/**
	 * Sets the workspace button to listen
	 * 
	 * @param aWorkspaceButton
	 *            the workspace button to listen
	 */
	public void setWorkspaceButton(final Button aWorkspaceButton) {
		pWorkspaceButton = aWorkspaceButton;
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
		// Do nothing
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.swt.events.SelectionListener#widgetSelected(org.eclipse.swt
	 * .events.SelectionEvent)
	 */
	@Override
	public void widgetSelected(final SelectionEvent aEvent) {

		final Object source = aEvent.getSource();

		if (source.equals(pWorkspaceButton)) {
			// Workspace folder selection
			handleWorkspaceSelected();

		} else if (source.equals(pFileSystemButton)) {
			// File system selection
			handleFileSystemSelected();

		} else if (source.equals(pVariablesButton)) {
			// Variables insertion
			handleVariablesSelected();
		}
	}
}
