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

import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;

import org.apache.felix.ipojo.annotations.Component;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.osgi.framework.internal.core.BundleURLConnection;
import org.ow2.chameleon.eclipse.ipojo.IClasspathConstants;
import org.ow2.chameleon.eclipse.ipojo.classpath.IPojoClasspathPlugin;

/**
 * Implementation of the annotation class path container.
 * 
 * Uses {@link BundleURLConnection} to find the JAR file.
 * 
 * @author Thomas Calmant
 */
@SuppressWarnings("restriction")
public class AnnotationContainer implements IClasspathContainer {

	/** The found annotation library path */
	private String pAnnotationLibraryPath;

	/**
	 * Tries to find the iPOJO annotations library path.
	 * 
	 * Does nothing if the path has already been found
	 * 
	 * @return True if the path has been found, else false
	 */
	protected boolean findAnnotationLibrary() {

		if (pAnnotationLibraryPath != null) {
			// We already found the annotation path
			return true;
		}

		// Get the class
		final Class<?> annotationClass = Component.class;

		// Get the class file URL
		final URL classUrl = annotationClass.getResource(annotationClass
				.getSimpleName() + ".class");

		if (classUrl == null) {
			// Class file not found
			IPojoClasspathPlugin.logError(
					"Can't find the annotation class URL", null);
			return false;
		}

		// Get the connection
		URLConnection urlConn;
		try {
			urlConn = classUrl.openConnection();

		} catch (IOException ex) {
			IPojoClasspathPlugin.logError("Error opening JAR connection to "
					+ classUrl, ex);
			return false;
		}

		// Get the Bundle URL
		if (urlConn instanceof BundleURLConnection) {

			final BundleURLConnection bundleUrlConn = (BundleURLConnection) urlConn;

			try {
				urlConn = bundleUrlConn.getLocalURL().openConnection();

			} catch (IOException ex) {
				IPojoClasspathPlugin.logError(
						"Error opening Bundle local connection to "
								+ bundleUrlConn, ex);
				return false;
			}
		}

		// Get the JAR URL
		if (urlConn instanceof JarURLConnection) {
			pAnnotationLibraryPath = ((JarURLConnection) urlConn)
					.getJarFileURL().getPath();

			// Found !
			return true;
		}

		IPojoClasspathPlugin.logInfo("Not a JarURLConnection : "
				+ urlConn.getClass().getName());

		return false;
	}

	/**
	 * Retrieves the computed annotation library path
	 * 
	 * @return The computed path
	 */
	public String getAnnotationLibraryPath() {

		findAnnotationLibrary();
		return pAnnotationLibraryPath;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jdt.core.IClasspathContainer#getClasspathEntries()
	 */
	@Override
	public IClasspathEntry[] getClasspathEntries() {

		if (!findAnnotationLibrary()) {
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
}
