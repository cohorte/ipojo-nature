/**
 * File:   SortedAttributes.java
 * Author: Thomas Calmant
 * Date:   22 juin 2011
 */
package org.psem2m.eclipse.ipojo.core;

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
