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
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
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

    /** iPOJO Manifest updater */
    private final ManifestUpdater pManifestUpdater = new ManifestUpdater();

    /**
     * Updates the project manifest if an iPOJO meta-data file has been modified
     * 
     * @see IncrementalProjectBuilder#build(int, java.util.Map,
     *      org.eclipse.core.runtime.IProgressMonitor)
     */
    @SuppressWarnings("rawtypes")
    @Override
    protected IProject[] build(final int aKind, final Map aArgs,
            final IProgressMonitor aMonitor) throws CoreException {

        switch (aKind) {
        case FULL_BUILD:
            // Do nothing: the JDT participant will be called
            break;

        case AUTO_BUILD:
        case INCREMENTAL_BUILD:
            // Modify the manifest only if the metadate file has been changed
            final IResourceDelta resourceDelta = getDelta(getProject());
            final File specifiedMetadata = Utilities.INSTANCE
                    .getSpecifiedMetadataFile(getProject());

            if (hasMetadataChanged(resourceDelta, specifiedMetadata)) {
                /*
                 * Only update the manifest; this modification will make JDT/PDE
                 * rebuild the project
                 */
                updateManifest(aMonitor);
            }

            break;
        }

        // Tell Eclipse that we want to have deltas on next calls
        return new IProject[0];
    }

    /**
     * Removes the iPOJO-Components entry from the manifest of the project
     * 
     * @see IncrementalProjectBuilder#clean(org.eclipse.core.runtime.IProgressMonitor)
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
     * Tests if a meta data file has been modified, created or removed.
     * 
     * @param aDeltaRoot
     *            A resource delta descriptor
     * @return True if a meta data file has been modified
     */
    private boolean hasMetadataChanged(final IResourceDelta aDeltaRoot,
            final File aSpecifiedFile) {

        final IResource testedResource = aDeltaRoot.getResource();
        if (testedResource.getType() == IResource.FILE) {
            // Avoid testing something other than a file
            if (testedResource.getName().equalsIgnoreCase("metadata.xml")) {
                // Default Metadata file modification
                return true;

            } else if (aSpecifiedFile != null
                    && testedResource.getFullPath().toFile()
                            .equals(aSpecifiedFile)) {
                // Specified file modification
                return true;
            }

        } else {
            // Test sub directories, if any
            final IResourceDelta[] subdeltas = aDeltaRoot.getAffectedChildren();

            if (subdeltas != null && subdeltas.length > 0) {
                for (final IResourceDelta subdelta : subdeltas) {
                    if (hasMetadataChanged(subdelta, aSpecifiedFile)) {
                        // Found one
                        return true;
                    }
                }
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
    private void updateManifest(final IProgressMonitor aMonitor)
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
