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
package org.ow2.chameleon.eclipse.ipojo.exporter.ui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.ow2.chameleon.eclipse.ipojo.Activator;
import org.ow2.chameleon.eclipse.ipojo.exporter.IPojoExporterPlugin;

/**
 * Bundle export configuration page
 * 
 * @author Thomas Calmant
 */
public class ExportPage extends WizardPage {

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

    /**
     * Folder selection button listener
     * 
     * @author Thomas Calmant
     */
    protected class FolderSelectionListener implements SelectionListener {

        /** Target text container */
        private final Text pTarget;

        /**
         * Prepares the listener
         * 
         * @param aTextWidget
         *            Widget that will contain the selected path
         */
        public FolderSelectionListener(final Text aTextWidget) {

            pTarget = aTextWidget;
        }

        @Override
        public void widgetDefaultSelected(final SelectionEvent aEvent) {

            // Update navigation buttons state
            getWizard().getContainer().updateButtons();
        }

        @Override
        public void widgetSelected(final SelectionEvent aEvent) {

            // Pop up a directory selection dialog
            final DirectoryDialog dialog = new DirectoryDialog(getShell(),
                    SWT.SAVE);
            dialog.setMessage("Choose a folder");
            dialog.setFilterPath(pTarget.getText());

            final String text = dialog.open();
            if (text != null) {
                pTarget.setText(text);

                // Update navigation buttons state
                getWizard().getContainer().updateButtons();
            }
        }
    }

    /**
     * A selection listener for table check all/uncheck all buttons
     * 
     * @author Thomas Calmant
     */
    protected class TableCheckButtonEvent implements SelectionListener {

        /** The selection flag to set */
        private boolean pCheckFlag;

        /** The associated table */
        private Table pTable;

        /**
         * Sets up the event handler
         * 
         * @param aTable
         *            The associated table
         * @param aCheckFlag
         *            The check box state to set to all items when selected
         */
        public TableCheckButtonEvent(final Table aTable,
                final boolean aCheckFlag) {

            pTable = aTable;
            pCheckFlag = aCheckFlag;
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * org.eclipse.swt.events.SelectionListener#widgetDefaultSelected(org
         * .eclipse.swt.events.SelectionEvent)
         */
        @Override
        public void widgetDefaultSelected(final SelectionEvent aEvent) {

            // Update navigation buttons state
            getWizard().getContainer().updateButtons();
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * org.eclipse.swt.events.SelectionListener#widgetSelected(org.eclipse
         * .swt.events.SelectionEvent)
         */
        @Override
        public void widgetSelected(final SelectionEvent aEvent) {

            // Change all items state
            for (final TableItem item : pTable.getItems()) {
                item.setChecked(pCheckFlag);
            }

            // Update navigation buttons state
            getWizard().getContainer().updateButtons();
        }
    }

    /** Flag indicating if Dialog Settings are actually set */
    public static final String SETTINGS_ARE_SET = "settingsSet";

    /** Output folder setting */
    public static final String SETTINGS_OUTPUT_FOLDER = "outputFolder";

    /** Use build.properties setting */
    public static final String SETTINGS_USE_BUILDPROPERTIES = "useBuildProperties";

    /** Simple event listener, for wizard buttons update */
    private final EventListener pEventListener = new EventListener();

    /** Initial project selection */
    private final Collection<IProject> pInitialProjectsSelection = new HashSet<IProject>();

    /** Output folder */
    private Text pOutputFolder;

    /** Wizard page root */
    private Composite pPageRoot;

    /** Projects selection table */
    private Table pProjectsTable;

    /** Dialog settings */
    private final IDialogSettings pSettings;

    /** Use build.properties check box */
    private Button pUseBuildProperties;

    /**
     * Constructor
     * 
     * @param aPageName
     *            Wizard page name
     */
    protected ExportPage(final String aPageName) {

        super(aPageName, aPageName, ImageDescriptor.createFromFile(
                Activator.class, "/icons/ipojo-small.png"));
        pSettings = IPojoExporterPlugin.getDefault().getDialogSettings();
    }

    /**
     * Adds a check box with the given label
     * 
     * @param aLabel
     *            The check box label
     * @return The created check box
     */
    protected Button addCheckBox(final String aLabel) {

        final Button button = new Button(pPageRoot, SWT.BORDER | SWT.CHECK);
        button.setText(aLabel);
        return button;
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

        /* Project selection area */
        final Group projectsGroup = new Group(pPageRoot, SWT.NONE);
        projectsGroup.setText("Projects to export :");
        projectsGroup
                .setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        createProjectSelection(projectsGroup);

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

        // Use build.properties
        pUseBuildProperties = new Button(aParent, SWT.BORDER | SWT.CHECK);
        pUseBuildProperties.setText("Use build.properties");
        pUseBuildProperties.addListener(SWT.Selection, pEventListener);
        pUseBuildProperties.setLayoutData(new GridData(SWT.BEGINNING,
                SWT.BOTTOM, true, false, 2, 1));

        // Selected, by default
        boolean checkUseBuildProperies = true;
        if (pSettings.getBoolean(SETTINGS_ARE_SET)) {
            // Settings have been set before, use them
            checkUseBuildProperies = pSettings
                    .getBoolean(SETTINGS_USE_BUILDPROPERTIES);
        }

        pUseBuildProperties.setSelection(checkUseBuildProperies);

        // Output folder
        pOutputFolder = new Text(aParent, SWT.BORDER);
        pOutputFolder.setLayoutData(new GridData(SWT.FILL, SWT.BOTTOM, true,
                false));
        pOutputFolder.addListener(SWT.Modify, pEventListener);

        // Set the default text
        final String settingsOutputFolder = pSettings
                .get(SETTINGS_OUTPUT_FOLDER);
        if (settingsOutputFolder != null) {
            pOutputFolder.setText(settingsOutputFolder);
        }

        // Output folder button
        final Button chooseFolder = new Button(aParent, SWT.RIGHT);
        chooseFolder.setText("Choose a folder");
        chooseFolder.addSelectionListener(new FolderSelectionListener(
                pOutputFolder));
    }

    /**
     * Creates the projects selection controls
     * 
     * @param aParent
     *            Parent widget
     */
    private void createProjectSelection(final Composite aParent) {

        // Set a grid layout
        aParent.setLayout(new GridLayout(2, false));

        // Prepare the project image
        final Image projectImage = PlatformUI.getWorkbench().getSharedImages()
                .getImage(IDE.SharedImages.IMG_OBJ_PROJECT);

        // Prepare the table
        pProjectsTable = new Table(aParent, SWT.BORDER | SWT.CHECK
                | SWT.H_SCROLL | SWT.V_SCROLL);
        pProjectsTable.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true,
                true, 2, 1));
        pProjectsTable.addListener(SWT.Selection, pEventListener);

        // Fill it
        for (final IProject project : ResourcesPlugin.getWorkspace().getRoot()
                .getProjects()) {

            try {
                if (project.isOpen() && project.hasNature(JavaCore.NATURE_ID)) {
                    // Only Java projects are shown
                    final TableItem item = new TableItem(pProjectsTable,
                            SWT.NONE);
                    item.setText(project.getName());
                    item.setImage(projectImage);
                    item.setData(project);

                    item.setChecked(pInitialProjectsSelection.contains(project));
                }

            } catch (final CoreException ex) {
                IPojoExporterPlugin.logWarning("Can't test nature of "
                        + project.getName(), ex);
            }
        }

        // Prepare the selection buttons
        final Button selectAllButton = new Button(aParent, SWT.PUSH);
        selectAllButton.setText("Select All");
        selectAllButton.addSelectionListener(new TableCheckButtonEvent(
                pProjectsTable, true));
        selectAllButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true,
                false));

        final Button unselectAllButton = new Button(aParent, SWT.PUSH);
        unselectAllButton.setText("Deselect All");
        unselectAllButton.addSelectionListener(new TableCheckButtonEvent(
                pProjectsTable, false));
        unselectAllButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true,
                false));
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.dialogs.DialogPage#getDescription()
     */
    @Override
    public String getDescription() {

        return "iPOJO Bundle export configuration";
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.dialogs.DialogPage#getMessage()
     */
    @Override
    public String getMessage() {

        return "Select projects to export";
    }

    /**
     * Retrieves the selected output folder
     * 
     * @return The output folder
     */
    public String getOutputFolder() {

        return pOutputFolder.getText();
    }

    /**
     * Retrieves the list of the projects to be exported (never null, only Java
     * projects)
     * 
     * @return The projects to be exported
     */
    public IProject[] getSelectedProjects() {

        final TableItem[] tableItems = pProjectsTable.getItems();
        final List<IProject> selectedProjects = new ArrayList<IProject>();

        for (final TableItem item : tableItems) {
            if (item.getChecked()) {
                // Project is checked
                final Object itemData = item.getData();
                if (itemData instanceof IProject) {
                    selectedProjects.add((IProject) itemData);
                }
            }
        }

        return selectedProjects.toArray(new IProject[0]);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.wizard.WizardPage#isPageComplete()
     */
    @Override
    public boolean isPageComplete() {

        return !pOutputFolder.getText().isEmpty()
                && getSelectedProjects().length != 0;
    }

    /**
     * Saves the page settings
     */
    public void save() {

        // Store settings
        pSettings.put(SETTINGS_USE_BUILDPROPERTIES, useBuildProperties());
        pSettings.put(SETTINGS_OUTPUT_FOLDER, getOutputFolder());

        // We now have user settings
        pSettings.put(SETTINGS_ARE_SET, true);
    }

    /**
     * Sets the initial project selection, that will be used during the project
     * table creation
     * 
     * @param aSelectedProjects
     *            Pre-selected projects
     */
    public void setSelectedProjects(final Collection<IProject> aSelectedProjects) {

        pInitialProjectsSelection.clear();

        if (aSelectedProjects != null) {
            pInitialProjectsSelection.addAll(aSelectedProjects);
        }
    }

    /**
     * "Use build.properties" check box state
     * 
     * @return True if selected
     */
    public boolean useBuildProperties() {

        return pUseBuildProperties.getSelection();
    }
}
