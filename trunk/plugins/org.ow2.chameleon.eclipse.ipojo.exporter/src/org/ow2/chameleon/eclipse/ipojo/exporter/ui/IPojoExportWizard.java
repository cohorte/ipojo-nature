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
	private ExportPage pExportPage;

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.wizard.Wizard#addPages()
	 */
	@Override
	public void addPages() {
		addPage(pExportPage);
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
		pExportPage = new ExportPage("iPOJO Bundle Export");

		// Get all selected projects
		final Set<IProject> selectedProjects = new HashSet<IProject>();
		for (Object selectedObject : aSelection.toArray()) {

			if (selectedObject instanceof IResource) {
				// Add the project to the set
				selectedProjects.add(((IResource) selectedObject).getProject());

			} else if (selectedObject instanceof IAdaptable) {
				// Try with an adapter
				final IResource resource = (IResource) ((IAdaptable) selectedObject)
						.getAdapter(IResource.class);
				if (resource != null) {
					selectedProjects.add(resource.getProject());
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
		for (IProject project : pExportPage.getSelectedProjects()) {

			try {
				pExporter.exportBundle(project, new NullProgressMonitor());

			} catch (CoreException e) {
				IPojoExporterPlugin.logError("Error exporting bundle", e);
			}
		}

		return true;
	}
}
