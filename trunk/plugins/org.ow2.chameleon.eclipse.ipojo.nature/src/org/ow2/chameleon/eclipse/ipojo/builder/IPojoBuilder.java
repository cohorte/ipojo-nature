/*
 * Copyright 2012 OW2 Chameleon
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

import java.io.File;
import java.text.MessageFormat;
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
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.ui.statushandlers.StatusManager;
import org.ow2.chameleon.eclipse.ipojo.Activator;
import org.ow2.chameleon.eclipse.ipojo.core.ManifestUpdater;
import org.ow2.chameleon.eclipse.ipojo.core.Utilities;

/**
 * iPOJO Manifest updater for iPOJO nature projects.
 * 
 * <b>DEACTIVATED</b> by a comment in the file plugin.xml.
 * 
 * @author Thomas Calmant
 */
public class IPojoBuilder extends IncrementalProjectBuilder {

	/** Plugin Builder ID */
	public static final String BUILDER_ID = "org.ow2.chameleon.eclipse.ipojo.ipojoBuilder";

	/** Last manipulation project session property */
	public static final QualifiedName PROJECT_LAST_MANIPULATION = new QualifiedName(
			BUILDER_ID, "project.last_manipulation");

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

		final SubMonitor subMonitor = SubMonitor.convert(aMonitor,
				"iPOJO Build", 100);

		switch (aKind) {
		case FULL_BUILD:
			updateManifest(subMonitor);
			break;

		case AUTO_BUILD:
		case INCREMENTAL_BUILD:
			final ArrayList<IResource> resources = new ArrayList<IResource>();
			final ArrayList<IResource> classes = new ArrayList<IResource>();
			final IResourceDelta resourceDelta = getDelta(getProject());

			List<IResource> deltas = new ArrayList<IResource>();

			// Preparation progress monitor
			final IProgressMonitor preparationMonitor = subMonitor.newChild(10);
			preparationMonitor.beginTask("Prepare incremental build", 10);

			boolean metadataModified = false;
			if (resourceDelta != null) {

				metadataModified = loadResourceDelta(resourceDelta, resources,
						classes);

				preparationMonitor.worked(5);
			}

			if (preparationMonitor.isCanceled()) {
				// Test cancellation
				break;
			}

			if (metadataModified || resourceDelta == null) {
				// Full build or metadata file modified : list all .class files
				getAllClassFiles(getProjectOutputContainer(), deltas);
				preparationMonitor.worked(5);

			} else {
				// Filter lists to get only needed binaries
				deltas = filterLists(resources, classes);
				preparationMonitor.worked(5);
			}

			if (preparationMonitor.isCanceled()) {
				// Test cancellation
				break;
			}

			// Test on specified metadata
			metadataModified |= hasSpecifiedMetadataChanged();

			// Do the work if needed
			if (metadataModified || !deltas.isEmpty()) {
				updateManifest(subMonitor.newChild(90));
			}

			aMonitor.done();
			break;
		}

		// Null, IProject, keep a list of handled projects ???
		return new IProject[0];
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.core.resources.IncrementalProjectBuilder#clean(org.eclipse
	 * .core.runtime.IProgressMonitor)
	 */
	@Override
	protected void clean(final IProgressMonitor aMonitor) throws CoreException {

		super.clean(aMonitor);

		// Remove the iPOJO-Component Manifest entry
		try {
			pManifestUpdater.removeManifestEntry(getProject());

		} catch (final CoreException ex) {
			Activator
					.logError(
							getProject(),
							"Something went wrong while cleaning the manifest file",
							ex);
		}
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
	protected List<IResource> filterLists(
			final List<IResource> aJavaSourceList,
			final List<IResource> aJavaClassList) {

		final ArrayList<IResource> selectedClasses = new ArrayList<IResource>();

		for (final IResource javaClass : aJavaClassList) {
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
		} catch (final CoreException e) {
			Activator.logError(aContainer.getProject(), MessageFormat.format(
					"Error listing members in : {0}", aContainer.getFullPath()
							.toOSString()), e);
			return;
		}

		for (final IResource member : members) {
			if (member.getType() == IResource.FOLDER) {
				getAllClassFiles((IContainer) member, aClassFileList);

			} else if (member.getType() == IResource.FILE
					&& member.getName().endsWith(".class")) {
				aClassFileList.add(member);
			}
		}
	}

	/**
	 * Retrieves the IContainer of the Java project output
	 * 
	 * @return The Java project output container
	 * @throws CoreException
	 *             An error occurred while retrieving project informations
	 */
	protected IContainer getProjectOutputContainer() throws CoreException {
		return (IContainer) getProject().getWorkspace().getRoot()
				.findMember(getProjectOutputPath());
	}

	/**
	 * Retrieves the Java project output path
	 * 
	 * @return The Java project output path
	 * @throws CoreException
	 *             An error occurred while retrieving project informations
	 */
	protected IPath getProjectOutputPath() throws CoreException {

		final IJavaProject javaProject = (IJavaProject) getProject().getNature(
				JavaCore.NATURE_ID);
		return javaProject.getOutputLocation();
	}

	/**
	 * Tests if the specified meta data file has been modified since last build.
	 * Returns false if the file does not exist or if no file is specified.
	 * 
	 * @return True if the file has been modified since last build
	 */
	protected boolean hasSpecifiedMetadataChanged() {

		final File specifiedFile = Utilities.INSTANCE
				.getSpecifiedMetadataFile(getProject());

		if (specifiedFile == null || !specifiedFile.exists()) {
			// No file
			return false;
		}

		// Read the last manipulation time stamp
		long lastProjectManipulation;

		try {
			final Object lastProjectManipulationValue = getProject()
					.getSessionProperty(PROJECT_LAST_MANIPULATION);

			if (lastProjectManipulationValue instanceof Long) {
				// Convert the value
				lastProjectManipulation = (Long) lastProjectManipulationValue;

			} else {
				// No such property
				lastProjectManipulation = 0;
			}

		} catch (final CoreException e) {
			// Error reading property
			lastProjectManipulation = 0;
		}

		return specifiedFile.lastModified() > lastProjectManipulation;
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
		final String resourceName = aDeltaRoot.getResource().getName();

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
			final IResourceDelta[] subdeltas = aDeltaRoot
					.getAffectedChildren(IResourceDelta.ADDED
							| IResourceDelta.CHANGED);

			if (subdeltas != null && subdeltas.length > 0) {
				for (final IResourceDelta subdelta : subdeltas) {
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
			final List<IResource> aJavaSourceList) {

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

		} catch (final CoreException e) {
			Activator.logError(javaClass.getProject(),
					"Error working on file name", e);
			return true;
		}

		for (final IResource javaSource : aJavaSourceList) {
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
		if (aMonitor == null) {
			monitor = new NullProgressMonitor();

		} else if (aMonitor.isCanceled()) {
			// Work cancelled
			return;
		}

		// Do the job
		final IStatus result = pManifestUpdater.updateManifest(getProject(),
				monitor);

		// Store the manipulation time
		getProject().setSessionProperty(PROJECT_LAST_MANIPULATION,
				Long.valueOf(System.currentTimeMillis()));

		// Log the result
		if (result.isOK()) {
			// No problem : full success
			Activator.logInfo(getProject(), "Manipulation done");

		} else {
			// Errors have already been logged, so just pop a dialog
			StatusManager.getManager().handle(result, StatusManager.SHOW);
		}
	}
}
