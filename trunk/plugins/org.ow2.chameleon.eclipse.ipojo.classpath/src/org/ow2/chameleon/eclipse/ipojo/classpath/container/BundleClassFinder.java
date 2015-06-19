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

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;

import org.ow2.chameleon.eclipse.ipojo.classpath.IPojoClasspathPlugin;

/**
 * Looks for the JAR file providing the given class. This class works in both
 * Eclipse Luna and previous releases. The way to work is inspired from
 * org.eclipse.jetty.osgi.boot.utils.internal.DefaultFileLocatorHelper.
 *
 * @author Thomas Calmant
 */
public class BundleClassFinder {

	/**
	 * The possible fully qualified names of the BundleURLConnection class, as
	 * it changed between Eclipse Kepler and Luna
	 */
	private static final String[] BUNDLE_URL_CONNECTION_CLASSES = {
			"org.eclipse.osgi.framework.internal.core.BundleURLConnection",
			"org.eclipse.osgi.storage.url.BundleURLConnection" };

	/** Keep track of the getLocalURL method from BundleURLConnection */
	private Method pGetLocalUrlMethod;

	/**
	 * Checks if the given string array contains the given string
	 *
	 * @param aArray
	 *            An array of strings
	 * @param aString
	 *            The string to look for
	 * @return True if the given string is found in the array
	 */
	protected boolean contains(final String[] aArray, final String aString) {

		if (aArray == null || aString == null) {
			return false;
		}

		for (final String arrayStr : aArray) {
			if (aString.equals(arrayStr)) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Tries to find the path to the bundle JAR file that provides the given
	 * class
	 *
	 * @param aClass
	 *            A class from a bundle
	 * @return The path to the bundle JAR file, or null
	 */
	public String getJarFile(final Class<?> aClass) {

		// Get the class file URL
		final URL classUrl = aClass.getResource(aClass.getSimpleName()
				+ ".class");

		if (classUrl == null) {
			// Class file not found
			IPojoClasspathPlugin.logError(
					"Can't find the annotation class URL", null);
			return null;
		}

		// Get the connection
		URLConnection urlConn;
		try {
			urlConn = classUrl.openConnection();

		} catch (final IOException ex) {
			IPojoClasspathPlugin.logError("Error opening JAR connection to "
					+ classUrl, ex);
			return null;
		}

		// Get the Bundle URL
		try {
			urlConn = getLocalURL(urlConn);

		} catch (final IOException ex) {
			IPojoClasspathPlugin.logError(
					"Error opening Bundle local connection to " + urlConn, ex);
		}

		// Get the JAR URL
		if (urlConn instanceof JarURLConnection) {
			// Found it !
			return ((JarURLConnection) urlConn).getJarFileURL().getPath();
		}

		IPojoClasspathPlugin.logInfo("Not a JarURLConnection : "
				+ urlConn.getClass().getName());

		return null;
	}

	/**
	 * Only useful for equinox: on felix we get the file:// or jar:// url
	 * already. Other OSGi implementations have not been tested <p> Get a URL to
	 * the bundle entry that uses a common protocol (i.e. file: jar: or http:
	 * etc.). </p>
	 *
	 * Method from
	 * org.eclipse.jetty.osgi.boot.utils.internal.DefaultFileLocatorHelper
	 *
	 * @return a URL to the bundle entry that uses a common protocol
	 * @throws IOException
	 */
	private URLConnection getLocalURL(final URLConnection urlConn)
			throws IOException {

		// Check class by name
		if (contains(BUNDLE_URL_CONNECTION_CLASSES, urlConn.getClass()
				.getName())) {

			// Find the method
			if (pGetLocalUrlMethod == null) {
				try {
					pGetLocalUrlMethod = urlConn.getClass().getMethod(
							"getLocalURL", (Class<?>[]) null);
					pGetLocalUrlMethod.setAccessible(true);

				} catch (final Exception ex) {
					IPojoClasspathPlugin.logError(
							"Can't find the getLocalURL() method", ex);
					return null;
				}
			}

			// Call it
			if (pGetLocalUrlMethod != null) {
				try {
					return ((URL) pGetLocalUrlMethod.invoke(urlConn,
							(Object[]) null)).openConnection();

				} catch (final IllegalArgumentException ex) {
					// Can't happen...

				} catch (final IllegalAccessException ex) {
					// Too bad...
					IPojoClasspathPlugin.logError(
							"Can't call the getLocalURL() method", ex);

				} catch (final InvocationTargetException ex) {
					// Error from the call
					IPojoClasspathPlugin.logError(
							"Error calling the getLocalURL() method",
							ex.getCause());
				}
			}
		}

		return urlConn;
	}
}
