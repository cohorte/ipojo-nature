/**
 * 
 */
package org.ow2.chameleon.eclipse.ipojo.core;

import java.util.LinkedList;
import java.util.List;

import org.apache.felix.ipojo.manipulator.Reporter;
import org.eclipse.core.resources.IProject;
import org.ow2.chameleon.eclipse.ipojo.Activator;

/**
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
	 * @see org.apache.felix.ipojo.manipulator.Reporter#error(java.lang.String)
	 */
	@Override
	public void error(final String aMessage) {

		pErrorsList.add(aMessage);
		Activator.logError(pProject, aMessage, null);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.felix.ipojo.manipulator.Reporter#getErrors()
	 */
	@SuppressWarnings("rawtypes")
	@Override
	public List getErrors() {
		return pErrorsList;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.felix.ipojo.manipulator.Reporter#getWarnings()
	 */
	@SuppressWarnings("rawtypes")
	@Override
	public List getWarnings() {
		return pWarningsList;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.felix.ipojo.manipulator.Reporter#warn(java.lang.String)
	 */
	@Override
	public void warn(final String aMessage) {

		pWarningsList.add(aMessage);
		Activator.logWarning(pProject, aMessage, null);
	}
}
