/*
 * Copyright 2013 OW2 Chameleon
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
package org.ow2.chameleon.eclipse.ipojo.m2e;

import java.io.File;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Plugin;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.configurator.AbstractProjectConfigurator;
import org.eclipse.m2e.core.project.configurator.ProjectConfigurationRequest;
import org.ow2.chameleon.eclipse.ipojo.core.ProjectUtilities;

/**
 * Sets up the iPOJO Nature to Maven projects using the iPOJO manipulation
 * plugin
 * 
 * @author Simon Chemouil
 */
public class IPojoProjectConfigurator extends AbstractProjectConfigurator {

	/** The iPOJO annotations artifact ID */
	private static final String ANNOTATIONS_ARTIFACT_ID = "";

	/** The iPOJO annotations group ID */
	private static final String ANNOTATIONS_GROUP_ID = "org.apache.felix";

	/** Bundle packager artifact ID */
	public static final String BUNDLE_MOJO_ARTIFACT_ID = "maven-bundle-plugin";

	/** Bundle packager group ID */
	public static final String BUNDLE_MOJO_GROUP_ID = "org.apache.felix";

	/** iPOJO manipulation Bnd plugin ID */
	public static final String IPOJO_BND_PLUGIN_ID = "org.apache.felix.ipojo.bnd.PojoizationPlugin";

	/** iPOJO manipulator artifact ID */
	public static final String IPOJO_MOJO_ARTIFACT_ID = "maven-ipojo-plugin";

	/** iPOJO manipulator group ID */
	public static final String IPOJO_MOJO_GROUP_ID = "org.apache.felix";

	private static final String MAVEN_BUNDLE_INCLUDE_TAG = "_include";

	private static final String MAVEN_BUNDLE_INSTRUCTION_PLUGIN = "_plugin";

	private static final String MAVEN_BUNDLE_INSTRUCTIONS_TAG = "instructions";

	/** Project manipulation utility bean */
	private final ProjectUtilities pUtilities = new ProjectUtilities();

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.m2e.core.project.configurator.AbstractProjectConfigurator
	 * #configure
	 * (org.eclipse.m2e.core.project.configurator.ProjectConfigurationRequest,
	 * org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public void configure(final ProjectConfigurationRequest request,
			final IProgressMonitor monitor) throws CoreException {

		final IMavenProjectFacade facade = request.getMavenProjectFacade();
		final IProject project = request.getProject();

		if (isIPojoMavenProject(facade, monitor)) {
			// iPOJO Project -> Add the iPOJO Nature
			pUtilities.addNature(project);

			if (usesAnnotations(facade, monitor)) {
				// Also add iPOJO annotations
				pUtilities.addAnnotations(project);
			}
		}
	}

	/**
	 * Tests if the project uses the Bnd iPojo plugin
	 * 
	 * @param facade
	 *            A Maven project facade
	 * @param plugin
	 *            A project plug-in
	 * @return True if the project uses the Bnd iPOJO plugin
	 */
	private boolean hasIpojoBndPlugin(final IMavenProjectFacade facade,
			final Plugin plugin) {

		final Xpp3Dom configuration = (Xpp3Dom) plugin.getConfiguration();
		final Xpp3Dom instructions = configuration
				.getChild(MAVEN_BUNDLE_INSTRUCTIONS_TAG);
		if (instructions != null) {
			final Xpp3Dom[] plugins = instructions
					.getChildren(MAVEN_BUNDLE_INSTRUCTION_PLUGIN);
			if (plugins != null) {
				// Looking for the bnd ipojo plugin in the instructions
				for (final Xpp3Dom p : plugins) {
					final String val = p.getValue();
					if (val != null && val.startsWith(IPOJO_BND_PLUGIN_ID)) {
						return true;
					}
				}
			}
			// We don't have the bnd ipojo plugin defined in the POM, let's
			// have a look in bnd files
			final Xpp3Dom[] includes = instructions
					.getChildren(MAVEN_BUNDLE_INCLUDE_TAG);
			if (includes != null) {
				for (final Xpp3Dom i : includes) {
					final String fileName = i.getValue();
					if (usesBndPlugin(facade.getPomFile(), fileName)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	/**
	 * Tests if the given Maven project uses the iPOJO plugin
	 * 
	 * @param facade
	 *            A Maven project facade
	 * @param monitor
	 *            The progression monitor
	 * @return True if the project uses iPOJO
	 * @throws CoreException
	 *             Error reading project information
	 */
	public boolean isIPojoMavenProject(final IMavenProjectFacade facade,
			final IProgressMonitor monitor) throws CoreException {

		final List<Plugin> plugins = facade.getMavenProject(monitor)
				.getBuildPlugins();
		if (plugins != null) {
			for (final Plugin plugin : plugins) {
				if (isIPojoMavenProject(facade, plugin)
						&& !plugin.getExecutions().isEmpty()) {
					return true;
				}
			}
		}

		return false;
	}

	/**
	 * Tests if the given plugin is the iPOJO one
	 * 
	 * @param facade
	 *            A Maven project facade
	 * @param plugin
	 *            A Maven plugin
	 * @return True if the given plugin is the iPOJO manipulator
	 */
	public boolean isIPojoMavenProject(final IMavenProjectFacade facade,
			final Plugin plugin) {

		return isMavenIPojoPluginMojo(plugin)
				|| (isMavenBundlePluginMojo(plugin) && hasIpojoBndPlugin(
						facade, plugin));
	}

	/**
	 * Tests if the given plugin information corresponds to the bundle packager
	 * 
	 * @param groupId
	 *            Plugin group ID
	 * @param artifactId
	 *            Plugin artifact ID
	 * @return True if the plugin is {@value #BUNDLE_MOJO_GROUP_ID}.
	 *         {@value #BUNDLE_MOJO_ARTIFACT_ID}
	 */
	public boolean isMavenBundlePlugin(final String groupId,
			final String artifactId) {

		return BUNDLE_MOJO_ARTIFACT_ID.equals(artifactId)
				&& BUNDLE_MOJO_GROUP_ID.equals(groupId);
	}

	/**
	 * Tests if the given plugin is the bundle packager
	 * 
	 * @param plugin
	 *            A Maven plugin
	 * @return True if the plugin is {@value #BUNDLE_MOJO_GROUP_ID}.
	 *         {@value #BUNDLE_MOJO_ARTIFACT_ID}
	 */
	public boolean isMavenBundlePluginMojo(final Plugin plugin) {

		return isMavenBundlePlugin(plugin.getGroupId(), plugin.getArtifactId());
	}

	/**
	 * Tests if the given plugin information corresponds to the iPOJO
	 * manipulator
	 * 
	 * @param groupId
	 *            Plugin group ID
	 * @param artifactId
	 *            Plugin artifact ID
	 * @return True if the plugin is {@value #BUNDLE_MOJO_GROUP_ID}.
	 *         {@value #BUNDLE_MOJO_ARTIFACT_ID}
	 */
	public boolean isMavenIPojoPlugin(final String groupId,
			final String artifactId) {

		return IPOJO_MOJO_ARTIFACT_ID.equals(artifactId)
				&& IPOJO_MOJO_GROUP_ID.equals(groupId);
	}

	/**
	 * Tests if the given plugin information corresponds to the iPOJO
	 * manipulator
	 * 
	 * @param groupId
	 *            Plugin group ID
	 * @param artifactId
	 *            Plugin artifact ID
	 * @return True if the plugin is {@value #BUNDLE_MOJO_GROUP_ID}.
	 *         {@value #BUNDLE_MOJO_ARTIFACT_ID}
	 */
	public boolean isMavenIPojoPluginMojo(final Plugin plugin) {

		return isMavenIPojoPlugin(plugin.getGroupId(), plugin.getArtifactId());
	}

	/**
	 * Tests if the project depends on iPOJO Annotations
	 * 
	 * @param aFacade
	 *            A Maven project facade
	 * @param aMonitor
	 *            The progression monitor
	 * @return True if the projects uses annotations
	 * @throws CoreException
	 *             Error reading Maven project
	 */
	public boolean usesAnnotations(final IMavenProjectFacade aFacade,
			final IProgressMonitor aMonitor) throws CoreException {

		final Set<Artifact> dependencies = aFacade.getMavenProject(aMonitor)
				.getDependencyArtifacts();
		for (final Artifact artifact : dependencies) {
			if (ANNOTATIONS_GROUP_ID.equals(artifact.getGroupId())
					&& ANNOTATIONS_ARTIFACT_ID.equals(artifact.getArtifactId())) {
				// Found artifacts
				return true;
			}
		}

		return false;
	}

	/**
	 * Tests if the Bnd Ipojo Plugin is defined in a bnd descriptor.
	 * 
	 * <b>NOT IMPLEMENTENTED</b>
	 * 
	 * @param pomFile
	 * @param fileName
	 * @return
	 */
	private boolean usesBndPlugin(final File pomFile, final String fileName) {

		// If you need this feature, please implement it!
		return false;
	}
}
