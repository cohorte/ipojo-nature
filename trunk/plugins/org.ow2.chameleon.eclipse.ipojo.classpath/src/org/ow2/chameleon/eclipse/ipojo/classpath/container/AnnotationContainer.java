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
package org.ow2.chameleon.eclipse.ipojo.classpath.container;

import org.apache.felix.ipojo.annotations.Component;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.JavaCore;
import org.ow2.chameleon.eclipse.ipojo.IClasspathConstants;
import org.ow2.chameleon.eclipse.ipojo.classpath.IPojoClasspathPlugin;

/**
 * Implementation of the annotation class path container.
 *
 * Uses {@link BundleURLConnection} to find the JAR file.
 *
 * @author Thomas Calmant
 */
public class AnnotationContainer implements IClasspathContainer {

	/** The found annotation library path */
	private String pAnnotationLibraryPath;

	/**
	 * Retrieves the computed annotation library path
	 *
	 * @return The computed path
	 */
	public String getAnnotationLibraryPath() {

		if (pAnnotationLibraryPath == null) {
			pAnnotationLibraryPath = new BundleClassFinder()
					.getJarFile(Component.class);
		}

		return pAnnotationLibraryPath;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jdt.core.IClasspathContainer#getClasspathEntries()
	 */
	@Override
	public IClasspathEntry[] getClasspathEntries() {

		if (!hasAnnotationLibrary()) {
			// JAR not found
			IPojoClasspathPlugin.logWarning(
					"iPOJO Annotations library not found.", null);

			return new IClasspathEntry[0];
		}

		// Prepare a path
		final IPath annotationPath = new Path(pAnnotationLibraryPath);

		// Create the corresponding entry
		final IClasspathEntry annotationEntry = JavaCore.newLibraryEntry(
				annotationPath, null, null);

		return new IClasspathEntry[] { annotationEntry };
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jdt.core.IClasspathContainer#getDescription()
	 */
	@Override
	public String getDescription() {

		return "iPOJO Annotations";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jdt.core.IClasspathContainer#getKind()
	 */
	@Override
	public int getKind() {

		return K_SYSTEM;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jdt.core.IClasspathContainer#getPath()
	 */
	@Override
	public IPath getPath() {

		return IClasspathConstants.ANNOTATIONS_CONTAINER_PATH;
	}

	/**
	 * Tries to find the iPOJO annotations library path.
	 *
	 * Does nothing if the path has already been found
	 *
	 * @return True if the path has been found, else false
	 */
	protected boolean hasAnnotationLibrary() {

		return getAnnotationLibraryPath() != null;
	}
}
