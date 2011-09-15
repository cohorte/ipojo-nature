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

import org.apache.felix.ipojo.InstanceManager;
import org.apache.felix.ipojo.manipulation.MethodCreator;
import org.apache.felix.ipojo.metadata.Attribute;
import org.apache.felix.ipojo.metadata.Element;

/**
 * Filters the given meta data elements to removes iPOJO injected members from
 * the manifest
 * 
 * @author Thomas Calmant
 */
public class MetadataIpojoElementFilter implements
		org.apache.felix.ipojo.manipulator.render.MetadataFilter {

	/** Element arguments attribute name */
	public static final String ELEMENT_ATTRIBUTE_ARGUMENTS = "arguments";

	/** iPOJO meta data element name attribute */
	public static final String ELEMENT_ATTRIBUTE_NAME = "name";

	/** Constructor method name */
	public static final String ELEMENT_CONSTRUCTOR_VALUE = "$init";

	/** Instance manager class name */
	public static final String INSTANCEMANAGER_CLASS_NAME = InstanceManager.class
			.getName();

	/**
	 * iPOJO injected elements prefixes
	 * 
	 * @see MethodCreatod
	 */
	public static final String[] IPOJO_PREFIXES = {
	/** Field flag prefix */
	"__F",
	/** Method flag prefix */
	"__M",
	/** Injected getter */
	"__get",
	/** Injected setter */
	"__set" };

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.felix.ipojo.manipulator.render.MetadataFilter#accept(org.apache
	 * .felix.ipojo.metadata.Element)
	 */
	@Override
	public boolean accept(final Element aElement) {

		if (isInjectedConstant(aElement.getAttribute(ELEMENT_ATTRIBUTE_NAME))
				|| isInjectedConstructor(aElement)) {
			// The given element has been injected
			return false;
		}

		if (!filterElement(null, aElement)) {
			// Root element has been injected
			return false;
		}

		return true;
	}

	/**
	 * Recursively prepares a formatted string representing the content of the
	 * given iPOJO meta data element, for debug purpose.
	 * 
	 * @param aBuilder
	 *            String builder (can't be null)
	 * @param aElement
	 *            Root element.
	 * @param aPrefix
	 *            Line prefix
	 */
	public void buildMetadataElementString(final StringBuilder aBuilder,
			final Element aElement, final String aPrefix) {

		final String prefix = aPrefix + "  ";

		aBuilder.append(aPrefix);
		aBuilder.append("Element {\n");

		for (Attribute attr : aElement.getAttributes()) {
			aBuilder.append(prefix);
			aBuilder.append(attr.getName()).append(" : " + attr.getValue());
			aBuilder.append("\n");
		}

		for (Element elem : aElement.getElements()) {
			buildMetadataElementString(aBuilder, elem, prefix);
		}

		aBuilder.append(aPrefix);
		aBuilder.append("}\n");
	}

	/**
	 * Recursively filters the given element object. If aParentElement, the
	 * returned value is significant, else the element structure will be
	 * filtered as needed.
	 * 
	 * @param aParentElement
	 *            Root element (null for the first recursion level)
	 * @param aElement
	 *            The element to be tested
	 * @return True if the element (first of recursion) can be accepted, else
	 *         false
	 */
	protected boolean filterElement(final Element aParentElement,
			final Element aElement) {

		final String elementName = aElement
				.getAttribute(ELEMENT_ATTRIBUTE_NAME);

		if (elementName != null) {

			boolean filtered = false;

			if (isInjectedConstant(elementName)
					|| isInjectedConstructor(aElement)
					|| nameExistsWithoutPrefix(aParentElement, elementName)) {

				filtered = true;
			}

			if (filtered) {
				// Filtered element must be handled

				if (aParentElement == null) {
					// Can't remove the element, so we don't want the current
					// one
					return false;
				} else {
					// Remove the element from the model
					aParentElement.removeElement(aElement);
				}
			}
		}

		// Recursive thing
		for (Element subElement : aElement.getElements()) {
			/*
			 * Do not worry about the method result : as the parent is not null,
			 * sub elements can be removed
			 */
			filterElement(aElement, subElement);
		}

		// Sub elements have been filtered
		return true;
	}

	/**
	 * Tests if the given element name corresponds to an iPOJO injected /
	 * renamed member.
	 * 
	 * @param aElementName
	 *            Tested element name
	 * @return True if the given name is a pure injection
	 */
	protected boolean isInjectedConstant(final String aElementName) {

		return aElementName.equals(MethodCreator.IM_FIELD)
				|| aElementName.startsWith(MethodCreator.PREFIX);
	}

	/**
	 * Tests if the given element is an iPOJO injected constructor
	 * 
	 * @param aElement
	 *            Element to be tested
	 * @return True if the element is an injected constructor, else false
	 */
	protected boolean isInjectedConstructor(final Element aElement) {

		final String elementName = aElement
				.getAttribute(ELEMENT_ATTRIBUTE_NAME);

		if (!ELEMENT_CONSTRUCTOR_VALUE.equals(elementName)) {
			// It's not a constructor
			return false;
		}

		final String constructorArguments = aElement
				.getAttribute(ELEMENT_ATTRIBUTE_ARGUMENTS);
		if (constructorArguments == null) {
			// No arguments, not injected
			return false;
		}

		if (constructorArguments.contains(INSTANCEMANAGER_CLASS_NAME)) {
			// One of the parameters is the instance manager -> injected
			return true;
		}

		// Standard constructor
		return false;
	}

	/**
	 * Tests if the given element name attribute exists without prefix at the
	 * same level, or if it contains some clues indicating that it's an iPOJO
	 * injection result. Returns false if the parent element is null.
	 * 
	 * @param aParentElement
	 *            Parent element
	 * @param aElementName
	 *            Element name attribute content
	 * @return True if an element have the same name, without an iPOJO prefix
	 */
	protected boolean nameExistsWithoutPrefix(final Element aParentElement,
			final String aElementName) {

		if (aParentElement == null) {
			// Don't handle what we can't handle
			return false;
		}

		String nameWithoutPrefix = null;

		for (String prefix : IPOJO_PREFIXES) {
			// Test all known prefixes
			if (aElementName.startsWith(prefix)) {
				nameWithoutPrefix = aElementName.substring(prefix.length());
			}
		}

		if (nameWithoutPrefix == null) {
			// Not an iPOJO prefix, ignore it
			return false;
		}

		/* Fast test : if it contains a '$' (dollar), it has been injected */
		if (nameWithoutPrefix.indexOf('$') != -1) {
			return true;
		}

		// Test all sub-elements names
		for (Element subElement : aParentElement.getElements()) {

			if (subElement.getAttribute(ELEMENT_ATTRIBUTE_NAME).equals(
					nameWithoutPrefix)) {
				// Found it
				return true;
			}
		}

		return false;
	}
}
