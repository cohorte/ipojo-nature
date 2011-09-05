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
package org.ow2.chameleon.eclipse.ipojo.exporter;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 * 
 * @author Thomas Calmant
 */
public class IPojoExporterPlugin extends AbstractUIPlugin {

	/** The plug-in ID **/
	public static final String PLUGIN_ID = "org.ow2.chameleon.eclipse.ipojo.exporter";

	/** The shared instance */
	private static IPojoExporterPlugin sPlugin;

	/**
	 * Returns the shared instance
	 * 
	 * @return the shared instance
	 */
	public static IPojoExporterPlugin getDefault() {
		return sPlugin;
	}

	/**
	 * Logs an exception
	 * 
	 * @param aMessage
	 *            Context description
	 * @param aThrowable
	 *            Exception caught
	 */
	public static void logError(final String aMessage,
			final Throwable aThrowable) {

		sPlugin.getLog().log(
				new Status(IStatus.ERROR, PLUGIN_ID, aMessage, aThrowable));
	}

	/**
	 * Logs an information
	 * 
	 * @param aProject
	 *            Current manipulated project
	 * @param aMessage
	 *            Message to log
	 */
	public static void logInfo(final String aMessage) {

		sPlugin.getLog().log(new Status(IStatus.INFO, PLUGIN_ID, aMessage));
	}

	/**
	 * Logs an ignored exception
	 * 
	 * @param aMessage
	 *            Context description
	 * @param aThrowable
	 *            Exception to log
	 */
	public static void logWarning(final String aMessage,
			final Throwable aThrowable) {

		sPlugin.getLog().log(
				new Status(IStatus.WARNING, PLUGIN_ID, aMessage, aThrowable));
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
		sPlugin = this;
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
		sPlugin = null;
		super.stop(context);
	}
}
