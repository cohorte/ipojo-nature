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
package org.ow2.chameleon.eclipse.ipojo.core;

import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;
import java.util.jar.Attributes;

/**
 * Writable copy of an {@link Attributes} object with sorted entries.
 * 
 * The {@link Attributes#map} field is replaced by a {@link TreeMap}.
 * 
 * @author Thomas Calmant
 */
public class SortedAttributes extends Attributes {

	/**
	 * Compare two objects according to their string representation
	 * 
	 * @author Thomas Calmant
	 * 
	 */
	protected static class ObjectStringComparator implements Comparator<Object> {

		@Override
		public int compare(final Object aObject1, final Object aObject2) {
			return aObject1.toString().compareTo(aObject2.toString());
		}
	}

	/**
	 * Acts like the given parent but uses a sorted map
	 * 
	 * @param aParent
	 *            The parent attributes
	 */
	public SortedAttributes(final Attributes aParent) {
		super();

		// Override the parent map by a sorted map
		Map<Object, Object> newMap = new TreeMap<Object, Object>(
				new ObjectStringComparator());

		newMap.putAll(aParent);
		map = newMap;
	}
}
