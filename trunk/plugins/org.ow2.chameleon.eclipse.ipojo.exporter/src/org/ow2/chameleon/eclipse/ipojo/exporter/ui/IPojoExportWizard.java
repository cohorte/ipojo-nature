/*
 * Copyright 2013 OW2 Chameleon
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

import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IExportWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkingSet;
import org.ow2.chameleon.eclipse.ipojo.exporter.IPojoExporterPlugin;
import org.ow2.chameleon.eclipse.ipojo.exporter.core.BundleExporter;

/**
 * iPOJO bundle export wizard
 * 
 * @author Thomas Calmant
 */
public class IPojoExportWizard extends Wizard implements IExportWizard {

    /** Bundle exporter */
    private final BundleExporter pExporter = new BundleExporter();

    /** Export configuration page */
    private BundleExportPage pExportPage;

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.wizard.Wizard#addPages()
     */
    @Override
    public void addPages() {

        addPage(pExportPage);
    }

    /**
     * Tries to get the project of the given adaptable element, adapting it to
     * an IRessource.
     * 
     * @param aAdaptable
     *            An adaptable object
     * @return The project containing the element, or null
     */
    private IProject getProject(final IAdaptable aAdaptable) {

        if (aAdaptable == null) {
            // Nothing to do here
            return null;
        }

        // Try to adapt element to a resource
        final IResource resource = (IResource) (aAdaptable)
                .getAdapter(IResource.class);

        if (resource != null) {
            // Project found
            return resource.getProject();
        }

        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.ui.IWorkbenchWizard#init(org.eclipse.ui.IWorkbench,
     * org.eclipse.jface.viewers.IStructuredSelection)
     */
    @Override
    public void init(final IWorkbench aWorkbench,
            final IStructuredSelection aSelection) {

        // Prepare the wizard page
        setWindowTitle("iPOJO Bundle export wizard");

        // Prepare the export page
        pExportPage = new BundleExportPage("iPOJO Bundle Export");

        // Get all selected projects
        final Set<IProject> selectedProjects = new HashSet<IProject>();
        for (final Object selectedObject : aSelection.toArray()) {

            if (selectedObject instanceof IResource) {
                // Resource selected : add its parent project to the set
                selectedProjects.add(((IResource) selectedObject).getProject());

            } else if (selectedObject instanceof IWorkingSet) {
                // Working set selected : add all projects it contains
                for (final IAdaptable element : ((IWorkingSet) selectedObject)
                        .getElements()) {
                    final IProject project = getProject(element);
                    if (project != null) {
                        selectedProjects.add(project);
                    }
                }

            } else if (selectedObject instanceof IAdaptable) {
                // Try with an adapter
                final IProject project = getProject((IAdaptable) selectedObject);
                if (project != null) {
                    selectedProjects.add(project);
                }
            }
        }

        // Set up the export page selection
        pExportPage.setSelectedProjects(selectedProjects);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.wizard.Wizard#performFinish()
     */
    @Override
    public boolean performFinish() {

        // Save page settings
        pExportPage.save();

        // Set up the exporter
        pExporter.setUseBuildProperties(pExportPage.useBuildProperties());
        pExporter.setOutputFolder(pExportPage.getOutputFolder());

        // Export each project
        for (final IProject project : pExportPage.getSelectedProjects()) {

            try {
                pExporter.exportBundle(project, new NullProgressMonitor());

            } catch (final CoreException e) {
                IPojoExporterPlugin.logError("Error exporting bundle", e);
            }
        }

        return true;
    }
}
