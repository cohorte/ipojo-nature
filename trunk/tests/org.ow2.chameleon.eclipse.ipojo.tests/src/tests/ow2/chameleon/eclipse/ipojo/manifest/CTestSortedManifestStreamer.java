package tests.ow2.chameleon.eclipse.ipojo.manifest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static tech.cohorte.pico.tooling.CCTLoggerUtils.logBanner;
import static tech.cohorte.pico.tooling.CCTLoggerUtils.logInfo;
import static tech.cohorte.pico.tooling.CCTLoggerUtils.logInfoBegin;
import static tech.cohorte.pico.tooling.CCTLoggerUtils.logInfoEnd;
import static tech.cohorte.pico.tooling.CCTLoggerUtils.logSevere;
import static tech.cohorte.pico.tooling.CCTMethodUtils.getMethodName;
import static tech.cohorte.pico.tooling.CCTStringUtils.truncatedToString;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.ow2.chameleon.eclipse.ipojo.core.SortedManifestStreamer;
import org.ow2.chameleon.eclipse.ipojo.core.SortedManifestStreamer.Attribute;

import tech.cohorte.pico.tooling.CCTExceptionUtils;
import tech.cohorte.pico.tooling.CCTJulUtils;
import tech.cohorte.pico.tooling.CCTLoggerUtils;
import tech.cohorte.pico.tooling.CCTTimer;

/**
 * @author ogattaz
 *
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class CTestSortedManifestStreamer {

	private static final String ARGUMENT_FULL_RESOURCE_PATH = "fullResourcePath";

	private static final String ARGUMENT_RESOURCE_NAME = "resourceName";

	private static final String ARGUMENT_RESOURCE_PATH = "resourcePath";

	private static final int sNbTest = countNbTest(CTestSortedManifestStreamer.class);

	private static final AtomicInteger sSuccessCounter = new AtomicInteger(0);

	private static final AtomicInteger sTestCounter = new AtomicInteger(0);

	private static CCTTimer sTimer = null;

	private static final String TESTNAME = CTestSortedManifestStreamer.class.getSimpleName();

	/**
	 * @param aTestClass
	 * @return the number of method having the annotation @Test
	 */
	private static int countNbTest(final Class<?> aTestClass) {

		int wNbTest = 0;
		for (Method wMethod : aTestClass.getMethods()) {
			Test wTestAnnotation = wMethod.getAnnotation(Test.class);
			if (wTestAnnotation != null) {
				wNbTest++;
			}
		}
		return wNbTest;
	}

	/**
	 *
	 */
	@AfterClass
	public static void destroy() throws Exception {
		String wMethod = getMethodName(1);

		logBanner(CTestSortedManifestStreamer.class, wMethod, Level.INFO,
				"Test of [%s] done. Success=[%d/%d] duration=[%s]", TESTNAME, sSuccessCounter.get(), sNbTest,
				sTimer.getDurationStrMicroSec());
	}

	/**
	 *
	 */
	@BeforeClass
	public static void initialize() throws Exception {
		String wMethod = getMethodName(1);

		sTimer = CCTTimer.newStartedTimer();

		logBanner(CTestSortedManifestStreamer.class, wMethod, Level.INFO, "Tests of [%s] Begin. NbTest=[%d]", TESTNAME,
				sNbTest);

		// dump the current logger
		logInfo(CTestSortedManifestStreamer.class, wMethod, "%s",
				//
				CCTJulUtils.dumpCurrentLogger(CCTLoggerUtils.getCurrentLogger()));

	}

	/**
	 * @param aFullResourcePath
	 * @return
	 * @throws Exception
	 */
	private static byte[] readResourceBytes(final Path aFullResourcePath) throws Exception {

		Path wFullResourcePath = (Path) validNotNull(aFullResourcePath, ARGUMENT_FULL_RESOURCE_PATH);

		URL wResourceUrl = SortedManifestStreamer.class.getResource(wFullResourcePath.toString());

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
	private static byte[] readResourceBytes(final String aSubPackage, final String aResourceName) throws Exception {

		String wResourcePath = "/".concat(CTestSortedManifestStreamer.class.getPackage().getName().replace('.', '/'))
				.concat(aSubPackage);

		Path wFullResourcePath = Paths.get(
				//
				validNotNullAndNotEmpty(wResourcePath, ARGUMENT_RESOURCE_PATH),
				//
				validNotNullAndNotEmpty(aResourceName, ARGUMENT_RESOURCE_NAME));

		return readResourceBytes(wFullResourcePath);
	}

	/**
	 * @param aValue
	 * @param aInfo
	 * @return
	 * @throws Exception
	 */
	private static Object validNotNull(final Object aValue, final String aInfo) throws Exception {
		if (aValue == null) {
			throw new IllegalArgumentException(String.format("The given [%s] is null", aInfo));
		}
		return aValue;
	}

	/**
	 * @param aValue
	 * @param aInfo
	 * @return
	 */
	private static String validNotNullAndNotEmpty(final String aValue, final String aInfo) throws Exception {

		String wValue = (String) validNotNull(aValue, aInfo);

		if (wValue.isEmpty()) {
			throw new IllegalArgumentException(String.format("The given [%s] is empty", aInfo));
		}
		if (wValue.isBlank()) {
			throw new IllegalArgumentException(String.format("The given [%s] is blank", aInfo));
		}
		return wValue;
	}

	/**
	 *
	 */
	public CTestSortedManifestStreamer() {
		super();
	}

	/**
	 * @param aMainAttributes
	 */
	private void dumpAttributes(final SortedManifestStreamer aManifestReformator) {
		String wMethod = getMethodName(1);
		int wSize = aManifestReformator.getMainAttributesSize();
		int wIdxA = 0;
		for (Attribute wAttribute : aManifestReformator.getOrderedMainAttributes()) {
			wIdxA++;
			String wValue = wAttribute.getStringValue();
			int wValueSize = wValue.length();
			logInfo(this, wMethod, "Attribute(%2d/%2d)=[%-36s]=[%6d][%s]", wIdxA, wSize, wAttribute.getId(), wValueSize,
					truncatedToString(wValue, 128));
		}
	}

	/**
	 * @param aSubPackage
	 * @param aResourceName
	 * @return
	 * @throws Exception
	 */
	private SortedManifestStreamer newFromResource(final String aSubPackage, final String aResourceName)
			throws Exception {
		String wMethod = getMethodName(1);

		byte[] wManifestBytes = readResourceBytes(aSubPackage, aResourceName);

		InputStream wManifestDataStream = new ByteArrayInputStream(wManifestBytes);

		// instanciate
		SortedManifestStreamer wSortedManifestStreamer = new SortedManifestStreamer(wManifestDataStream);
		logInfo(this, wMethod, "Manifest.version=[%s]", wSortedManifestStreamer.getVersion());

		return wSortedManifestStreamer;
	}

	/**
	 * @param aResourceName
	 * @return
	 * @throws Exception
	 */
	private byte[] readResourceBytes(final String aResourceName) throws Exception {
		return readResourceBytes("", aResourceName);
	}

	/**
	 *
	 */
	@Test
	public void test05LoadManifest() throws Exception {
		String wMethod = getMethodName(1);
		CCTTimer wTimer = CCTTimer.newStartedTimer();

		logInfoBegin(this, wMethod, "test=[%d/%d)", sTestCounter.incrementAndGet(), sNbTest);

		try {

			// load the manifest "a"
			SortedManifestStreamer wSortedManifestStreamer = newFromResource("/a", "MANIFEST.MF");
			String wVersion = wSortedManifestStreamer.getVersion();
			// version 1.0
			assertEquals(SortedManifestStreamer.MANIFEST_VERSION_10, wVersion);
			logInfo(this, wMethod, "Manifest.version=[%s] >>>  assert equals 1.0 OK", wVersion);

			final int wAttributesSize = wSortedManifestStreamer.getMainAttributesSize();
			logInfo(this, wMethod, "Manifest.MainAttributes.size=[%d]", wAttributesSize);

			dumpAttributes(wSortedManifestStreamer);

			boolean wHasIPojoAttribute = wSortedManifestStreamer.hasIPojoAttribute();
			// yes there is an iPojoAttribute
			assertTrue(wHasIPojoAttribute);
			logInfo(this, wMethod, "Manifest.hasIPojoAttribute=[%b] >>> assert true OK", wHasIPojoAttribute);

			String wIPojoAttributeValue = wSortedManifestStreamer.getIPojoAttribute().getStringValue();
			logInfo(this, wMethod, "Manifest.IPojoAttribute=[%s]", truncatedToString(wIPojoAttributeValue, 256));

			logInfo(this, wMethod, "Done. Success=[%d/%d] duration=[%s]", sSuccessCounter.incrementAndGet(), sNbTest,
					wTimer.getDurationStrMicroSec());
		}
		//
		catch (final Throwable e) {
			logSevere(this, wMethod, "UNEXPECTED ERROR: %s", CCTExceptionUtils.eCauseMessagesInString(e));
			throw e;
		}
		//
		finally {
			logInfoEnd(this, wMethod);
		}
	}

	/**
	 *
	 */
	@Test
	public void test10NewEmpty() throws Exception {
		String wMethod = getMethodName(1);
		CCTTimer wTimer = CCTTimer.newStartedTimer();

		logInfoBegin(this, wMethod, "test=[%d/%d)", sTestCounter.incrementAndGet(), sNbTest);

		try {
			// load the manifest "a"
			SortedManifestStreamer wSortedManifestStreamerA = newFromResource("/a", "MANIFEST.MF");
			logInfo(this, wMethod, "ReformatorA.IPojoAttribute.rank=[%d]",
					wSortedManifestStreamerA.getIPojoAttributeRank());
			logInfo(this, wMethod, "ReformatorA.IPojoAttribute.last=[%b]",
					wSortedManifestStreamerA.isIPojoAttributeLastOne());

			// new empty manifest
			SortedManifestStreamer wSortedManifestStreamer = new SortedManifestStreamer();
			String wVersion = wSortedManifestStreamer.getVersion();
			// versin 1.0 in the epty manifest
			assertEquals(SortedManifestStreamer.MANIFEST_VERSION_10, wVersion);
			logInfo(this, wMethod, "Manifest.version=[%s]", wVersion);

			final int wAttributesSize = wSortedManifestStreamer.getMainAttributesSize();
			// only one attribute in the empty manifest
			assertEquals(1, wAttributesSize);
			logInfo(this, wMethod, "Manifest.MainAttributes.size=[%d] >>> assert equals 1 OK", wAttributesSize);

			logInfo(this, wMethod, "Done. Success=[%d/%d] duration=[%s]", sSuccessCounter.incrementAndGet(), sNbTest,
					wTimer.getDurationStrMicroSec());
		}
		//
		catch (final Throwable e) {
			logSevere(this, wMethod, "UNEXPECTED ERROR: %s", CCTExceptionUtils.eCauseMessagesInString(e));
			throw e;
		}
		//
		finally {
			logInfoEnd(this, wMethod);
		}
	}

	/**
	 *
	 */
	@Test
	public void test15CompareIPojoAttribute() throws Exception {
		String wMethod = getMethodName(1);
		CCTTimer wTimer = CCTTimer.newStartedTimer();

		logInfoBegin(this, wMethod, "test=[%d/%d)", sTestCounter.incrementAndGet(), sNbTest);

		try {
			// load the manifest "a"
			SortedManifestStreamer wSortedManifestStreamerA = newFromResource("/a", "MANIFEST.MF");
			logInfo(this, wMethod, "ReformatorA.IPojoAttribute.rank=[%d]",
					wSortedManifestStreamerA.getIPojoAttributeRank());
			logInfo(this, wMethod, "ReformatorA.IPojoAttribute.last=[%b]",
					wSortedManifestStreamerA.isIPojoAttributeLastOne());
			// load the manifest "b"(with the same IPojo attribute)
			SortedManifestStreamer wSortedManifestStreamerB = newFromResource("/b", "MANIFEST.MF");
			logInfo(this, wMethod, "ReformatorB.IPojoAttribute.rank=[%d]",
					wSortedManifestStreamerB.getIPojoAttributeRank());
			logInfo(this, wMethod, "ReformatorB.IPojoAttribute.last=[%b]",
					wSortedManifestStreamerB.isIPojoAttributeLastOne());

			boolean wSame = wSortedManifestStreamerA.isIPojoAttributeSameAsIn(wSortedManifestStreamerB);
			// they are equal
			assertTrue(wSame);
			logInfo(this, wMethod, "isIPojoAttributeSameAsIn=[%b] >>> assert true OK", wSame);

			logInfo(this, wMethod, "Done. Success=[%d/%d] duration=[%s]", sSuccessCounter.incrementAndGet(), sNbTest,
					wTimer.getDurationStrMicroSec());
		}
		//
		catch (final Throwable e) {
			logSevere(this, wMethod, "UNEXPECTED ERROR: %s", CCTExceptionUtils.eCauseMessagesInString(e));
			throw e;
		}
		//
		finally {
			logInfoEnd(this, wMethod);
		}
	}

	/**
	 *
	 */
	@Test
	public void test20CompareIPojoAttribute() throws Exception {
		String wMethod = getMethodName(1);
		CCTTimer wTimer = CCTTimer.newStartedTimer();

		logInfoBegin(this, wMethod, "test=[%d/%d)", sTestCounter.incrementAndGet(), sNbTest);

		try {
			// load the manifest "a"
			SortedManifestStreamer wSortedManifestStreamerA = newFromResource("/a", "MANIFEST.MF");
			logInfo(this, wMethod, "ReformatorA.IPojoAttribute.rank=[%d]",
					wSortedManifestStreamerA.getIPojoAttributeRank());
			logInfo(this, wMethod, "ReformatorA.IPojoAttribute.last=[%b]",
					wSortedManifestStreamerA.isIPojoAttributeLastOne());
			// load the manifest "c" (without IPojo attribute)
			SortedManifestStreamer wSortedManifestStreamerC = newFromResource("/c", "MANIFEST.MF");
			logInfo(this, wMethod, "ReformatorC.IPojoAttribute.rank=[%d]",
					wSortedManifestStreamerC.getIPojoAttributeRank());
			logInfo(this, wMethod, "ReformatorC.IPojoAttribute.last=[%b]",
					wSortedManifestStreamerC.isIPojoAttributeLastOne());

			boolean wSame = wSortedManifestStreamerA.isIPojoAttributeSameAsIn(wSortedManifestStreamerC);
			// they are different
			assertFalse(wSame);
			logInfo(this, wMethod, "isIPojoAttributeSameAsIn=[%b] >>> assert false OK", wSame);

			logInfo(this, wMethod, "Done. Success=[%d/%d] duration=[%s]", sSuccessCounter.incrementAndGet(), sNbTest,
					wTimer.getDurationStrMicroSec());
		}
		//
		catch (final Throwable e) {
			logSevere(this, wMethod, "UNEXPECTED ERROR: %s", CCTExceptionUtils.eCauseMessagesInString(e));
			throw e;
		}
		//
		finally {
			logInfoEnd(this, wMethod);
		}
	}

	/**
	 *
	 */
	@Test
	public void test30AppendIPojoAttributeInC() throws Exception {
		String wMethod = getMethodName(1);
		CCTTimer wTimer = CCTTimer.newStartedTimer();

		logInfoBegin(this, wMethod, "test=[%d/%d)", sTestCounter.incrementAndGet(), sNbTest);

		try {
			// load the manifest "c" (without IPojo attribute)
			SortedManifestStreamer wSortedManifestStreamerC = newFromResource("/c", "MANIFEST.MF");
			logInfo(this, wMethod, "ReformatorC.IPojoAttribute.rank=[%d]",
					wSortedManifestStreamerC.getIPojoAttributeRank());
			logInfo(this, wMethod, "ReformatorC.IPojoAttribute.last=[%b]",
					wSortedManifestStreamerC.isIPojoAttributeLastOne());

			final int wAttributesSize = wSortedManifestStreamerC.getMainAttributesSize();
			//
			assertEquals(13, wAttributesSize);
			logInfo(this, wMethod, "Manifest.MainAttributes.size=[%d] >>> assert equals 13 OK ", wAttributesSize);

			// before
			dumpAttributes(wSortedManifestStreamerC);

			wSortedManifestStreamerC.appendIPojoAttribute("jhflqsjdfqlsdfjqlsdjfhlsjfdhj");

			final int wAttributesSizeAfter = wSortedManifestStreamerC.getMainAttributesSize();
			//
			assertEquals(14, wAttributesSizeAfter);
			logInfo(this, wMethod, "Manifest.MainAttributes.size=[%d] >>> assert equals 14 OK ", wAttributesSizeAfter);

			logInfo(this, wMethod, "ReformatorC.IPojoAttribute.rank=[%d]",
					wSortedManifestStreamerC.getIPojoAttributeRank());
			logInfo(this, wMethod, "ReformatorC.IPojoAttribute.last=[%b]",
					wSortedManifestStreamerC.isIPojoAttributeLastOne());

			// after
			dumpAttributes(wSortedManifestStreamerC);

			logInfo(this, wMethod, "Done. Success=[%d/%d] duration=[%s]", sSuccessCounter.incrementAndGet(), sNbTest,
					wTimer.getDurationStrMicroSec());
		}
		//
		catch (final Throwable e) {
			logSevere(this, wMethod, "UNEXPECTED ERROR: %s", CCTExceptionUtils.eCauseMessagesInString(e));
			throw e;
		}
		//
		finally {
			logInfoEnd(this, wMethod);
		}
	}

	/**
	 *
	 */
	@Test
	public void test35ReplaceIPojoAttributeInA() throws Exception {
		String wMethod = getMethodName(1);
		CCTTimer wTimer = CCTTimer.newStartedTimer();

		logInfoBegin(this, wMethod, "test=[%d/%d)", sTestCounter.incrementAndGet(), sNbTest);

		try {

			// load the manifest "a"
			SortedManifestStreamer wSortedManifestStreamerA = newFromResource("/a", "MANIFEST.MF");
			logInfo(this, wMethod, "ReformatorA.IPojoAttribute.rank=[%d]",
					wSortedManifestStreamerA.getIPojoAttributeRank());
			logInfo(this, wMethod, "ReformatorA.IPojoAttribute.last=[%b]",
					wSortedManifestStreamerA.isIPojoAttributeLastOne());
			logInfo(this, wMethod, "ReformatorA.IPojoAttribute.len =[%d]",
					wSortedManifestStreamerA.getIPojoAttribute().getStringValue().length());

			// before
			dumpAttributes(wSortedManifestStreamerA);

			// load the pojoizationStream
			String wPojoIzationStream = new String(readResourceBytes("pojoizationStream.txt"));

			logInfo(this, wMethod, "pojoizationStream.size=[%d]", wPojoIzationStream.length());

			SortedManifestStreamer wSortedManifestStreamerZ = new SortedManifestStreamer();

			wSortedManifestStreamerZ.appendIPojoAttribute(wPojoIzationStream);

			logInfo(this, wMethod, "ReformatorZ.IPojoAttribute.rank=[%d]",
					wSortedManifestStreamerZ.getIPojoAttributeRank());
			logInfo(this, wMethod, "ReformatorZ.IPojoAttribute.last=[%b]",
					wSortedManifestStreamerZ.isIPojoAttributeLastOne());
			logInfo(this, wMethod, "ReformatorZ.IPojoAttribute.len =[%d]",
					wSortedManifestStreamerZ.getIPojoAttribute().getStringValue().length());

			boolean wSame = wSortedManifestStreamerA.isIPojoAttributeSameAsIn(wSortedManifestStreamerZ);
			// they are different
			assertFalse(wSame);
			logInfo(this, wMethod, "isIPojoAttributeSameAsIn=[%b] >>> assert false OK", wSame);

			if (!wSame) {
				wSortedManifestStreamerA.replaceIPojoAttribute(wSortedManifestStreamerZ);

				// after
				dumpAttributes(wSortedManifestStreamerA);

				logInfo(this, wMethod, "DUMP SortedManifestStreamer :\n%s", wSortedManifestStreamerA.toString());

			}

			logInfo(this, wMethod, "Done. Success=[%d/%d] duration=[%s]", sSuccessCounter.incrementAndGet(), sNbTest,
					wTimer.getDurationStrMicroSec());
		}
		//
		catch (final Throwable e) {
			logSevere(this, wMethod, "UNEXPECTED ERROR: %s", CCTExceptionUtils.eCauseMessagesInString(e));
			throw e;
		}
		//
		finally {
			logInfoEnd(this, wMethod);
		}
	}

}
