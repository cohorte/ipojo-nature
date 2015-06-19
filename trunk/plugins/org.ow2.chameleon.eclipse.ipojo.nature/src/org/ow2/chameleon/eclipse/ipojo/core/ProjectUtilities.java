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
package org.ow2.chameleon.eclipse.ipojo.core;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IAccessRule;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.ow2.chameleon.eclipse.ipojo.Activator;
import org.ow2.chameleon.eclipse.ipojo.IClasspathConstants;
import org.ow2.chameleon.eclipse.ipojo.builder.IPojoNature;

/**
 * Utility methods for project updates
 * 
 * @author Thomas Calmant
 */
public class ProjectUtilities {

	/**
	 * Creates a copy of the given entry with a new access rule
	 * 
	 * @param aEntry
	 *            A class path entry (container)
	 * @param aNewAccessRule
	 *            A new access rule
	 * @return The new container entry
	 */
	private IClasspathEntry addAccessRule(final IClasspathEntry aEntry,
			final IAccessRule aNewAccessRule) {

		// Update access rules array
		final IAccessRule[] currentRules = aEntry.getAccessRules();
		final IAccessRule[] newRules = new IAccessRule[currentRules.length + 1];

		System.arraycopy(currentRules, 0, newRules, 0, currentRules.length);
		newRules[newRules.length - 1] = aNewAccessRule;

		// Create a new container entry
		return JavaCore.newContainerEntry(aEntry.getPath(), newRules,
				aEntry.getExtraAttributes(), aEntry.isExported());
	}

	/**
	 * Adds the annotations class path container to the project build path
	 * 
	 * @param aProject
	 *            Project to be modified
	 */
	public void addAnnotations(final IProject aProject) {

		// Get the Java nature
		final IJavaProject javaProject;
		try {
			javaProject = (IJavaProject) aProject.getNature(JavaCore.NATURE_ID);

		} catch (final CoreException e) {
			Activator.logError(aProject, "Can't get the Java nature", e);
			return;
		}

		// Get current entries
		final IClasspathEntry[] currentEntries;
		try {
			currentEntries = javaProject.getRawClasspath();

		} catch (final JavaModelException e) {
			Activator.logError(aProject, "Error reading project classpath", e);
			return;
		}

		// Flag to indicate if the annotations container must be added
		boolean addAnnotations = true;

		// ID of the modified class path entry, if any
		int newPdeEntryIdx = -1;
		IClasspathEntry newPdeEntry = null;

		// Loop on project class path entries
		int idx = 0;
		for (final IClasspathEntry entry : currentEntries) {

			if (entry.getEntryKind() == IClasspathEntry.CPE_CONTAINER) {
				if (IClasspathConstants.ANNOTATIONS_CONTAINER_PATH.equals(entry
						.getPath())) {
					// The annotation container is already here.
					addAnnotations = false;

				} else if (IClasspathConstants.PDE_CONTAINER_PATH.equals(entry
						.getPath())) {
					// Found the PDE container

					// Flag to activate the access rule creation
					boolean needsRule = true;

					// Check if the access rule has already been set
					for (final IAccessRule accessRule : entry.getAccessRules()) {
						if (IClasspathConstants.ANNOTATIONS_ACCESS_PATTERN
								.equals(accessRule.getPattern())) {
							// iPOJO annotations access pattern already set
							needsRule = false;
							break;
						}
					}

					if (needsRule) {
						// Replace the container by a new one
						newPdeEntryIdx = idx;
						newPdeEntry = addAccessRule(
								entry,
								JavaCore.newAccessRule(
										IClasspathConstants.ANNOTATIONS_ACCESS_PATTERN,
										IAccessRule.K_ACCESSIBLE));
					}
				}
			}

			idx++;
		}

		if (!addAnnotations && newPdeEntry == null) {
			// Nothing to do
			return;
		}

		// Set up the new class path array
		final IClasspathEntry[] newEntries;

		if (addAnnotations) {
			newEntries = new IClasspathEntry[currentEntries.length + 1];

			// Add the new annotations at the tail
			newEntries[currentEntries.length] = JavaCore
					.newContainerEntry(IClasspathConstants.ANNOTATIONS_CONTAINER_PATH);

		} else {
			newEntries = new IClasspathEntry[currentEntries.length];
		}

		// Copy previous value
		System.arraycopy(currentEntries, 0, newEntries, 0,
				currentEntries.length);

		// Replace the PDE entry, if any
		if (newPdeEntry != null) {
			newEntries[newPdeEntryIdx] = newPdeEntry;
		}

		// Set the project class path
		try {
			javaProject.setRawClasspath(newEntries, null);

		} catch (final JavaModelException e) {
			Activator.logError(aProject,
					"Error setting up the new project class path", e);
		}
	}

	/**
	 * Adds the IPOJO Nature to the given project
	 * 
	 * @param aProject
	 *            A Java project
	 * @throws CoreException
	 *             Something went wrong
	 */
	public void addNature(final IProject aProject) throws CoreException {

		final IPojoNature nature = new IPojoNature();
		nature.setProject(aProject);
		nature.configure();
	}

	/**
	 * Creates a copy of the given entry without the given access rule
	 * 
	 * @param aEntry
	 *            A class path entry (container)
	 * @param aPath
	 *            An access rule path
	 * @return The new container entry
	 */
	private IClasspathEntry removeAccessRule(final IClasspathEntry aEntry,
			final IPath aPath) {

		// Update the access rules array
		final IAccessRule[] currentRules = aEntry.getAccessRules();
		final List<IAccessRule> newRules = new ArrayList<IAccessRule>(
				currentRules.length);

		// Filter access rules
		for (final IAccessRule rule : currentRules) {
			if (!aPath.equals(rule.getPattern())) {
				newRules.add(rule);
			}
		}

		// Convert the list to an array
		final IAccessRule[] newRulesArray = newRules
				.toArray(new IAccessRule[newRules.size()]);

		// Create a new container entry
		return JavaCore.newContainerEntry(aEntry.getPath(), newRulesArray,
				aEntry.getExtraAttributes(), aEntry.isExported());
	}

	/**
	 * Removes the annotations container from the project build path (opposite
	 * of {@link #addAnnotationsLibrary(IProject)}
	 * 
	 * @param aProject
	 *            A java project
	 */
	public void removeAnnotations(final IProject aProject) {

		// Get the Java nature
		final IJavaProject javaProject;
		try {
			javaProject = (IJavaProject) aProject.getNature(JavaCore.NATURE_ID);

		} catch (final CoreException e) {
			Activator.logError(aProject, "Can't get the Java nature", e);
			return;
		}

		// Get current entries
		final IClasspathEntry[] currentEntries;
		try {
			currentEntries = javaProject.getRawClasspath();

		} catch (final JavaModelException e) {
			Activator.logError(aProject, "Error reading project classpath", e);
			return;
		}

		// Flag to indicate if the annotations container must be added
		boolean removeAnnotations = false;
		int annotationsEntryIdx = -1;

		// ID of the modified class path entry, if any
		int newPdeEntryIdx = -1;
		IClasspathEntry newPdeEntry = null;

		// Loop on project class path entries
		int idx = 0;
		for (final IClasspathEntry entry : currentEntries) {

			if (entry.getEntryKind() == IClasspathEntry.CPE_CONTAINER) {
				if (IClasspathConstants.ANNOTATIONS_CONTAINER_PATH.equals(entry
						.getPath())) {
					// The annotation container is here.
					removeAnnotations = true;
					annotationsEntryIdx = idx;

				} else if (IClasspathConstants.PDE_CONTAINER_PATH.equals(entry
						.getPath())) {
					// Found the PDE container

					// Flag to activate the access rule creation
					boolean removeRule = false;

					// Check if the access rule has already been set
					for (final IAccessRule accessRule : entry.getAccessRules()) {
						if (IClasspathConstants.ANNOTATIONS_ACCESS_PATTERN
								.equals(accessRule.getPattern())) {
							// iPOJO annotations access pattern is set
							removeRule = true;
							System.out.println("Found access rule");
							break;
						}
					}

					if (removeRule) {
						// Replace the container by a new one
						newPdeEntryIdx = idx;
						newPdeEntry = removeAccessRule(entry,
								IClasspathConstants.ANNOTATIONS_ACCESS_PATTERN);
					}
				}
			}

			idx++;
		}

		if (!removeAnnotations && newPdeEntry == null) {
			// Nothing to do
			return;
		}

		// Set up the new class path array
		final IClasspathEntry[] newEntries;

		if (removeAnnotations) {
			// Copy the array, ignoring the annotations container
			newEntries = new IClasspathEntry[currentEntries.length - 1];
			for (int i = 0, j = 0; i < currentEntries.length - 1; i++) {
				if (i != annotationsEntryIdx) {
					newEntries[j++] = currentEntries[i];
				}
			}

		} else {
			// Copy previous value
			newEntries = new IClasspathEntry[currentEntries.length];
			System.arraycopy(currentEntries, 0, newEntries, 0,
					currentEntries.length);
		}

		// Replace the PDE entry, if any
		if (newPdeEntry != null) {

			if (newPdeEntryIdx > annotationsEntryIdx) {
				// We removed the annotation entry...
				newPdeEntryIdx--;
			}

			newEntries[newPdeEntryIdx] = newPdeEntry;
		}

		// Set the project class path
		try {
			javaProject.setRawClasspath(newEntries, null);

		} catch (final JavaModelException e) {
			Activator.logError(aProject,
					"Error setting up the new project class path", e);
		}
	}

	/**
	 * Removes the IPOJO Nature from the given project
	 * 
	 * @param aProject
	 *            A Java project
	 * @throws CoreException
	 *             Something went wrong
	 */
	public void removeNature(final IProject aProject) throws CoreException {

		final IPojoNature nature = new IPojoNature();
		nature.setProject(aProject);
		nature.deconfigure();
	}
}
