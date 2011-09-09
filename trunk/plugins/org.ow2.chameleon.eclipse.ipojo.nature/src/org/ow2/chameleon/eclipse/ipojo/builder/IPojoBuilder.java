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
import org.eclipse.core.runtime.NullProgressMonitor;
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
	protected IProject[] build(final int aKind, final Map aArgs,
			final IProgressMonitor aMonitor) throws CoreException {

		switch (aKind) {
		case FULL_BUILD:
			updateManifest(aMonitor);
			break;

		case AUTO_BUILD:
		case INCREMENTAL_BUILD:
			final ArrayList<IResource> resources = new ArrayList<IResource>();
			final ArrayList<IResource> classes = new ArrayList<IResource>();
			final IResourceDelta resourceDelta = getDelta(getProject());

			List<IResource> deltas = new ArrayList<IResource>();

			boolean metadataModified = false;
			if (resourceDelta != null) {

				metadataModified = loadResourceDelta(resourceDelta, resources,
						classes);
			}

			if (metadataModified || resourceDelta == null) {
				// Full build or metadata file modified : list all .class files
				getAllClassFiles(getProjectOutputContainer(), deltas);

			} else {
				// Filter lists to get only needed binaries
				deltas = filterLists(resources, classes);
			}

			// Do the work if needed
			if (metadataModified || !deltas.isEmpty()) {
				updateManifest(aMonitor);
			}

			break;
		}

		// Null, IProject, keep a list of handled projects ???
		return new IProject[0];
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

		final ArrayList<IResource> selectedClasses = new ArrayList<IResource>();

		for (IResource javaClass : aJavaClassList) {
			if (sourceChanged(javaClass, aJavaSourceList)) {
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

		boolean metadataModified = false;

		// Test resource name
		String resourceName = aDeltaRoot.getResource().getName();

		if (resourceName.endsWith(".java")) {
			// Test Java source file
			aJavaResourcesList.add(aDeltaRoot.getResource());

		} else if (resourceName.endsWith("metadata.xml")) {
			// Tests Metadata file
			metadataModified = true;

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
					metadataModified |= loadResourceDelta(subdelta,
							aJavaResourcesList, aJavaClasslist);
				}
			}
		}

		return metadataModified;
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
	 * @param aMonitor
	 *            Progress monitor
	 * 
	 * @throws CoreException
	 *             An error occurred during manipulation
	 */
	protected void updateManifest(final IProgressMonitor aMonitor)
			throws CoreException {

		IProgressMonitor monitor = aMonitor;
		if (monitor == null) {
			monitor = new NullProgressMonitor();
		}

		pManifestUpdater.updateManifest(getProject());
	}
}
