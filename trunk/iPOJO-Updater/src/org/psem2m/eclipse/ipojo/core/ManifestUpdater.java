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
package org.psem2m.eclipse.ipojo.core;

import java.io.FileNotFoundException;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.JavaCore;
import org.psem2m.eclipse.ipojo.Activator;

import fr.imag.adele.cadse.builder.iPojo.EclipsePojoization;

/**
 * Utility class applying iPOJO configuration to the Manifest file
 * 
 * @author Thomas Calmant
 */
public class ManifestUpdater {

	/**
	 * Search for the given file
	 * 
	 * @param aRoot
	 *            Root container to look in
	 * @param aFileName
	 *            File to look for
	 * @return The File, or null if not found
	 * @throws CoreException
	 *             An error occurred while reading members list (mainly on
	 *             remote files)
	 */
	private IFile findFile(final IContainer aRoot, final String aFileName) {

		if (aRoot == null || aFileName == null || aFileName.isEmpty()) {
			return null;
		}

		IResource[] members;
		try {
			members = aRoot.members();
		} catch (CoreException ex) {
			Activator.logError("Error searching for file '" + aFileName + "'",
					ex);
			return null;
		}

		if (members == null) {
			return null;
		}

		for (IResource resource : members) {

			if (resource.getName().equalsIgnoreCase(aFileName)) {
				if (resource.getType() == IResource.FILE) {
					return (IFile) resource;
				}

			} else if (resource.getType() == IResource.FOLDER) {
				IFile found = findFile((IContainer) resource, aFileName);
				if (found != null) {
					return found;
				}
			}
		}

		return null;
	}

	/**
	 * Verifies the Nature of the given project
	 * 
	 * @param aProject
	 *            Project to be tested
	 * @return True if the project is of JavaCore nature
	 */
	private boolean isJavaProject(final IProject aProject) {
		try {
			return aProject.hasNature(JavaCore.NATURE_ID);
		} catch (Exception ex) {
			Activator.logError("Error retrieving project nature", ex);
			return false;
		}
	}

	/**
	 * Applies a full iPOJO update on the project Manifest.
	 * 
	 * @param aProject
	 *            Eclipse Java project containing the Manifest
	 * @throws FileNotFoundException
	 *             No manifest found
	 */
	public void updateManifest(final IProject aProject)
			throws FileNotFoundException {
		updateManifest(aProject, null);
	}

	/**
	 * Applies iPOJO update on the project Manifest. Manifest file must be
	 * refreshed after calling this method
	 * 
	 * @param aProject
	 *            Parent project of the Manifest
	 * @param delta
	 * @throws FileNotFoundException
	 *             No Manifest or no source path found
	 */
	public void updateManifest(final IProject aProject,
			final List<IResource> delta) throws FileNotFoundException {

		if (!isJavaProject(aProject)) {
			Activator.logInfo("Not a Java project");
			return;
		}

		IWorkspaceRoot workspaceRoot = aProject.getWorkspace().getRoot();

		// Search for the Manifest
		IFile manifestIFile = findFile(aProject, "MANIFEST.MF");
		if (manifestIFile == null) {
			Activator.logError("Manifest file not found", null);
			return;
		}

		// Conversion to iPOJO understandable file
		CompositeFile manifestFile = new CompositeFile(workspaceRoot,
				manifestIFile.getFullPath());

		// Search for Metadata.xml file
		CompositeFile metadataFile = null;
		IFile metadataIFile = findFile(aProject, "metadata.xml");
		if (metadataIFile == null) {
			Activator.logInfo("No metadata.xml file found (not critical)");

		} else {

			// Convert the IFile to an iPOJO understandable File
			metadataFile = new CompositeFile(workspaceRoot,
					metadataIFile.getFullPath());
		}

		// iPOJO application
		try {
			EclipsePojoization pojo = new EclipsePojoization(delta);
			if (pojo.directoryPojoization(aProject, metadataFile, manifestFile)) {
				Activator.logInfo("iPOJO transformation done");
			} else {
				Activator.logError("iPOJO transformation ended with errors",
						null);
			}
		} catch (Exception ex) {
			Activator.logError("iPOJO manipulation error", ex);
		}

		// Refresh UI
		try {
			aProject.refreshLocal(IResource.DEPTH_INFINITE, null);
		} catch (CoreException ex) {
			Activator.logError("Project refresh error", ex);
		}
	}
}
