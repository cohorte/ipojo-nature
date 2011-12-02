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
package org.ow2.chameleon.eclipse.ipojo.classpath.ui;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.ui.wizards.IClasspathContainerPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.ow2.chameleon.eclipse.ipojo.Activator;
import org.ow2.chameleon.eclipse.ipojo.IClasspathConstants;
import org.ow2.chameleon.eclipse.ipojo.IImagesConstants;
import org.ow2.chameleon.eclipse.ipojo.classpath.container.AnnotationContainer;

/**
 * Implementation of the class path container selection page
 * 
 * @author Thomas Calmant
 */
public class AnnotationsContainerPage extends WizardPage implements
		IClasspathContainerPage {

	/**
	 * Default constructor, called when instantiating plug-in
	 */
	public AnnotationsContainerPage() {
		super("iPOJO Annotations", "iPOJO Annotations", Activator
				.getImageDescriptor(IImagesConstants.LOGO_IPOJO_SMALL));

		setPageComplete(true);
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
		final Composite pageRoot = new Composite(aParent, SWT.NONE);

		final GridLayout layout = new GridLayout(1, false);
		layout.verticalSpacing = 10;
		pageRoot.setLayout(layout);

		// Prepare the label text : JAR file path
		final String annotationJarPath = new AnnotationContainer()
				.getAnnotationLibraryPath();

		final StringBuilder builder = new StringBuilder();
		builder.append("iPOJO Annotations JAR file :\n");

		if (annotationJarPath == null) {
			builder.append("Library not found.");
			setPageComplete(false);

		} else {
			builder.append("Found at ").append(annotationJarPath);
			setPageComplete(true);
		}

		// Just add a label to the page
		final Label someText = new Label(pageRoot, SWT.WRAP);
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		someText.setLayoutData(data);
		someText.setText(builder.toString());

		// Set the control
		setControl(pageRoot);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jdt.ui.wizards.IClasspathContainerPage#finish()
	 */
	@Override
	public boolean finish() {
		// Do nothing...
		return isPageComplete();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jdt.ui.wizards.IClasspathContainerPage#getSelection()
	 */
	@Override
	public IClasspathEntry getSelection() {

		return JavaCore
				.newContainerEntry(IClasspathConstants.ANNOTATIONS_CONTAINER_PATH);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.jdt.ui.wizards.IClasspathContainerPage#setSelection(org.eclipse
	 * .jdt.core.IClasspathEntry)
	 */
	@Override
	public void setSelection(final IClasspathEntry aContainerEntry) {
		// Do nothing...
	}
}
