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
import org.ow2.chameleon.eclipse.ipojo.core.ManifestReformator;
import org.ow2.chameleon.eclipse.ipojo.core.ManifestReformator.Attribute;

import tech.cohorte.pico.tooling.CCTExceptionUtils;
import tech.cohorte.pico.tooling.CCTJulUtils;
import tech.cohorte.pico.tooling.CCTLoggerUtils;
import tech.cohorte.pico.tooling.CCTTimer;

/**
 * @author ogattaz
 *
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class CTestManifestReformator {

	private static final String ARGUMENT_FULL_RESOURCE_PATH = "fullResourcePath";

	private static final String ARGUMENT_RESOURCE_NAME = "resourceName";

	private static final String ARGUMENT_RESOURCE_PATH = "resourcePath";

	private static final int sNbTest = countNbTest(CTestManifestReformator.class);

	private static final AtomicInteger sSuccessCounter = new AtomicInteger(0);

	private static final AtomicInteger sTestCounter = new AtomicInteger(0);

	private static CCTTimer sTimer = null;

	private static final String TESTNAME = CTestManifestReformator.class.getSimpleName();

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

		logBanner(CTestManifestReformator.class, wMethod, Level.INFO,
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

		logBanner(CTestManifestReformator.class, wMethod, Level.INFO, "Tests of [%s] Begin. NbTest=[%d]", TESTNAME,
				sNbTest);

		// dump the current logger
		logInfo(CTestManifestReformator.class, wMethod, "%s",
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

		URL wResourceUrl = ManifestReformator.class.getResource(wFullResourcePath.toString());

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

		String wResourcePath = "/".concat(CTestManifestReformator.class.getPackage().getName().replace('.', '/'))
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
	public CTestManifestReformator() {
		super();
	}

	/**
	 * @param aMainAttributes
	 */
	private void dumpAttributes(final ManifestReformator aManifestReformator) {
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
	private ManifestReformator loadManifestReformator(final String aSubPackage, final String aResourceName)
			throws Exception {
		String wMethod = getMethodName(1);

		byte[] wManifestBytes = readResourceBytes(aSubPackage, aResourceName);

		InputStream wManifestDataStream = new ByteArrayInputStream(wManifestBytes);

		// instanciate
		ManifestReformator wManifestReformator = new ManifestReformator(wManifestDataStream);
		logInfo(this, wMethod, "Manifest.version=[%s]", wManifestReformator.getVersion());

		return wManifestReformator;
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
			ManifestReformator wManifestReformator = loadManifestReformator("/a", "MANIFEST.MF");
			String wVersion = wManifestReformator.getVersion();
			// version 1.0
			assertEquals(ManifestReformator.MANIFEST_VERSION_10, wVersion);
			logInfo(this, wMethod, "Manifest.version=[%s] >>>  assert equals 1.0 OK", wVersion);

			final int wAttributesSize = wManifestReformator.getMainAttributesSize();
			logInfo(this, wMethod, "Manifest.MainAttributes.size=[%d]", wAttributesSize);

			dumpAttributes(wManifestReformator);

			boolean wHasIPojoAttribute = wManifestReformator.hasIPojoAttribute();
			// yes there is an iPojoAttribute
			assertTrue(wHasIPojoAttribute);
			logInfo(this, wMethod, "Manifest.hasIPojoAttribute=[%b] >>> assert true OK", wHasIPojoAttribute);

			String wIPojoAttributeValue = wManifestReformator.getIPojoAttribute().getStringValue();
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
			ManifestReformator wManifestReformatorA = loadManifestReformator("/a", "MANIFEST.MF");
			logInfo(this, wMethod, "ReformatorA.IPojoAttribute.rank=[%d]",
					wManifestReformatorA.getIPojoAttributeRank());
			logInfo(this, wMethod, "ReformatorA.IPojoAttribute.last=[%b]",
					wManifestReformatorA.isIPojoAttributeLastOne());

			// new empty manifest
			ManifestReformator wManifestReformator = new ManifestReformator();
			String wVersion = wManifestReformator.getVersion();
			// versin 1.0 in the epty manifest
			assertEquals(ManifestReformator.MANIFEST_VERSION_10, wVersion);
			logInfo(this, wMethod, "Manifest.version=[%s]", wVersion);

			final int wAttributesSize = wManifestReformator.getMainAttributesSize();
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
			ManifestReformator wManifestReformatorA = loadManifestReformator("/a", "MANIFEST.MF");
			logInfo(this, wMethod, "ReformatorA.IPojoAttribute.rank=[%d]",
					wManifestReformatorA.getIPojoAttributeRank());
			logInfo(this, wMethod, "ReformatorA.IPojoAttribute.last=[%b]",
					wManifestReformatorA.isIPojoAttributeLastOne());
			// load the manifest "b"(with the same IPojo attribute)
			ManifestReformator wManifestReformatorB = loadManifestReformator("/b", "MANIFEST.MF");
			logInfo(this, wMethod, "ReformatorB.IPojoAttribute.rank=[%d]",
					wManifestReformatorB.getIPojoAttributeRank());
			logInfo(this, wMethod, "ReformatorB.IPojoAttribute.last=[%b]",
					wManifestReformatorB.isIPojoAttributeLastOne());

			boolean wSame = wManifestReformatorA.isIPojoAttributeSameAsIn(wManifestReformatorB);
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
			ManifestReformator wManifestReformatorA = loadManifestReformator("/a", "MANIFEST.MF");
			logInfo(this, wMethod, "ReformatorA.IPojoAttribute.rank=[%d]",
					wManifestReformatorA.getIPojoAttributeRank());
			logInfo(this, wMethod, "ReformatorA.IPojoAttribute.last=[%b]",
					wManifestReformatorA.isIPojoAttributeLastOne());
			// load the manifest "c" (without IPojo attribute)
			ManifestReformator wManifestReformatorC = loadManifestReformator("/c", "MANIFEST.MF");
			logInfo(this, wMethod, "ReformatorC.IPojoAttribute.rank=[%d]",
					wManifestReformatorC.getIPojoAttributeRank());
			logInfo(this, wMethod, "ReformatorC.IPojoAttribute.last=[%b]",
					wManifestReformatorC.isIPojoAttributeLastOne());

			boolean wSame = wManifestReformatorA.isIPojoAttributeSameAsIn(wManifestReformatorC);
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
			ManifestReformator wManifestReformatorC = loadManifestReformator("/c", "MANIFEST.MF");
			logInfo(this, wMethod, "ReformatorC.IPojoAttribute.rank=[%d]",
					wManifestReformatorC.getIPojoAttributeRank());
			logInfo(this, wMethod, "ReformatorC.IPojoAttribute.last=[%b]",
					wManifestReformatorC.isIPojoAttributeLastOne());

			final int wAttributesSize = wManifestReformatorC.getMainAttributesSize();
			//
			assertEquals(13, wAttributesSize);
			logInfo(this, wMethod, "Manifest.MainAttributes.size=[%d] >>> assert equals 13 OK ", wAttributesSize);

			// before
			dumpAttributes(wManifestReformatorC);

			wManifestReformatorC.appendIPojoAttribute("jhflqsjdfqlsdfjqlsdjfhlsjfdhj");

			final int wAttributesSizeAfter = wManifestReformatorC.getMainAttributesSize();
			//
			assertEquals(14, wAttributesSizeAfter);
			logInfo(this, wMethod, "Manifest.MainAttributes.size=[%d] >>> assert equals 14 OK ", wAttributesSizeAfter);

			logInfo(this, wMethod, "ReformatorC.IPojoAttribute.rank=[%d]",
					wManifestReformatorC.getIPojoAttributeRank());
			logInfo(this, wMethod, "ReformatorC.IPojoAttribute.last=[%b]",
					wManifestReformatorC.isIPojoAttributeLastOne());

			// after
			dumpAttributes(wManifestReformatorC);

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
			ManifestReformator wManifestReformatorA = loadManifestReformator("/a", "MANIFEST.MF");
			logInfo(this, wMethod, "ReformatorA.IPojoAttribute.rank=[%d]",
					wManifestReformatorA.getIPojoAttributeRank());
			logInfo(this, wMethod, "ReformatorA.IPojoAttribute.last=[%b]",
					wManifestReformatorA.isIPojoAttributeLastOne());
			logInfo(this, wMethod, "ReformatorA.IPojoAttribute.len =[%d]",
					wManifestReformatorA.getIPojoAttribute().getStringValue().length());

			// before
			dumpAttributes(wManifestReformatorA);

			// load the pojoizationStream
			String wPojoIzationStream = new String(readResourceBytes("pojoizationStream.txt"));

			logInfo(this, wMethod, "pojoizationStream.size=[%d]", wPojoIzationStream.length());

			ManifestReformator wManifestReformatorZ = new ManifestReformator();

			wManifestReformatorZ.appendIPojoAttribute(wPojoIzationStream);

			logInfo(this, wMethod, "ReformatorZ.IPojoAttribute.rank=[%d]",
					wManifestReformatorZ.getIPojoAttributeRank());
			logInfo(this, wMethod, "ReformatorZ.IPojoAttribute.last=[%b]",
					wManifestReformatorZ.isIPojoAttributeLastOne());
			logInfo(this, wMethod, "ReformatorZ.IPojoAttribute.len =[%d]",
					wManifestReformatorZ.getIPojoAttribute().getStringValue().length());

			boolean wSame = wManifestReformatorA.isIPojoAttributeSameAsIn(wManifestReformatorZ);
			// they are different
			assertFalse(wSame);
			logInfo(this, wMethod, "isIPojoAttributeSameAsIn=[%b] >>> assert false OK", wSame);

			if (!wSame) {
				wManifestReformatorA.replaceIPojoAttribute(wManifestReformatorZ);

				// after
				dumpAttributes(wManifestReformatorA);

				logInfo(this, wMethod, "DUMP ReformatorA:\n%s", wManifestReformatorA.toString());

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
