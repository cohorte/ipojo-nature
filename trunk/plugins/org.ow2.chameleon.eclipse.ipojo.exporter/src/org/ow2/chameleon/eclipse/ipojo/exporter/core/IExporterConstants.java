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
package org.ow2.chameleon.eclipse.ipojo.exporter.core;

/**
 * iPOJO Export wizard constant
 * 
 * @author Thomas Calmant
 */
public interface IExporterConstants {

	/** Additional binary includes */
	String BUILD_PROPERTIES_KEY_BIN_INCLUDES = "bin.includes";

	/** Build output */
	String BUILD_PROPERTIES_KEY_OUTPUT = "output..";

	/** Build.properties file path, relative to a project root */
	String BUILD_PROPERTIES_PATH = "/build.properties";
}
