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
package org.ow2.chameleon.eclipse.ipojo;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.ui.statushandlers.StatusManager;
import org.osgi.framework.BundleContext;

/**
 * The activator class controlling the plug-in life cycle
 * 
 * @author Thomas Calmant
 */
public class Activator extends AbstractUIPlugin {

	/** Log info messages */
	public static final String LOG_INFO = Activator.PLUGIN_ID + "/log/info";

	/** Log trace messages */
	public static final String LOG_TRACES = Activator.PLUGIN_ID + "/log/traces";

	/** The plug-in ID */
	public static final String PLUGIN_ID = "org.ow2.chameleon.eclipse.ipojo.nature"; //$NON-NLS-1$

	/** The shared instance */
	private static Activator pPluginInstance;

	/**
	 * Checks if information messages must be logged
	 * 
	 * @return True if information messages are activated
	 * @see <a
	 *      href="http://wiki.eclipse.org/FAQ_How_do_I_use_the_platform_debug_tracing_facility%3F">How
	 *      do I use the platform debug tracing facility?</a>
	 */
	public static boolean areInfoOn() {

		return "true".equalsIgnoreCase(Platform.getDebugOption(LOG_INFO));
	}

	/**
	 * Checks if trace messages must be logged
	 * 
	 * @return True if trace messages are activated
	 * @see <a
	 *      href="http://wiki.eclipse.org/FAQ_How_do_I_use_the_platform_debug_tracing_facility%3F">How
	 *      do I use the platform debug tracing facility?</a>
	 */
	public static boolean areTracesOn() {

		return "true".equalsIgnoreCase(Platform.getDebugOption(LOG_TRACES));
	}

	/**
	 * Returns an image descriptor for the image file at the given plug-in
	 * relative path
	 * 
	 * @param path
	 *            the path
	 * @return the image descriptor
	 */
	public static ImageDescriptor getImageDescriptor(final String path) {

		return imageDescriptorFromPlugin(PLUGIN_ID, path);
	}

	/**
	 * Return the log prefix ("projectName :") or an empty string aProject is
	 * null
	 * 
	 * @param aProject
	 *            Current manipulated project
	 * @return The log prefix
	 */
	protected static String getLogPrefix(final IProject aProject) {

		if (aProject != null) {
			return aProject.getName() + ": ";
		}

		return "";
	}

	/**
	 * Returns the shared instance
	 * 
	 * @return the shared instance
	 */
	public static Activator getPluginInstance() {

		return pPluginInstance;
	}

	/**
	 * Logs an exception
	 * 
	 * @param aProject
	 *            Current manipulated project
	 * @param aMessage
	 *            Context description
	 * @param aThrowable
	 *            Exception caught
	 */
	public static void logError(final IProject aProject, final String aMessage,
			final Throwable aThrowable) {

		StatusManager.getManager().handle(
				new Status(IStatus.ERROR, PLUGIN_ID, getLogPrefix(aProject)
						+ aMessage, aThrowable));
	}

	/**
	 * Logs an information
	 * 
	 * @param aProject
	 *            Current manipulated project
	 * @param aMessage
	 *            Message to log
	 */
	public static void logInfo(final IProject aProject, final String aMessage) {

		if (areInfoOn()) {
			StatusManager.getManager().handle(
					new Status(IStatus.INFO, PLUGIN_ID, getLogPrefix(aProject)
							+ aMessage));
		}
	}

	/**
	 * Logs a trace
	 * 
	 * @param aProject
	 *            Current manipulated project
	 * @param aMessage
	 *            Message to log
	 */
	public static void logTrace(final IProject aProject, final String aMessage) {

		if (areTracesOn()) {
			StatusManager.getManager().handle(
					new Status(IStatus.INFO, PLUGIN_ID, getLogPrefix(aProject)
							+ aMessage));
		}
	}

	/**
	 * Logs a warning
	 * 
	 * @param aProject
	 *            Current manipulated project
	 * @param aMessage
	 *            Context description
	 */
	public static void logWarning(final IProject aProject, final String aMessage) {

		logWarning(aProject, aMessage, null);
	}

	/**
	 * Logs an handled exception
	 * 
	 * @param aProject
	 *            Current manipulated project
	 * @param aMessage
	 *            Context description
	 * @param aThrowable
	 *            Exception to log
	 */
	public static void logWarning(final IProject aProject,
			final String aMessage, final Throwable aThrowable) {

		StatusManager.getManager().handle(
				new Status(IStatus.WARNING, PLUGIN_ID, getLogPrefix(aProject)
						+ aMessage, aThrowable));
	}

	/**
	 * Shows an error in the UI (doesn't log it)
	 * 
	 * @param aProject
	 *            Erroneous project (can be null)
	 * @param aMessage
	 *            Error message
	 * @param aThrowable
	 *            Exception to show (can be null)
	 */
	public static void showError(final IProject aProject,
			final String aMessage, final Throwable aThrowable) {

		StatusManager.getManager().handle(
				new Status(IStatus.ERROR, PLUGIN_ID, getLogPrefix(aProject)
						+ aMessage, aThrowable), StatusManager.SHOW);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext
	 * )
	 */
	@Override
	public void start(final BundleContext context) throws Exception {

		super.start(context);
		pPluginInstance = this;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext
	 * )
	 */
	@Override
	public void stop(final BundleContext context) throws Exception {

		pPluginInstance = null;
		super.stop(context);
	}
}
