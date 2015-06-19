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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.Enumeration;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IExportWizard;
import org.eclipse.ui.IWorkbench;
import org.osgi.framework.Bundle;
import org.ow2.chameleon.eclipse.ipojo.Activator;

/**
 * iPOJO core Jar file export wizard
 * 
 * @author Thomas Calmant
 */
public class IPojoCoreJarExportWizard extends Wizard implements IExportWizard {

	/** The dependencies plug-in ID */
	private static final String DEPENDENCIES_PLUGIN_ID = "org.ow2.chameleon.eclipse.ipojo.dependencies";

	/** Pattern of the iPOJO Core Jar file name */
	private static final String IPOJO_CORE_FILE_PATTERN = "org.apache.felix.ipojo-*.jar";

	/** Export configuration page */
	private CoreFileExportPage pExportPage;

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
	 * Copies the content of the readable channel into the writable one, using a
	 * 16Kb buffer.
	 * 
	 * Code from
	 * http://thomaswabner.wordpress.com/2007/10/09/fast-stream-copy-using
	 * -javanio-channels/
	 * 
	 * @param src
	 *            Readable channel
	 * @param dest
	 *            Writable channel
	 * @throws IOException
	 *             Something went wrong
	 */
	private void fastChannelCopy(final ReadableByteChannel src,
			final WritableByteChannel dest) throws IOException {

		final ByteBuffer buffer = ByteBuffer.allocateDirect(16 * 1024);

		while (src.read(buffer) != -1) {
			// prepare the buffer to be drained
			buffer.flip();

			// write to the channel, may block
			dest.write(buffer);

			// If partial transfer, shift remainder down
			// If buffer is empty, same as doing clear()
			buffer.compact();
		}

		// EOF will leave buffer in fill state
		buffer.flip();

		// make sure the buffer is fully drained.
		while (buffer.hasRemaining()) {
			dest.write(buffer);
		}
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
		setWindowTitle("iPOJO Core Jar File export wizard");

		// Prepare the export page
		pExportPage = new CoreFileExportPage("iPOJO Core Jar File Export");
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

		// Get the bundle containing the JAR
		final Bundle dependenciesBundle = Platform
				.getBundle(DEPENDENCIES_PLUGIN_ID);

		// Find the JAR file name
		final Enumeration<URL> entries = dependenciesBundle.findEntries("/",
				IPOJO_CORE_FILE_PATTERN, false);
		if (!entries.hasMoreElements()) {
			// File not found
			Activator.logError(null, "iPOJO Core JAR file not found", null);
			return false;
		}

		// Get the file access & name
		final URL coreFileURL = entries.nextElement();
		final String[] nameParts = coreFileURL.getPath().split("/");
		final String coreFileName = nameParts[nameParts.length - 1];

		// Compute the export file name
		final String outputFolder = pExportPage.getOutputFolder();
		final File outputFile = new File(outputFolder, coreFileName);
		if (outputFile.exists()) {
			Activator.logWarning(null, "Overwritting file: " + outputFile);

		} else {
			// Create the file
			try {
				outputFile.createNewFile();

			} catch (final IOException ex) {
				Activator.showError(null, "Error creating iPOJO JAR file: "
						+ outputFile, ex);
				return false;
			}
		}

		// Export the file
		ReadableByteChannel inChannel = null;
		WritableByteChannel outChannel = null;
		try {
			// Open the NIO channels
			inChannel = Channels.newChannel(coreFileURL.openStream());
			outChannel = Channels.newChannel(new FileOutputStream(outputFile));

			// Copy
			fastChannelCopy(inChannel, outChannel);

		} catch (final IOException ex) {
			// Error copying file
			Activator.showError(null, "Error exporting iPOJO JAR file: "
					+ outputFile, ex);
			return false;

		} finally {
			// Close channels
			if (inChannel != null) {
				try {
					inChannel.close();
				} catch (final IOException ex) {
					// Ignore
				}
			}

			if (outChannel != null) {
				try {
					outChannel.close();
				} catch (final IOException ex) {
					// Ignore
				}
			}
		}

		// No error
		Activator.logInfo(null, "iPOJO Core JAR file exported to: "
				+ outputFile);
		return true;
	}
}
