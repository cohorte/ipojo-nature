package org.ow2.chameleon.eclipse.ipojo.core;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

/**
 * 
 * 
 * @author ogattaz
 *
 */
public class ManifestManipulator {

	public static final String ARGUMENT_FULL_RESOURCE_PATH = "fullResourcePath";

	public static final String ARGUMENT_RESOURCE_NAME = "resourceName";

	public static final String ARGUMENT_RESOURCE_PATH = "resourcePath";

	public static final String ATTRIBUTE_IPOJO_NAME = "iPOJO-Components";

	public static final String ATTRIBUTE_MF_VESRION = "Manifest-Version";

	private static final String MANIFEST_MINIMAL = "Manifest-Version: 1.0\n\r\n\r";

	public static final String MANIFEST_VERSION_1 = "1.0";

	/**
	 * @param aBuffer
	 * @return
	 */
	private static String dumpInputStreamInfos(final InputStream aInputStream) {
		if (aInputStream == null) {
			return "available=[-1]";
		}
		try {
			return String.valueOf(aInputStream.available());
		} catch (IOException e) {
			return e.getMessage();
		}
	}

	/**
	 * @param aThrowable
	 * @return
	 */
	private static String eStackInString(Throwable aThrowable) {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		aThrowable.printStackTrace(pw);
		// stack trace as a string
		return sw.toString();
	}

	/**
	 * @return
	 * @throws Exception
	 */
	private static InputStream manifestMinimalToStream() throws Exception {

		// create input stream from baos
		InputStream isFromFirstData = new ByteArrayInputStream(MANIFEST_MINIMAL.getBytes(StandardCharsets.UTF_8));

		return isFromFirstData;
	}

	/**
	 * @param aManifest
	 * @return
	 * @throws Exception
	 */
	private static InputStream manifestToStream(final Manifest aManifest) throws Exception {

		// create temporary bayte array output stream
		ByteArrayOutputStream wOutputStream = new ByteArrayOutputStream();

		aManifest.write(wOutputStream);

		// create input stream from baos
		InputStream isFromFirstData = new ByteArrayInputStream(wOutputStream.toByteArray());

		return isFromFirstData;
	}

	/**
	 * @param aFullResourcePath
	 * @return
	 * @throws Exception
	 */
	public static byte[] readResourceBytes(final String aFullResourcePath) throws Exception {

		String wFullResourcePath = validNotNullAndNotEmpty(aFullResourcePath, ARGUMENT_FULL_RESOURCE_PATH);

		URL wResourceUrl = ManifestManipulator.class.getResource(wFullResourcePath);

		Path wResourcePath = Paths.get(wResourceUrl.toURI());

		return Files.readAllBytes(wResourcePath);
	}

	/**
	 * @param aName
	 * @param aResourcePath
	 * @param aBuffer
	 * @return
	 * @throws Exception
	 */
	public static byte[] readResourceBytes(final String aResourceName, final String aResourcePath) throws Exception {

		String wFullResourcePath = String.format("%s/%s",
				//
				validNotNullAndNotEmpty(aResourcePath, ARGUMENT_RESOURCE_PATH),
				//
				validNotNullAndNotEmpty(aResourceName, ARGUMENT_RESOURCE_NAME));

		return readResourceBytes(wFullResourcePath);
	}

	/**
	 * @param aValue
	 * @param aInfo
	 * @return
	 */
	private static String validNotNullAndNotEmpty(final String aValue, final String aInfo) throws Exception {
		if (aValue == null) {
			throw new IllegalArgumentException(String.format("The given [%s] is null", aInfo));
		}
		if (aValue.isEmpty()) {
			throw new IllegalArgumentException(String.format("The given [%s] is empty", aInfo));
		}
		if (aValue.isBlank()) {
			throw new IllegalArgumentException(String.format("The given [%s] is blank", aInfo));
		}
		return aValue;
	}

	private final Manifest pManifest;

	/**
	 * @throws Exception
	 */
	public ManifestManipulator() throws Exception {
		this(manifestMinimalToStream());
	}

	/**
	 * @param aName
	 * @param aResourcePath
	 * @param aBuffer
	 * @throws IOException
	 */
	public ManifestManipulator(final InputStream aInputStream) throws Exception {

		super();
		try {

			pManifest = new Manifest(aInputStream);
		} catch (Exception e) {
			throw new Exception(String.format("Unable to instanciate ManifestManipulator using the InputStream: %s",
					dumpInputStreamInfos(aInputStream)), e);
		}
	}

	/**
	 * @param aManifest
	 * @throws Exception
	 */
	public ManifestManipulator(final Manifest aManifest) throws Exception {
		this(manifestToStream(aManifest));
	}

	/**
	 * @param aIPojoAttribute
	 * @return
	 */
	public boolean appendIPojoAttribute(final String aIPojoAttribute) {

		Object wPrevious = getMainAttributes().putIfAbsent(new Attributes.Name(ATTRIBUTE_IPOJO_NAME), aIPojoAttribute);

		return wPrevious == null;
	}

	/**
	 * @return
	 */
	public Map<String, Attributes> getEntries() {
		return getManifest().getEntries();
	}

	/**
	 * @return
	 */
	public int getIPojoAttributeRank() {
		if (!this.hasIPojoAttribute()) {
			return -1;
		}
		int wRank = 0;
		for (Object wKey : getMainAttributes().keySet()) {
			if (String.valueOf(wKey).equals(ATTRIBUTE_IPOJO_NAME)) {
				return wRank;
			}
			wRank++;
		}

		return -1;
	}

	/**
	 * @return
	 */
	public String getIPojoAttributeValue() {

		return getMainAttributes().getValue(ATTRIBUTE_IPOJO_NAME);
	}

	/**
	 * @return
	 */
	private Attributes getMainAttributes() {
		return getManifest().getMainAttributes();
	}

	/**
	 * @return
	 */
	public Set<Entry<Object, Object>> getMainAttributesEntrySet() {
		return getMainAttributes().entrySet();
	}

	/**
	 * @return
	 */
	public int getMainAttributesSize() {
		return getMainAttributes().size();
	}

	/**
	 * @return
	 */
	Manifest getManifest() {
		return pManifest;
	}

	/**
	 * @return
	 */
	public String getVersion() {

		return getMainAttributes().getValue(ATTRIBUTE_MF_VESRION);
	}

	/**
	 * @return
	 */
	public boolean hasIPojoAttribute() {
		return getMainAttributes().containsKey(new Attributes.Name(ATTRIBUTE_IPOJO_NAME));
	}

	/**
	 * @return
	 */
	public boolean isIPojoAttributeLastOne() {
		return getIPojoAttributeRank() + 1 == getMainAttributes().size();
	}

	/**
	 * @param aManifest
	 * @return
	 * @throws Exception
	 */
	public boolean isIPojoAttributeSameAsIn(final Manifest aManifest) throws Exception {

		return isIPojoAttributeSameAsIn(new ManifestManipulator(aManifest));
	}

	/**
	 * @param aManifestManipulator
	 * @return
	 * @throws Exception
	 */
	public boolean isIPojoAttributeSameAsIn(final ManifestManipulator aManifestManipulator) throws Exception {

		String wIPojoAttributeA = getIPojoAttributeValue();

		String wIPojoAttributeB = aManifestManipulator.getIPojoAttributeValue();

		if (wIPojoAttributeA == null && wIPojoAttributeB == null) {
			return true;
		}
		if (wIPojoAttributeA == null && wIPojoAttributeB != null) {
			return false;
		}
		if (wIPojoAttributeA != null && wIPojoAttributeB == null) {
			return false;
		}

		return wIPojoAttributeA.equals(wIPojoAttributeB);
	}

	/**
	 * @param aManifestManipulator
	 * @return
	 */
	public boolean replaceIPojoAttribute(final ManifestManipulator aManifestManipulator) {
		return replaceIPojoAttribute(aManifestManipulator.getIPojoAttributeValue());
	}

	/**
	 * @param aIPojoAttribute
	 * @return
	 */
	public boolean replaceIPojoAttribute(final String aIPojoAttribute) {

		getMainAttributes().remove(new Attributes.Name(ATTRIBUTE_IPOJO_NAME));

		return appendIPojoAttribute(aIPojoAttribute);
	}

	/**
	 * @return
	 * @throws IOException
	 */
	public byte[] toBytes() throws IOException {

		// create temporary bayte array output stream
		ByteArrayOutputStream wOutputStream = new ByteArrayOutputStream();
		getManifest().write(wOutputStream);
		return wOutputStream.toByteArray();
	}

	@Override
	public String toString() {
		try {
			return new String(toBytes(), StandardCharsets.UTF_8);
		} catch (IOException e) {
			return eStackInString(e);
		}
	}
}
