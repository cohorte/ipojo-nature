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
package org.ow2.chameleon.eclipse.ipojo.core;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.apache.felix.ipojo.manipulator.Reporter;
import org.eclipse.core.resources.IProject;
import org.ow2.chameleon.eclipse.ipojo.Activator;

/**
 * The error reporter used by the manipulator
 * 
 * @author Thomas Calmant
 */
public class EclipseReporter implements Reporter {

	/** Errors list */
	private final List<String> pErrorsList = new LinkedList<String>();

	/** Manipulated project */
	private final IProject pProject;

	/** Warning list */
	private final List<String> pWarningsList = new LinkedList<String>();

	/**
	 * Sets up the reporter
	 * 
	 * @param aProject
	 *            Manipulated project
	 */
	public EclipseReporter(final IProject aProject) {
		pProject = aProject;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.felix.ipojo.manipulator.Reporter#error(java.lang.String,
	 * java.lang.Object[])
	 */
	@Override
	public void error(final String aMessage, final Object... aFormatArgs) {

		final Throwable throwable = getThrowable(aFormatArgs);
		final String formattedMessage = String.format(aMessage,
				getMessageArguments(aFormatArgs));

		pErrorsList.add(formattedMessage);
		Activator.logError(pProject, formattedMessage, throwable);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.felix.ipojo.manipulator.Reporter#getErrors()
	 */
	@Override
	public List<String> getErrors() {
		return pErrorsList;
	}

	/**
	 * Retrieves the message format arguments, removing the last object of the
	 * array if it's a Throwable.
	 * 
	 * @param aFormatArgs
	 *            Message arguments
	 * @return The given array without the last argument if it was a Throwable.
	 */
	protected Object[] getMessageArguments(final Object... aFormatArgs) {

		if (aFormatArgs != null && aFormatArgs.length > 0) {

			if (aFormatArgs[aFormatArgs.length - 1] instanceof Throwable) {
				// Source throwable found
				return Arrays.copyOf(aFormatArgs, aFormatArgs.length - 1);
			}
		}

		return aFormatArgs;

	}

	/**
	 * Retrieves the throwable associated to the arguments, null if not found
	 * 
	 * @param aFormatArgs
	 *            Message arguments
	 * @return The found throwable, null if not found
	 */
	protected Throwable getThrowable(final Object... aFormatArgs) {

		if (aFormatArgs != null && aFormatArgs.length > 0) {

			if (aFormatArgs[aFormatArgs.length - 1] instanceof Throwable) {
				return (Throwable) aFormatArgs[aFormatArgs.length - 1];
			}
		}

		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.felix.ipojo.manipulator.Reporter#getWarnings()
	 */
	@Override
	public List<String> getWarnings() {
		return pWarningsList;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.felix.ipojo.manipulator.Reporter#info(java.lang.String,
	 * java.lang.Object[])
	 */
	@Override
	public void info(final String aMessage, final Object... aFormatArgs) {

		final String formattedMessage = String.format(aMessage,
				getMessageArguments(aFormatArgs));

		pErrorsList.add(formattedMessage);
		Activator.logInfo(pProject, formattedMessage);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.felix.ipojo.manipulator.Reporter#trace(java.lang.String,
	 * java.lang.Object[])
	 */
	@Override
	public void trace(final String aMessage, final Object... aFormatArgs) {
		// There is no TRACE level in Eclipse, consider it as INFO
		info(aMessage, aFormatArgs);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.felix.ipojo.manipulator.Reporter#warn(java.lang.String,
	 * java.lang.Object[])
	 */
	@Override
	public void warn(final String aMessage, final Object... aFormatArgs) {

		final Throwable throwable = getThrowable(aFormatArgs);
		final String formattedMessage = String.format(aMessage,
				getMessageArguments(aFormatArgs));

		pErrorsList.add(formattedMessage);
		Activator.logWarning(pProject, formattedMessage, throwable);
	}
}