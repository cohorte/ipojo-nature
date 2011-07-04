/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 * Copyright (C) 2010 isandlaTech.com, France
 */
package org.ow2.chameleon.eclipse.ipojo;

import org.eclipse.core.runtime.Status;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controlling the plug-in life cycle
 * 
 * @author Thomas Calmant
 */
public class Activator extends AbstractUIPlugin {

	/** The plug-in ID */
	public static final String PLUGIN_ID = "org.psem2m.eclipse.ipojo.manifest.updater"; //$NON-NLS-1$

	/** The shared instance */
	private static Activator pPluginInstance;

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
	 * Returns the shared instance
	 * 
	 * @return the shared instance
	 */
	public static Activator getUIPluginInstance() {
		return pPluginInstance;
	}

	/**
	 * Logs an exception
	 * 
	 * @param msg
	 *            Context description
	 * @param ex
	 *            Exception caught
	 */
	public static void logError(final String msg, final Exception ex) {
		getUIPluginInstance().getLog().log(
				new Status(Status.ERROR, PLUGIN_ID, msg, ex));
	}

	/**
	 * Logs an information
	 * 
	 * @param msg
	 *            Message to log
	 */
	public static void logInfo(final String msg) {
		getUIPluginInstance().getLog().log(
				new Status(Status.INFO, PLUGIN_ID, msg));
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
