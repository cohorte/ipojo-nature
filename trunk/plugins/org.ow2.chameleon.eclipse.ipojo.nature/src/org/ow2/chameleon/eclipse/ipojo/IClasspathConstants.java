/*
 * Copyright 2011 OW2 Chameleon
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
package org.ow2.chameleon.eclipse.ipojo;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

/**
 * Defines iPOJO Nature class path constants
 * 
 * @author Thomas Calmant
 */
public interface IClasspathConstants {

	/** Annotations access rules file pattern */
	IPath ANNOTATIONS_ACCESS_PATTERN = new Path(
			IClasspathConstants.ANNOTATIONS_ACCESS_PATTERN_STR);

	/** Annotations access rules file pattern string */
	String ANNOTATIONS_ACCESS_PATTERN_STR = "org/apache/felix/ipojo/**";

	/** The class path container path object */
	IPath ANNOTATIONS_CONTAINER_PATH = new Path(
			IClasspathConstants.ANNOTATIONS_CONTAINER_PATH_STR);

	/** The iPOJO annotations class path container path */
	String ANNOTATIONS_CONTAINER_PATH_STR = "org.ow2.chameleon.ipojo.CLASSPATH_CONTAINER";

	/** Path of the PDE "Plug-in Dependencies" container path object */
	IPath PDE_CONTAINER_PATH = new Path("org.eclipse.pde.core.requiredPlugins");
}
