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
package org.ow2.chameleon.eclipse.ipojo.builder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.ow2.chameleon.eclipse.ipojo.Activator;
import org.ow2.chameleon.eclipse.ipojo.core.ManifestUpdater;

/**
 * iPOJO Manifest updater for iPOJO nature projects
 * 
 * @author Thomas Calmant
 */
public class IPojoBuilder extends IncrementalProjectBuilder {

	/** Plugin Builder ID */
	public static final String BUILDER_ID = "org.ow2.chameleon.eclipse.ipojo.ipojoBuilder";

	/** Last build session property */
	public static final QualifiedName PROJECT_LAST_BUILD = new QualifiedName(
			null, BUILDER_ID + ".lastBuild");

	/** iPOJO Manifest updater */
	private final ManifestUpdater pManifestUpdater = new ManifestUpdater();

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.internal.events.InternalBuilder#build(int,
	 * java.util.Map, org.eclipse.core.runtime.IProgressMonitor)
	 */
	@SuppressWarnings("rawtypes")
	@Override
	protected IProject[] build(final int kind, final Map args,
			final IProgressMonitor monitor) throws CoreException {

		switch (kind) {
		case FULL_BUILD:
			updateManifest();
			break;

		case AUTO_BUILD:
		case INCREMENTAL_BUILD:
			ArrayList<IResource> resources = new ArrayList<IResource>();
			ArrayList<IResource> classes = new ArrayList<IResource>();
			List<IResource> deltas = new ArrayList<IResource>();

			IResourceDelta resourceDelta = getDelta(getProject());

			boolean needCompleteBuild = false;
			if (resourceDelta != null) {

				needCompleteBuild = loadResourceDelta(resourceDelta, resources,
						classes);
			}

			if (needCompleteBuild || resourceDelta == null) {
				// Full build or metadata file modified : list all .class files
				getAllClassFiles(getProjectOutputContainer(), deltas);

			} else {
				// Filter lists to get only needed binaries
				deltas = filterLists(resources, classes);

			}

			// Do the work if needed
			if (needCompleteBuild) {
				updateManifest();
			}

			break;
		}

		// Null, IProject, keep a list of handled projects ???
		return new IProject[0];
	}

	/**
	 * Tests if the resource has been modified since the given time
	 * 
	 * @param aJavaClass
	 *            A Java .class file
	 * @param aLastBuildTimestamp
	 *            A build time stamp
	 * @return True if the file has been modified
	 */
	protected boolean classFileChanged(final IResource aJavaClass,
			final long aLastBuildTimestamp) {

		if (aJavaClass.getModificationStamp() > aLastBuildTimestamp) {
			System.out.println("Modified behind my back");
			return true;
		}

		return false;
	}

	/**
	 * Filters classes : returns only classes corresponding to modified sources
	 * 
	 * @param aJavaSourceList
	 *            Source files list
	 * @param aJavaClassList
	 *            Class files list
	 * @return Class files to update
	 */
	protected ArrayList<IResource> filterLists(
			final ArrayList<IResource> aJavaSourceList,
			final ArrayList<IResource> aJavaClassList) {

		ArrayList<IResource> selectedClasses = new ArrayList<IResource>();

		// Last build time
		long lastBuildTimestamp = 0;
		try {
			Object propertyValue = getProject().getSessionProperty(
					PROJECT_LAST_BUILD);
			if (propertyValue != null) {
				lastBuildTimestamp = (Long) propertyValue;
			}

		} catch (CoreException e) {
			// First build ?
			lastBuildTimestamp = 0;
		}

		for (IResource javaClass : aJavaClassList) {

			if (classFileChanged(javaClass, lastBuildTimestamp)) {
				selectedClasses.add(javaClass);
			} else if (sourceChanged(javaClass, aJavaSourceList)) {
				selectedClasses.add(javaClass);
			}
		}

		return selectedClasses;
	}

	/**
	 * Retrieves all .class files from the output path
	 * 
	 * @return The .class files list
	 */
	protected void getAllClassFiles(final IContainer aContainer,
			final List<IResource> aClassFileList) {

		if (aContainer == null) {
			return;
		}

		IResource[] members;

		try {
			members = aContainer.members();
		} catch (CoreException e) {
			Activator.logError(aContainer.getProject(),
					"Error listing members in : "
							+ aContainer.getFullPath().toOSString(), e);
			e.printStackTrace();
			return;
		}

		for (IResource member : members) {
			if (member.getType() == IResource.FOLDER) {
				getAllClassFiles((IContainer) member, aClassFileList);

			} else if (member.getType() == IResource.FILE
					&& member.getName().endsWith(".class")) {
				aClassFileList.add(member);
			}
		}
	}

	/**
	 * @return The Java project output container
	 * @throws CoreException
	 *             An error occurred while retrieving project informations
	 */
	protected IContainer getProjectOutputContainer() throws CoreException {
		return (IContainer) getProject().getWorkspace().getRoot()
				.findMember(getProjectOutputPath());
	}

	/**
	 * @return The Java project output path
	 * @throws CoreException
	 *             An error occurred while retrieving project informations
	 */
	protected IPath getProjectOutputPath() throws CoreException {

		IJavaProject javaProject = (IJavaProject) getProject().getNature(
				JavaCore.NATURE_ID);
		return javaProject.getOutputLocation();
	}

	/**
	 * Prepares two list of modified resources : source files and binary class
	 * files
	 * 
	 * @param aDeltaRoot
	 *            Root file container
	 * @param aJavaResourcesList
	 *            Java source files list
	 * @param aJavaClasslist
	 *            Java class files list
	 * @return True if the metadata.xml file was modified (not added to
	 *         aJavaResourcesList)
	 */
	protected boolean loadResourceDelta(final IResourceDelta aDeltaRoot,
			final List<IResource> aJavaResourcesList,
			final List<IResource> aJavaClasslist) {

		boolean needCompleteBuild = false;

		// Test resource name
		String resourceName = aDeltaRoot.getResource().getName();

		if (resourceName.endsWith(".java")) {
			// Test Java source file
			aJavaResourcesList.add(aDeltaRoot.getResource());

		} else if (resourceName.endsWith("metadata.xml")) {
			// Tests Metadata file
			needCompleteBuild = true;

		} else if (resourceName.endsWith(".class")) {
			// Tests binary .class file
			aJavaClasslist.add(aDeltaRoot.getResource());

		} else {
			// Test sub directories, if any
			IResourceDelta[] subdeltas = aDeltaRoot
					.getAffectedChildren(IResourceDelta.ADDED
							| IResourceDelta.CHANGED);

			if (subdeltas != null && subdeltas.length > 0) {
				for (IResourceDelta subdelta : subdeltas) {
					needCompleteBuild |= loadResourceDelta(subdelta,
							aJavaResourcesList, aJavaClasslist);
				}
			}
		}

		return needCompleteBuild;
	}

	/**
	 * Returns true if the source of the given class file is in the source list
	 * 
	 * @param javaClass
	 *            .class file tested
	 * @param aJavaSourceList
	 *            Modified .java source files
	 * @return True if the corresponding .java file is in the list
	 */
	protected boolean sourceChanged(final IResource javaClass,
			final ArrayList<IResource> aJavaSourceList) {

		String sourceName;
		try {
			sourceName = javaClass.getFullPath()
					.makeRelativeTo(getProjectOutputPath()).toString();
			int end = sourceName.lastIndexOf('$');
			if (end == -1) {
				end = sourceName.lastIndexOf('.');
			}

			if (end == -1) {
				Activator.logInfo(javaClass.getProject(),
						"Strange file name : " + sourceName);
				return true;
			}

			sourceName = sourceName.substring(0, end);
			sourceName += ".java";

		} catch (CoreException e) {
			Activator.logError(javaClass.getProject(),
					"Error working on file name", e);
			e.printStackTrace();
			return true;
		}

		for (IResource javaSource : aJavaSourceList) {
			if (javaSource.getFullPath().toString().contains(sourceName)) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Calls {@link ManifestUpdater#updateManifest(IProject)} on the current
	 * project
	 * 
	 * @throws CoreException
	 *             An error occurred during manipulation
	 */
	protected void updateManifest() throws CoreException {

		pManifestUpdater.updateManifest(getProject());

		// Store last update time
		getProject().setSessionProperty(PROJECT_LAST_BUILD,
				System.currentTimeMillis());
	}
}
