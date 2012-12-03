package org.ow2.chameleon.eclipse.ipojo.m2e;

import java.io.File;
import java.util.List;

import org.apache.maven.model.Plugin;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.configurator.AbstractProjectConfigurator;
import org.eclipse.m2e.core.project.configurator.ProjectConfigurationRequest;

public class IPojoProjectConfigurator extends AbstractProjectConfigurator {

	private static final String MAVEN_BUNDLE_INCLUDE_TAG = "_include";

	private static final String MAVEN_BUNDLE_INSTRUCTION_PLUGIN = "_plugin";

	private static final String MAVEN_BUNDLE_INSTRUCTIONS_TAG = "instructions";

	public static final String IPOJO_MOJO_GROUP_ID = "org.apache.felix";

	public static final String IPOJO_MOJO_ARTIFACT_ID = "maven-ipojo-plugin";

	public static final String BUNDLE_MOJO_GROUP_ID = "org.apache.felix";

	public static final String BUNDLE_MOJO_ARTIFACT_ID = "maven-bundle-plugin";

	public static final String IPOJO_BND_PLUGIN_ID = "org.apache.felix.ipojo.bnd.PojoizationPlugin";

	public static final String IPOJO_NATURE_ID = "org.ow2.chameleon.eclipse.ipojo.iPojoNature";

	@Override
	public void configure(ProjectConfigurationRequest request,
			IProgressMonitor monitor) throws CoreException {
		IMavenProjectFacade facade = request.getMavenProjectFacade();
		IProject project = request.getProject();

		if (isIPojoMavenProject(facade, monitor)) {
			addIPojoNature(project, monitor);
		}

	}

	public static void addIPojoNature(IProject project, IProgressMonitor monitor)
			throws CoreException {
		AbstractProjectConfigurator
				.addNature(project, IPOJO_NATURE_ID, monitor);
	}

	public static boolean isIPojoMavenProject(IMavenProjectFacade facade,
			IProgressMonitor monitor) throws CoreException {

		List<Plugin> plugins = facade.getMavenProject(monitor)
				.getBuildPlugins();
		if (plugins != null) {
			for (Plugin plugin : plugins) {
				if (isIPojoMavenProject(facade, plugin)
						&& !plugin.getExecutions().isEmpty()) {
					return true;
				}
			}
		}

		return false;
	}

	static boolean isIPojoMavenProject(IMavenProjectFacade facade, Plugin plugin) {
		return isMavenIPojoPluginMojo(plugin)
				|| (isMavenBundlePluginMojo(plugin) && hasIpojoBndPlugin(
						facade, plugin));
	}

	private static boolean hasIpojoBndPlugin(IMavenProjectFacade facade,
			Plugin plugin) {
		Xpp3Dom configuration = (Xpp3Dom) plugin.getConfiguration();
		Xpp3Dom instructions = configuration
				.getChild(MAVEN_BUNDLE_INSTRUCTIONS_TAG);
		if (instructions != null) {
			Xpp3Dom[] plugins = instructions
					.getChildren(MAVEN_BUNDLE_INSTRUCTION_PLUGIN);
			if (plugins != null) {
				// Looking for the bnd ipojo plugin in the instructions
				for (Xpp3Dom p : plugins) {
					String val = p.getValue();
					if (val != null && val.startsWith(IPOJO_BND_PLUGIN_ID)) {
						return true;
					}
				}
			}
			// We don't have the bnd ipojo plugin defined in the POM, let's
			// have a look in bnd files
			Xpp3Dom[] includes = instructions
					.getChildren(MAVEN_BUNDLE_INCLUDE_TAG);
			if (includes != null) {
				for (Xpp3Dom i : includes) {
					String fileName = i.getValue();
					if (usesBndPlugin(facade.getPomFile(), fileName)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	// Is the Bnd Ipojo Plugin defined in a bnd descriptor?
	private static boolean usesBndPlugin(File pomFile, String fileName) {
		// If you need this feature, please implement it!
		return false;
	}

	static boolean isMavenBundlePluginMojo(Plugin plugin) {
		return isMavenBundlePlugin(plugin.getGroupId(), plugin.getArtifactId());
	}

	static boolean isMavenBundlePlugin(String groupId, String artifactId) {
		return BUNDLE_MOJO_ARTIFACT_ID.equals(artifactId)
				&& BUNDLE_MOJO_GROUP_ID.equals(groupId);
	}

	static boolean isMavenIPojoPluginMojo(Plugin plugin) {
		return isMavenIPojoPlugin(plugin.getGroupId(), plugin.getArtifactId());
	}

	static boolean isMavenIPojoPlugin(String groupId, String artifactId) {
		return IPOJO_MOJO_ARTIFACT_ID.equals(artifactId)
				&& IPOJO_MOJO_GROUP_ID.equals(groupId);
	}

}
