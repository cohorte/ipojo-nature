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
package org.psem2m.eclipse.ipojo.builder;

import java.io.FileNotFoundException;
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
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.psem2m.eclipse.ipojo.Activator;
import org.psem2m.eclipse.ipojo.core.ManifestUpdater;

/**
 * iPOJO Manifest updater for iPOJO nature projects
 * 
 * @author Thomas Calmant
 */
public class IPojoBuilder extends IncrementalProjectBuilder {

	public static final String BUILDER_ID = "org.psem2m.eclipse.ipojo.ipojoBuilder";

	/** iPOJO Manifest updater */
	private ManifestUpdater pManifestUpdater = new ManifestUpdater();

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
			ArrayList<IResource> classFiles = new ArrayList<IResource>();
			getAllClassFiles(getProjectOutputContainer(), classFiles);
			updateManifest(classFiles);
			break;

		case AUTO_BUILD:
		case INCREMENTAL_BUILD:
			ArrayList<IResource> resources = new ArrayList<IResource>();
			ArrayList<IResource> classes = new ArrayList<IResource>();
			List<IResource> deltas = new ArrayList<IResource>();

			if (loadResourceDelta(getDelta(getProject()), resources, classes)) {
				// Metadata file modified : list all .class files
				getAllClassFiles(getProjectOutputContainer(), deltas);
			} else {
				// Filter lists to get only needed binaries
				deltas = filterLists(resources, classes);
			}

			if (deltas.size() > 0) {
				updateManifest(deltas);
			}
			break;
		}

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
	private ArrayList<IResource> filterLists(
			final ArrayList<IResource> aJavaSourceList,
			final ArrayList<IResource> aJavaClassList) {

		ArrayList<IResource> selectedClasses = new ArrayList<IResource>();

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
	private void getAllClassFiles(final IContainer aContainer,
			final List<IResource> aClassFileList) {

		if (aContainer == null) {
			return;
		}

		IResource[] members;

		try {
			members = aContainer.members();
		} catch (CoreException e) {
			Activator.logError("Error listing members in : "
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
	private IContainer getProjectOutputContainer() throws CoreException {
		return (IContainer) getProject().getWorkspace().getRoot()
				.findMember(getProjectOutputPath());
	}

	/**
	 * @return The Java project output path
	 * @throws CoreException
	 *             An error occurred while retrieving project informations
	 */
	private IPath getProjectOutputPath() throws CoreException {
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
	private boolean loadResourceDelta(final IResourceDelta aDeltaRoot,
			final List<IResource> aJavaResourcesList,
			final List<IResource> aJavaClasslist) {
		boolean foundMetadata = false;

		// Test resource name
		String resourceName = aDeltaRoot.getResource().getName();

		// Test Java source file
		if (resourceName.endsWith(".java")) {
			aJavaResourcesList.add(aDeltaRoot.getResource());
			// Tests Metadata file
		} else if (resourceName.endsWith("metadata.xml")) {
			foundMetadata = true;
			// Tests binary .class file
		} else if (resourceName.endsWith(".class")) {
			aJavaClasslist.add(aDeltaRoot.getResource());
		} else {
			// Test sub directories, if any
			IResourceDelta[] subdeltas = aDeltaRoot
					.getAffectedChildren(IResourceDelta.ADDED
							| IResourceDelta.CHANGED);

			if (subdeltas != null && subdeltas.length > 0) {
				for (IResourceDelta subdelta : subdeltas) {
					foundMetadata |= loadResourceDelta(subdelta,
							aJavaResourcesList, aJavaClasslist);
				}
			}
		}

		return foundMetadata;
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
	private boolean sourceChanged(final IResource javaClass,
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
				Activator.logInfo("Strange file name : " + sourceName);
				return true;
			}

			sourceName = sourceName.substring(0, end);
			sourceName += ".java";

		} catch (CoreException e) {
			Activator.logError("Error working on file name", e);
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
	 */
	private void updateManifest(final List<IResource> aResourceList) {
		try {
			pManifestUpdater.updateManifest(getProject(), aResourceList);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
}
