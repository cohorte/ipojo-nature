package org.ow2.chameleon.eclipse.ipojo.core;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.jar.Attributes;
import java.util.jar.Attributes.Name;
import java.util.jar.Manifest;

/**
 * 
 * Evolution for java17: Using of the ManifestReformator class rather than the
 * replacing of the property "entries" in the Manifest instance (HashMap to
 * TreeMap) using the "setAccessible()" method of the java reflexion tooling.
 * 
 * How :
 * 
 * The ManifestReformator parse the content of the MANIFEST.MF files to extract
 * the attributes to store them in the Map "OrderedAttributes" wich is a
 * TreeMap.
 * 
 * Each attribute is considered as a couple of an Id and a bunch of bytes
 * without any consideration of their meening and their format
 * 
 * The formting of the attributes as a set of sized lines is always the
 * reponsability of the java Manifest class (since 1.4)
 * 
 * 
 * @author ogattaz
 *
 */
public class SortedManifestStreamer {

	/**
	 * @author ogattaz
	 *
	 */
	public class Attribute implements Comparable<Attribute> {

		byte[] pContent;
		final String pId;

		/**
		 * @param aId
		 * @param aContent
		 */
		Attribute(final byte[] aContent) {
			super();
			pId = extractId(aContent);
			pContent = aContent;
		}

		/**
		 * @param aId
		 * @param aValue
		 * @throws IOException
		 */
		Attribute(final String aId, final String aValue) throws IOException {
			this(toAttributeContent(aId, aValue));
		}

		/**
		 * @param aContent
		 * @return
		 * @throws IOException
		 */
		int appendContent(final byte[] aContent) throws IOException {

			validBuffer(aContent, "appendContent");

			if (aContent[0] != ' ') {
				throw new RuntimeException(String.format(
						"Unable to add a content of the attribute [%s] the new content doesn't start with a space character",
						getId()));
			}

			byte[] wNewContent = new byte[pContent.length + aContent.length];
			// previous content
			System.arraycopy(pContent, 0, wNewContent, 0, pContent.length);
			// appended content
			System.arraycopy(aContent, 0, wNewContent, pContent.length, aContent.length);

			pContent = wNewContent;

			return aContent.length;
		}

		/**
		 *
		 */
		@Override
		public int compareTo(Attribute o) {
			return pId.compareTo(o.pId);
		}

		/**
		 *
		 */
		@Override
		public boolean equals(Object aObject) {
			if (aObject instanceof Attribute) {
				return Arrays.equals(pContent, ((Attribute) aObject).pContent);
			} else {
				return super.equals(aObject);
			}
		}

		/**
		 * @param aContent
		 * @return
		 */
		private String extractId(final byte[] aContent) {

			int wPos = indexOfBytes(aContent, ATTRIBUTE_ID_SEPARATOR_BYTES);

			if (wPos == -1) {
				throw new RuntimeException(
						"Unable to find the attribute id separator in the byte buffer of the attribute");
			}
			return new String(aContent, 0, wPos, StandardCharsets.UTF_8);
		}

		/**
		 * @return
		 */
		public byte[] getContent() {
			return pContent;
		}

		/**
		 * @return the if of the attribute
		 */
		public String getId() {
			return pId;
		}

		/**
		 * @return the Name of the attribute
		 */
		public Name getName() {
			return new Name(getId());
		}

		/**
		 * @return
		 */
		public String getStringValue() {
			return new String(getValue(getContent()), StandardCharsets.UTF_8).replace(LINE_BREAK, "");
		}

		/**
		 * @param aContent
		 * @return
		 */
		public byte[] getValue(final byte[] aContent) {

			int wPosSeparator = indexOfBytes(aContent, ATTRIBUTE_ID_SEPARATOR_BYTES);

			if (wPosSeparator == -1) {
				throw new RuntimeException(
						"Unable to find the attribute id separator in the byte buffer of the attribute");
			}
			int wPosValue = wPosSeparator + ATTRIBUTE_ID_SEPARATOR_BYTES.length;
			int wValueLen = aContent.length - wPosValue;
			byte[] wValue = new byte[wValueLen];
			System.arraycopy(aContent, wPosValue, wValue, 0, wValueLen);
			return wValue;
		}

		/**
		 * @param aContent
		 */
		int setContent(final byte[] aContent) throws IOException {

			validBuffer(aContent, "setContent");

			String wId = extractId(aContent);

			if (!getId().equals(wId)) {
				throw new IOException(String.format(
						"Unable to modify the content of the attribute [%s] the new content contains an other ne : [%s]",
						getId(), wId));
			}
			pContent = aContent;

			return aContent.length;
		}

		/**
		 *
		 */
		@Override
		public String toString() {

			try {
				// create temporary bayte array output stream
				ByteArrayOutputStream wOutputStream = new ByteArrayOutputStream();

				this.write(wOutputStream);

				return new String(wOutputStream.toByteArray(), StandardCharsets.UTF_8).replace(LINE_BREAK, "");

			} catch (IOException e) {
				return dumpStackTrace(e);
			}
		}

		/**
		 * @param aBuffer
		 * @param aMethodName
		 * @throws IOException
		 */
		private void validBuffer(final byte[] aBuffer, final String aMethodName) throws IOException {
			if (aBuffer == null) {
				throw new IOException(String.format("the buffer given to the method [%s] is null", aMethodName));
			}
			if (aBuffer.length < 2) {
				throw new IOException(String.format("the buffer given to the method [%s] is too short", aMethodName));
			}
		}

		/**
		 * @param aOutputStream
		 * @return
		 * @throws IOException
		 */
		int write(final OutputStream aOutputStream) throws IOException {

			aOutputStream.write(getContent());

			return getContent().length;
		}
	}

	/**
	 * Simplified reader.
	 * 
	 * As the manifest files are never too big to be put in memory (!...) all the
	 * bytes are read at first.
	 * 
	 * Manifest Specification:
	 * 
	 * <pre>
		manifest-file:	main-section newline *individual-section
		main-section:	version-info newline *main-attribute
		version-info:	Manifest-Version : version-number
		version-number:	digit+{.digit+}*
		main-attribute:	(any legitimate main attribute) newline
		individual-section:	Name : value newline *perentry-attribute
		perentry-attribute:	(any legitimate perentry attribute) newline
		newline:	CR LF | LF | CR (not followed by LF)
		digit:	{0-9}
	 * </pre>
	 * 
	 * @see https://docs.oracle.com/en/java/javase/17/docs/specs/jar/jar.html
	 * 
	 * @author ogattaz
	 *
	 */
	class ManifestReader {

		private final InputStream pInputStream;
		private int pMax = 0;
		private byte[] pMf = null;
		private int pMfLen = 0;
		private int pPos = 0;

		/**
		 * @param aInputStream
		 * @throws IOException
		 */
		ManifestReader(final InputStream aInputStream) throws IOException {
			super();

			pInputStream = aInputStream;
		}

		/**
		 * @param wLine
		 */
		void dumpLine(byte[] wLine) {
			int wMax = wLine.length;
			StringBuilder wSB = new StringBuilder();
			wSB.append('[');
			for (int wIdx = 0; wIdx < wMax; wIdx++) {
				byte wByte = wLine[wIdx];
				if (wByte == ' ') {
					wSB.append(".");
				} else if (wByte == '\n') {
					wSB.append("\\n");
				} else if (wByte == '\r') {
					wSB.append("\\r");
				} else {
					wSB.append((char) wByte);
				}
			}
			wSB.append(']');
			System.out.println(wSB.toString());
		}

		/**
		 * @param aBuffer
		 * @return the index of the last LF | CR found in the given buffer
		 * @throws IOException
		 */
		private int findLastCrOrLf(final byte[] aBuffer) throws IOException {

			for (int wIdx = aBuffer.length - 1; wIdx > 0; wIdx--) {
				if (aBuffer[wIdx] == '\r' || aBuffer[wIdx] == '\n') {
					return wIdx;
				}
			}
			throw new IOException("no CR or LF in the given buffer");
		}

		/**
		 * @param aLine
		 * @return
		 */
		boolean isEndLine(final byte[] aLine) {
			return aLine != null && aLine.length <= 2 && (aLine[0] == '\r' || aLine[0] == '\n');
		}

		/**
		 * @param aLine
		 * @return
		 */
		boolean isLineAttribute(final byte[] aLine) {

			return aLine != null && aLine.length > 2 && aLine[0] != ' ';
		}

		/**
		 * @return
		 */
		OrderedAttributes read() throws IOException {
			OrderedAttributes wSortedAttributes = new OrderedAttributes();

			pPos = 0;
			boolean wContinue = true;
			Attribute wAttribute = null;
			while (wContinue) {

				if (pPos == pMax) {
					readInStream();
				}

				byte[] wLine = readOneLine();

				wContinue = !isEndLine(wLine);

				if (wContinue) {
					if (isLineAttribute(wLine)) {
						wAttribute = new Attribute(wLine);
						wSortedAttributes.put(wAttribute.getName(), wAttribute);
					} else {
						if (wAttribute == null) {
							throw new IOException(
									"wrong manifest content, a follow line appeared before an attribute line");
						}
						wAttribute.appendContent(wLine);
					}
				}
			}
			return wSortedAttributes;
		}

		/**
		 * read up to 8192 bytes in the inpustream
		 * 
		 * @return the number of bytes set or added in pMf
		 * @throws IOException
		 */
		private int readInStream() throws IOException {

			byte[] wBuffer = pInputStream.readNBytes(8192);

			//
			if (pMf != null && pMax < pMfLen) {
				int wRemainder = pMfLen - pMax;
				byte[] wSigmaBuffer = new byte[wBuffer.length + wRemainder];
				// previous content
				System.arraycopy(pMf, pMax, wSigmaBuffer, 0, wRemainder);
				// appended content
				System.arraycopy(wBuffer, 0, wSigmaBuffer, wRemainder, wBuffer.length);
				pMf = wSigmaBuffer;
			} else {
				pMf = wBuffer;

			}
			pMfLen = pMf.length;
			// search last LF | CR
			pMax = findLastCrOrLf(pMf) + 1;
			pPos = 0;
			return wBuffer.length;
		}

		/**
		 * 
		 * Manifest Specification:
		 * 
		 * newline: CR LF | LF | CR (not followed by LF)
		 * 
		 * @return
		 * @see https://docs.oracle.com/en/java/javase/17/docs/specs/jar/jar.html
		 */
		byte[] readOneLine() {
			byte wByte = 0;
			int wPos = pPos;
			int wPosStart = wPos;
			while (pPos < pMax) {
				wPos++;
				wByte = pMf[wPos];
				// LF | CR
				if (wByte == '\n' || wByte == '\r') {
					break;
				}
			}
			// CR LF
			if (wByte == '\r' && wPos < pMax && pMf[wPos + 1] == '\n') {
				wPos++;
			}

			int wLen = wPos - wPosStart + 1;
			byte[] wLine = new byte[wLen];
			System.arraycopy(pMf, wPosStart, wLine, 0, wLen);

			// debug => dumpLine(wLine);

			// for the next line
			pPos = wPos + 1;

			return wLine;
		}
	}

	/**
	 * @author ogattaz
	 *
	 */
	public class NameComparator implements Comparator<Name> {

		/**
		 *
		 */
		@Override
		public int compare(Name aName1, Name aName2) {

			return aName1.toString().compareTo(aName2.toString());
		}
	}

	/**
	 * @author ogattaz
	 *
	 */
	public class OrderedAttributes extends TreeMap<Name, Attribute> {

		private static final long serialVersionUID = 394914480441542464L;

		/**
		 * 
		 */
		OrderedAttributes() {
			// to sort the names which are not comparable !
			super(new NameComparator());
		}

		/**
		 * @param aKey
		 * @return
		 */
		boolean containsKey(final String aKey) {
			return super.containsKey(new Attributes.Name(aKey));
		}

		/**
		 * @param aKey
		 * @return
		 */
		Attribute get(final String aKey) {
			return super.get(new Name(aKey));
		}

		List<Attribute> getOrderedAttributes() {

			List<Attribute> wList = new ArrayList<>();
			// the firt
			wList.add(get(ATTRIBUTE_ID_MFVERSION));

			// all the others "name id" sorted by the TreeMap
			for (Entry<Name, Attribute> wEntry : this.entrySet()) {

				// if the "name id" is not equal to MFVERSION
				if (!ATTRIBUTE_ID_MFVERSION.equals(wEntry.getKey().toString())) {
					wList.add(wEntry.getValue());
				}
			}
			return wList;
		}

		/**
		 * @param aKey
		 * @return
		 */
		String getStringValue(final String aKey) {

			return get(new Name(aKey)).getStringValue();
		}

		/**
		 * @param aKey
		 * @return
		 */
		Attribute remove(final String aKey) {
			return super.remove(new Attributes.Name(ATTRIBUTE_IPOJO_NAME));
		}

		/**
		 * @param aAttribute
		 * @return
		 * @throws IOException
		 */
		boolean replace(final Attribute aAttribute) throws IOException {

			Attribute wExistingAttribute = get(aAttribute.getName());

			if (wExistingAttribute == null) {
				throw new IOException(String.format("Unable to replace attribute [%s], it doesn't exist in the Map",
						aAttribute.getId()));
			}
			wExistingAttribute.setContent(aAttribute.getContent());

			return true;
		}

		/**
		 * @param aOutputStream
		 * @return
		 * @throws IOException
		 */
		int write(final OutputStream aOutputStream) throws IOException {
			int wSize = 0;

			// get the ordrer list Attribute

			List<Attribute> wOrderedAttributes = getOrderedAttributes();

			for (Attribute wAttribute : wOrderedAttributes) {
				wSize += wAttribute.write(aOutputStream);
			}
			return wSize;
		}
	}

	public static final String ATTRIBUTE_ID_MFVERSION = "Manifest-Version";
	public static final byte[] ATTRIBUTE_ID_MFVERSION_BYTES = ATTRIBUTE_ID_MFVERSION.getBytes();

	public static final String ATTRIBUTE_ID_SEPARATOR = ": ";
	public static final byte[] ATTRIBUTE_ID_SEPARATOR_BYTES = ATTRIBUTE_ID_SEPARATOR.getBytes();

	public static final String ATTRIBUTE_IMPORT_PACKAGE = "Import-Package";

	public static final String ATTRIBUTE_IPOJO_NAME = "iPOJO-Components";

	public static final String LINE_BREAK = "\r\n";
	public static final String LINE_BREAK_AND_FOLLOW = "\r\n ";

	public static final byte[] LINE_BREAK_AND_FOLLOW_BYTES = LINE_BREAK_AND_FOLLOW.getBytes();
	public static final byte[] LINE_BREAK_BYTES = LINE_BREAK.getBytes();

	private static final String MANIFEST_MINIMAL = "Manifest-Version: 1.0\r\n\r\n";

	public static final String MANIFEST_VERSION_10 = "1.0";
	public static final byte[] MANIFEST_VERSION_10_BYTES = MANIFEST_VERSION_10.getBytes();

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
	private static String dumpStackTrace(Throwable aThrowable) {
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
	private static InputStream manifestMinimalStream() throws IOException {

		// create input stream from baos
		return new ByteArrayInputStream(MANIFEST_MINIMAL.getBytes(StandardCharsets.UTF_8));
	}

	/**
	 * @param aManifest
	 * @return
	 * @throws Exception
	 */
	private static InputStream manifestToStream(final Manifest aManifest) throws IOException {

		// create temporary bayte array output stream
		ByteArrayOutputStream wOutputStream = new ByteArrayOutputStream();

		aManifest.write(wOutputStream);

		// create input stream from baos
		InputStream isFromFirstData = new ByteArrayInputStream(wOutputStream.toByteArray());

		return isFromFirstData;
	}

	/**
	 * @param aInputStream
	 * @return
	 * @throws Exception
	 */
	public static Manifest newManifest(final InputStream aInputStream) throws IOException {
		try {
			return new Manifest(aInputStream);
		} catch (Exception e) {
			throw new IOException(String.format("Unable to instanciate ManifestManipulator using the InputStream: %s",
					dumpInputStreamInfos(aInputStream)), e);
		}
	}

	/**
	 * @return
	 * @throws Exception
	 */
	public static Manifest newMinimalManifest() throws IOException {
		return newManifest(manifestMinimalStream());
	}

	/**
	 * @param aId
	 * @param aValue
	 * @return
	 * @throws IOException
	 */
	private static byte[] toAttributeContent(final String aId, final String aValue) throws IOException {

		Manifest wManifest = newMinimalManifest();

		wManifest.getMainAttributes().put(new Name(aId), aValue);

		SortedManifestStreamer wManifestReformator = new SortedManifestStreamer(wManifest);

		Attribute wNewAttribute = wManifestReformator.getMainAttributes().get(aId);

		return wNewAttribute.getContent();
	}

	// The map of Arribute : private member of ManifestReformator
	private final OrderedAttributes pSortedAttributes;

	/**
	 * @throws Exception
	 */
	public SortedManifestStreamer() throws Exception {
		this(manifestMinimalStream());
	}

	/**
	 * @param aName
	 * @param aResourcePath
	 * @param aBuffer
	 * @throws IOException
	 */
	public SortedManifestStreamer(final InputStream aInputStream) throws IOException {
		super();
		pSortedAttributes = new ManifestReader(aInputStream).read();
	}

	/**
	 * @param aManifest
	 * @throws Exception
	 */
	public SortedManifestStreamer(final Manifest aManifest) throws IOException {
		this(manifestToStream(aManifest));
	}

	/**
	 * @param aIPojoAttribute
	 * @return
	 */
	public boolean appendIPojoAttribute(final Attribute aIPojoAttribute) {

		Object wPrevious = getMainAttributes().putIfAbsent(new Attributes.Name(ATTRIBUTE_IPOJO_NAME), aIPojoAttribute);

		return wPrevious == null;
	}

	/**
	 * @param string
	 * @throws IOException
	 */
	public boolean appendIPojoAttribute(final String aValue) throws IOException {
		return appendIPojoAttribute(new Attribute(ATTRIBUTE_IPOJO_NAME, aValue));
	}

	/**
	 * @return
	 */
	public Attribute getImportPackageAttribute() {

		return getMainAttributes().get(ATTRIBUTE_IMPORT_PACKAGE);
	}

	/**
	 * 
	 * 
	 * /**
	 * 
	 * @return
	 */
	public Attribute getIPojoAttribute() {

		return getMainAttributes().get(ATTRIBUTE_IPOJO_NAME);
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
	private OrderedAttributes getMainAttributes() {
		return pSortedAttributes;
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
	public List<Attribute> getOrderedMainAttributes() {
		return getMainAttributes().getOrderedAttributes();
	}

	/**
	 * @return
	 */
	public String getVersion() {

		return getMainAttributes().getStringValue(ATTRIBUTE_ID_MFVERSION);
	}

	/**
	 * @return
	 */
	public boolean hasIPojoAttribute() {
		return getMainAttributes().containsKey(new Attributes.Name(ATTRIBUTE_IPOJO_NAME));
	}

	/**
	 * @param aBuffer
	 * @param aPattern
	 * @return
	 */
	private int indexOfBytes(byte[] aBuffer, byte[] aPattern) {
		int wMax = aBuffer.length - aPattern.length + 1;
		for (int wPos = 0; wPos < wMax; ++wPos) {
			// hypthesis
			boolean found = true;
			// search the stream bytes of the pattern
			for (int wIdx = 0; wIdx < aPattern.length; ++wIdx) {
				if (aBuffer[wPos + wIdx] != aPattern[wIdx]) {
					found = false;
					break;
				}
			}
			if (found) {
				return wPos;
			}
		}
		return -1;
	}

	/**
	 * "Import-Package"
	 * 
	 * @param aManifestManipulator
	 * @return
	 * @throws Exception
	 */
	public boolean isImportPackageAttributeSameAsIn(final SortedManifestStreamer aManifestManipulator) throws Exception {

		Attribute wImportPackageAttributeA = getImportPackageAttribute();

		Attribute wImportPackageAttributeB = aManifestManipulator.getImportPackageAttribute();

		if (wImportPackageAttributeA == null && wImportPackageAttributeB == null) {
			return true;
		}
		if (wImportPackageAttributeA == null && wImportPackageAttributeB != null) {
			return false;
		}
		if (wImportPackageAttributeA != null && wImportPackageAttributeB == null) {
			return false;
		}

		return wImportPackageAttributeA.equals(wImportPackageAttributeB);
	}

	/**
	 * @return
	 */
	public boolean isIPojoAttributeLastOne() {
		return getIPojoAttributeRank() + 1 == getMainAttributes().size();
	}

	/**
	 * "iPOJO-Components"
	 * 
	 * @param aManifestManipulator
	 * @return
	 * @throws Exception
	 */
	public boolean isIPojoAttributeSameAsIn(final SortedManifestStreamer aManifestManipulator) throws Exception {

		Attribute wIPojoAttributeA = getIPojoAttribute();

		Attribute wIPojoAttributeB = aManifestManipulator.getIPojoAttribute();

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
	 * @param aManifest
	 * @return
	 * @throws Exception
	 */
	public boolean isIPojoAttributesSameAsIn(final Manifest aManifest) throws Exception {
		return isIPojoAttributesSameAsIn(new SortedManifestStreamer(aManifest));

	}

	/**
	 * @param aManipulator
	 * @return
	 * @throws Exception
	 */
	public boolean isIPojoAttributesSameAsIn(final SortedManifestStreamer aManipulator) throws Exception {

		return isIPojoAttributeSameAsIn(aManipulator) && isImportPackageAttributeSameAsIn(aManipulator);
	}

	/**
	 * @return
	 */
	public Attribute removeIPojoAttribute() {

		return getMainAttributes().remove(ATTRIBUTE_IPOJO_NAME);
	}

	/**
	 * @param aIPojoAttribute
	 * @return
	 */
	public boolean replaceIPojoAttribute(final Attribute aIPojoAttribute) throws IOException {

		return getMainAttributes().replace(aIPojoAttribute);
	}

	/**
	 * @param aManifestManipulator
	 * @return
	 */
	public boolean replaceIPojoAttribute(final SortedManifestStreamer aManifestReformator) throws IOException {

		return replaceIPojoAttribute(aManifestReformator.getIPojoAttribute());
	}

	/**
	 * @param string
	 * @throws IOException
	 */
	public boolean replaceIPojoAttribute(final String aValue) throws IOException {
		return replaceIPojoAttribute(new Attribute(ATTRIBUTE_IPOJO_NAME, aValue));
	}

	/**
	 * @return
	 * @throws IOException
	 */
	public byte[] toBytes() throws IOException {

		// create temporary bayte array output stream
		ByteArrayOutputStream wOutputStream = new ByteArrayOutputStream();
		write(wOutputStream);
		return wOutputStream.toByteArray();
	}

	@Override
	public String toString() {
		try {
			return new String(toBytes(), StandardCharsets.UTF_8);
		} catch (IOException e) {
			return dumpStackTrace(e);
		}
	}

	/**
	 * @param awOutputStream
	 * @return
	 * @throws IOException
	 */
	public int write(final OutputStream aOutputStream) throws IOException {

		return getMainAttributes().write(aOutputStream);
	}

}
