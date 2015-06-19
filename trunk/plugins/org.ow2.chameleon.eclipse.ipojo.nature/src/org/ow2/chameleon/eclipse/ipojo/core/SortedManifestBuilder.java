/*
 * Copyright 2015 OW2 Chameleon
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
package org.ow2.chameleon.eclipse.ipojo.core;

import java.util.Map;
import java.util.TreeMap;

import org.apache.felix.ipojo.manipulator.store.builder.DefaultManifestBuilder;

/**
 * Extension of the default manifest builder : Import-Package value is now
 * sorted.
 * 
 * Avoids a weird behavior of JDT : when Import-Package value order changes, the
 * project is fully-rebuilt, calling iPOJO Builder again, modifying the
 * Import-Package again, etc.
 * 
 * @author Thomas Calmant
 */
public class SortedManifestBuilder extends DefaultManifestBuilder {

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.felix.ipojo.manipulator.store.builder.DefaultManifestBuilder
	 * #parseHeader(java.lang.String)
	 */
	@Override
	protected Map<String, Map<String, String>> parseHeader(final String aValue) {

		final TreeMap<String, Map<String, String>> sortedResult = new TreeMap<String, Map<String, String>>();
		sortedResult.putAll(super.parseHeader(aValue));
		return sortedResult;
	}
}
