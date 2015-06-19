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

import java.io.File;
import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.launching.JavaRuntime;
import org.ow2.chameleon.eclipse.ipojo.Activator;

/**
 * Utility object to resolve the classpath entries of a Java project, ignoring
 * the JRE information
 *
 * @author Thomas Calmant
 */
public class ClasspathResolver {

	/**
	 * Finds the project matching the given entry path
	 *
	 * @param aRoot
	 *            Workspace root
	 * @param aPath
	 *            Workspace-relative path
	 * @return The Java project object or null
	 */
	public IJavaProject findProject(final IWorkspaceRoot aRoot,
			final IPath aPath) {

		final IResource resource = aRoot.findMember(aPath);
		if (resource instanceof IProject) {
			// The path was leading to a project
			return Utilities.INSTANCE.getJavaProject((IProject) resource);

		} else if (resource != null) {
			// The path was leading to a folder inside a project
			final IProject project = resource.getProject();
			if (project != null) {
				return Utilities.INSTANCE.getJavaProject(project);
			}
		}

		// Not a project
		return null;
	}

	/**
	 * Returns a set of file-system paths that corresponds the classpath of the
	 * given project.
	 *
	 * @param aJavaProject
	 *            A JDT Java project
	 * @return A set of paths
	 * @throws JavaModelException
	 *             An error occurred reading the project classpath
	 */
	public Set<String> getClasspath(final IJavaProject aJavaProject)
			throws JavaModelException {

		return resolveEntries(aJavaProject.getRawClasspath(), aJavaProject,
				aJavaProject.getProject().getWorkspace().getRoot());
	}

	/**
	 * Resolves the paths of the given container entry
	 *
	 * @param aEntry
	 *            A classpath entry of {@link IClasspathEntry#CPE_CONTAINER}
	 *            type
	 * @param aJavaProject
	 *            The resolved JDT Java project
	 * @param aRoot
	 *            The workspace root
	 * @return The paths of the libraries and projects references in the
	 *         container
	 * @throws JavaModelException
	 *             Error reading a project classpath
	 */
	protected Set<String> resolveContainer(final IClasspathEntry aEntry,
			final IJavaProject aJavaProject, final IWorkspaceRoot aRoot)
			throws JavaModelException {

		final Set<String> results = new LinkedHashSet<String>();
		if (!JavaRuntime.JRE_CONTAINER.equals(aEntry.getPath().segment(0))) {
			// Ignore the JRE Container
			final IClasspathContainer classpathContainer = JavaCore
					.getClasspathContainer(aEntry.getPath(), aJavaProject);

			// Recursively resolve container entries
			results.addAll(resolveEntries(
					classpathContainer.getClasspathEntries(), aJavaProject,
					aRoot));
		}

		return results;
	}

	/**
	 * Resolves the path of the given classpath entries
	 *
	 * @param aClasspathEntries
	 *            An array of classpath entries to resolve
	 * @param aJavaProject
	 *            The resolved JDT Java project
	 * @param aRoot
	 *            The workspace root
	 * @return A set of paths
	 * @throws JavaModelException
	 *             Error reading a classpath entry
	 */
	protected Set<String> resolveEntries(
			final IClasspathEntry[] aClasspathEntries,
			final IJavaProject aJavaProject, final IWorkspaceRoot aRoot)
			throws JavaModelException {

		final Set<String> results = new LinkedHashSet<String>();
		for (final IClasspathEntry entry : aClasspathEntries) {

			switch (entry.getEntryKind()) {
			case IClasspathEntry.CPE_CONTAINER:
				// Set of libraries and projects
				results.addAll(resolveContainer(entry, aJavaProject, aRoot));
				break;

			case IClasspathEntry.CPE_LIBRARY:
				// JAR file
				results.add(resolveLibrary(entry, aRoot));
				break;

			case IClasspathEntry.CPE_PROJECT:
			case IClasspathEntry.CPE_SOURCE:
				// Referenced project or source folder
				results.add(resolveProject(entry, aRoot));
				break;

			case IClasspathEntry.CPE_VARIABLE:
				// Workspace variable
				results.addAll(resolveVariable(entry, aJavaProject, aRoot));
				break;

			default:
				// Future cases...
				Activator.logWarning(aJavaProject.getProject(),
						"Unhandled kind of classpath entry: " + entry);
				break;
			}
		}

		// Remove null from values (avoids checks in the for-loop)
		results.remove(null);

		return results;
	}

	/**
	 * Resolves the path to the given library
	 *
	 * @param aEntry
	 *            A classpath entry of {@link IClasspathEntry#CPE_LIBRARY} type
	 * @param aRoot
	 *            The workspace root
	 * @return The path to the given library
	 */
	protected String resolveLibrary(final IClasspathEntry aEntry,
			final IWorkspaceRoot aRoot) {

		// Get workspace-based location
		final IResource libResource = aRoot.findMember(aEntry.getPath());
		if (libResource == null) {
			// Not a local resource
			final File classpathFile = aEntry.getPath().toFile();
			if (classpathFile.exists()) {
				return classpathFile.getAbsolutePath();
			}

		} else {
			// Local resource
			return libResource.getLocation().toString();
		}

		// Unknown file
		return null;
	}

	/**
	 * Resolves the path to the output location of the given project
	 *
	 * @param aEntry
	 *            A classpath entry of {@link IClasspathEntry#CPE_PROJECT} or
	 *            {@link IClasspathEntry#CPE_SOURCE} types
	 * @param aRoot
	 *            The workspace root
	 * @return The path to the output location of the project
	 * @throws JavaModelException
	 *             Error reading Java project description
	 */
	protected String resolveProject(final IClasspathEntry aEntry,
			final IWorkspaceRoot aRoot) throws JavaModelException {

		// Refers to another project
		final IJavaProject javaProject = findProject(aRoot, aEntry.getPath());
		if (javaProject != null) {
			return aRoot.getFile(javaProject.getOutputLocation())
					.getRawLocation().toOSString();
		}

		return null;
	}

	/**
	 * Resolves the path of the given variable classpath entry
	 *
	 * @param aEntry
	 *            A classpath entry of {@link IClasspathEntry#CPE_VARIABLE} type
	 * @param aJavaProject
	 *            The resolved Java project
	 * @param aRoot
	 *            The workspace root
	 * @return The resolved path of the classpath entry or null
	 * @throws JavaModelException
	 *             Error recursively resolving the classpath entry
	 */
	protected Set<String> resolveVariable(final IClasspathEntry aEntry,
			final IJavaProject aJavaProject, final IWorkspaceRoot aRoot)
			throws JavaModelException {

		// Try to resolve the entry
		final IClasspathEntry resolvedEntry = JavaCore
				.getResolvedClasspathEntry(aEntry);
		if (resolvedEntry == null) {
			// Can't resolve it
			Activator.logWarning(aJavaProject.getProject(),
					"Can't resolve variable classpath entry: " + aEntry);
			return new LinkedHashSet<String>();
		}

		// Work with it
		return resolveEntries(new IClasspathEntry[] { resolvedEntry },
				aJavaProject, aRoot);
	}
}
