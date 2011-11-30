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
package org.ow2.chameleon.eclipse.ipojo.classpath.container;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.ClasspathContainerInitializer;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.ow2.chameleon.eclipse.ipojo.classpath.IPojoClasspathPlugin;

/**
 * Simple container initializer, setting up a project class path
 * 
 * @author Thomas Calmant
 */
public class AnnotationContainerInitializer extends
		ClasspathContainerInitializer {

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jdt.core.ClasspathContainerInitializer#
	 * canUpdateClasspathContainer(org.eclipse.core.runtime.IPath,
	 * org.eclipse.jdt.core.IJavaProject)
	 */
	@Override
	public boolean canUpdateClasspathContainer(final IPath aContainerPath,
			final IJavaProject aProject) {

		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.jdt.core.ClasspathContainerInitializer#getDescription(org
	 * .eclipse.core.runtime.IPath, org.eclipse.jdt.core.IJavaProject)
	 */
	@Override
	public String getDescription(final IPath aContainerPath,
			final IJavaProject aProject) {

		return "iPOJO Annotations";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.jdt.core.ClasspathContainerInitializer#initialize(org.eclipse
	 * .core.runtime.IPath, org.eclipse.jdt.core.IJavaProject)
	 */
	@Override
	public void initialize(final IPath aContainerPath,
			final IJavaProject aProject) throws CoreException {

		// The container
		final AnnotationContainer annotationContainer = new AnnotationContainer();

		if (annotationContainer.findAnnotationLibrary()) {
			// JAR found : setup the project class path
			JavaCore.setClasspathContainer(aContainerPath,
					new IJavaProject[] { aProject },
					new IClasspathContainer[] { annotationContainer }, null);

		} else {
			// Nothing available
			IPojoClasspathPlugin.logWarning(
					"iPOJO Annotations JAR file not found.", null);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jdt.core.ClasspathContainerInitializer#
	 * requestClasspathContainerUpdate(org.eclipse.core.runtime.IPath,
	 * org.eclipse.jdt.core.IJavaProject,
	 * org.eclipse.jdt.core.IClasspathContainer)
	 */
	@Override
	public void requestClasspathContainerUpdate(final IPath aContainerPath,
			final IJavaProject aProject,
			final IClasspathContainer aContainerSuggestion)
			throws CoreException {

		JavaCore.setClasspathContainer(aContainerPath,
				new IJavaProject[] { aProject },
				new IClasspathContainer[] { aContainerSuggestion }, null);
	}
}
