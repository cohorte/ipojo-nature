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
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.jar.Attributes;
import java.util.logging.Level;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.ow2.chameleon.eclipse.ipojo.core.ManifestManipulator;

import tech.cohorte.pico.tooling.CCTExceptionUtils;
import tech.cohorte.pico.tooling.CCTJulUtils;
import tech.cohorte.pico.tooling.CCTLoggerUtils;
import tech.cohorte.pico.tooling.CCTTimer;

/**
 * @author ogattaz
 *
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class CTestManifestManipulator {

	private static final int sNbTest = countNbTest(CTestManifestManipulator.class);

	private static final AtomicInteger sSuccessCounter = new AtomicInteger(0);
	private static final AtomicInteger sTestCounter = new AtomicInteger(0);

	private static final String TESTNAME = CTestManifestManipulator.class.getSimpleName();

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

		logBanner(CTestManifestManipulator.class, wMethod, Level.INFO, "Test of [%s] done. Success=[%d/%d]", TESTNAME,
				sSuccessCounter.get(), sNbTest);
	}

	/**
	 *
	 */
	@BeforeClass
	public static void initialize() throws Exception {
		String wMethod = getMethodName(1);

		logBanner(CTestManifestManipulator.class, wMethod, Level.INFO, "Tests of [%s] Begin. NbTest=[%d]", TESTNAME,
				sNbTest);

		// dump the current logger
		logInfo(CTestManifestManipulator.class, wMethod, "%s",
				//
				CCTJulUtils.dumpCurrentLogger(CCTLoggerUtils.getCurrentLogger()));

	}

	/**
	 *
	 */
	public CTestManifestManipulator() {
		super();
	}

	/**
	 * @param aMainAttributes
	 */
	private void dumpAttributes(final ManifestManipulator aManifestManipulator) {
		String wMethod = getMethodName(1);
		int wSize = aManifestManipulator.getMainAttributesSize();
		int wIdxA = 0;
		for (Entry<Object, Object> wEntry : aManifestManipulator.getMainAttributesEntrySet()) {
			wIdxA++;
			String wValue = String.valueOf(wEntry.getValue());
			int wValueSize = wValue.length();
			logInfo(this, wMethod, "Attribute(%2d/%2d)=[%-36s]=[%6d][%s]", wIdxA, wSize, wEntry.getKey(), wValueSize,
					truncatedToString(wEntry.getValue(), 128));
		}
	}

	/**
	 * @param aSubPackage
	 * @param aResourceName
	 * @return
	 * @throws Exception
	 */
	private ManifestManipulator loadManifestManipulator(final String aSubPackage, final String aResourceName)
			throws Exception {
		String wMethod = getMethodName(1);

		byte[] wManifestBytes = readResourceBytes(aSubPackage, aResourceName);

		InputStream wManifestDataStream = new ByteArrayInputStream(wManifestBytes);

		// instanciate
		ManifestManipulator wManifestManipulator = new ManifestManipulator(wManifestDataStream);
		logInfo(this, wMethod, "Manifest.version=[%s]", wManifestManipulator.getVersion());

		return wManifestManipulator;
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
	 * @param aSubPackage
	 * @param aResourceName
	 * @return
	 * @throws Exception
	 */
	private byte[] readResourceBytes(final String aSubPackage, final String aResourceName) throws Exception {

		String wResourcePath = "/".concat(getClass().getPackage().getName().replace('.', '/')).concat(aSubPackage);
		return ManifestManipulator.readResourceBytes(aResourceName, wResourcePath);
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
			ManifestManipulator wManifestManipulator = loadManifestManipulator("/a", "MANIFEST.MF");
			String wVersion = wManifestManipulator.getVersion();
			// version 1.0
			assertEquals(ManifestManipulator.MANIFEST_VERSION_1, wVersion);
			logInfo(this, wMethod, "Manifest.version=[%s] >>>  assert equals 1.0 OK", wVersion);

			final int wAttributesSize = wManifestManipulator.getMainAttributesSize();
			logInfo(this, wMethod, "Manifest.MainAttributes.size=[%d]", wAttributesSize);

			dumpAttributes(wManifestManipulator);

			final Map<String, Attributes> wEntries = wManifestManipulator.getEntries();
			final int wEntriesSize = wEntries.size();
			logInfo(this, wMethod, "Manifest.entries.size=[%d]", wEntriesSize);

			int wIdxB = 0;
			for (Entry<String, Attributes> wEntry : new TreeMap<>(wEntries).entrySet()) {
				wIdxB++;
				logInfo(this, wMethod, "Entry(%2d/%2d)=[%-40s]=[%s]", wIdxB, wEntriesSize, wEntry.getKey(),
						truncatedToString(wEntry.getValue(), 256));
			}

			boolean wHasIPojoAttribute = wManifestManipulator.hasIPojoAttribute();
			// yes there is an iPojoAttribute
			assertTrue(wHasIPojoAttribute);
			logInfo(this, wMethod, "Manifest.hasIPojoAttribute=[%b] >>> assert true OK", wHasIPojoAttribute);

			String wIPojoAttributeValue = wManifestManipulator.getIPojoAttributeValue();
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
			ManifestManipulator wManipulatorA = loadManifestManipulator("/a", "MANIFEST.MF");
			logInfo(this, wMethod, "ManipulatorA.IPojoAttribute.rank=[%d]", wManipulatorA.getIPojoAttributeRank());
			logInfo(this, wMethod, "ManipulatorA.IPojoAttribute.last=[%b]", wManipulatorA.isIPojoAttributeLastOne());

			// new empty manifest
			ManifestManipulator wManifestManipulator = new ManifestManipulator();
			String wVersion = wManifestManipulator.getVersion();
			// versin 1.0 in the epty manifest
			assertEquals(ManifestManipulator.MANIFEST_VERSION_1, wVersion);
			logInfo(this, wMethod, "Manifest.version=[%s]", wVersion);

			final int wAttributesSize = wManifestManipulator.getMainAttributesSize();
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
			ManifestManipulator wManipulatorA = loadManifestManipulator("/a", "MANIFEST.MF");
			logInfo(this, wMethod, "ManipulatorA.IPojoAttribute.rank=[%d]", wManipulatorA.getIPojoAttributeRank());
			logInfo(this, wMethod, "ManipulatorA.IPojoAttribute.last=[%b]", wManipulatorA.isIPojoAttributeLastOne());
			// load the manifest "b"(with the same IPojo attribute)
			ManifestManipulator wManipulatorB = loadManifestManipulator("/b", "MANIFEST.MF");
			logInfo(this, wMethod, "ManipulatorB.IPojoAttribute.rank=[%d]", wManipulatorB.getIPojoAttributeRank());
			logInfo(this, wMethod, "ManipulatorB.IPojoAttribute.last=[%b]", wManipulatorB.isIPojoAttributeLastOne());

			boolean wSame = wManipulatorA.isIPojoAttributeSameAsIn(wManipulatorB);
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
			ManifestManipulator wManipulatorA = loadManifestManipulator("/a", "MANIFEST.MF");
			logInfo(this, wMethod, "ManipulatorA.IPojoAttribute.rank=[%d]", wManipulatorA.getIPojoAttributeRank());
			logInfo(this, wMethod, "ManipulatorA.IPojoAttribute.last=[%b]", wManipulatorA.isIPojoAttributeLastOne());
			// load the manifest "c" (without IPojo attribute)
			ManifestManipulator wManipulatorC = loadManifestManipulator("/c", "MANIFEST.MF");
			logInfo(this, wMethod, "ManipulatorC.IPojoAttribute.rank=[%d]", wManipulatorC.getIPojoAttributeRank());
			logInfo(this, wMethod, "ManipulatorC.IPojoAttribute.last=[%b]", wManipulatorC.isIPojoAttributeLastOne());

			boolean wSame = wManipulatorA.isIPojoAttributeSameAsIn(wManipulatorC);
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
			ManifestManipulator wManipulatorC = loadManifestManipulator("/c", "MANIFEST.MF");
			logInfo(this, wMethod, "ManipulatorC.IPojoAttribute.rank=[%d]", wManipulatorC.getIPojoAttributeRank());
			logInfo(this, wMethod, "ManipulatorC.IPojoAttribute.last=[%b]", wManipulatorC.isIPojoAttributeLastOne());

			final int wAttributesSize = wManipulatorC.getMainAttributesSize();
			//
			assertEquals(13, wAttributesSize);
			logInfo(this, wMethod, "Manifest.MainAttributes.size=[%d] >>> assert equals 13 OK ", wAttributesSize);

			// before
			dumpAttributes(wManipulatorC);

			wManipulatorC.appendIPojoAttribute("jhflqsjdfqlsdfjqlsdjfhlsjfdhj");

			final int wAttributesSizeAfter = wManipulatorC.getMainAttributesSize();
			//
			assertEquals(14, wAttributesSizeAfter);
			logInfo(this, wMethod, "Manifest.MainAttributes.size=[%d] >>> assert equals 14 OK ", wAttributesSizeAfter);

			logInfo(this, wMethod, "ManipulatorC.IPojoAttribute.rank=[%d]", wManipulatorC.getIPojoAttributeRank());
			logInfo(this, wMethod, "ManipulatorC.IPojoAttribute.last=[%b]", wManipulatorC.isIPojoAttributeLastOne());

			// after
			dumpAttributes(wManipulatorC);

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
			ManifestManipulator wManipulatorA = loadManifestManipulator("/a", "MANIFEST.MF");
			logInfo(this, wMethod, "ManipulatorA.IPojoAttribute.rank=[%d]", wManipulatorA.getIPojoAttributeRank());
			logInfo(this, wMethod, "ManipulatorA.IPojoAttribute.last=[%b]", wManipulatorA.isIPojoAttributeLastOne());
			logInfo(this, wMethod, "ManipulatorA.IPojoAttribute.len =[%d]",
					wManipulatorA.getIPojoAttributeValue().length());

			// before
			dumpAttributes(wManipulatorA);

			// load the pojoizationStream
			String wPojoIzationStream = new String(readResourceBytes("pojoizationStream.txt"));

			logInfo(this, wMethod, "pojoizationStream.size=[%d]", wPojoIzationStream.length());

			ManifestManipulator wManipulatorZ = new ManifestManipulator();

			wManipulatorZ.appendIPojoAttribute(wPojoIzationStream);

			logInfo(this, wMethod, "ManipulatorZ.IPojoAttribute.rank=[%d]", wManipulatorZ.getIPojoAttributeRank());
			logInfo(this, wMethod, "ManipulatorZ.IPojoAttribute.last=[%b]", wManipulatorZ.isIPojoAttributeLastOne());
			logInfo(this, wMethod, "ManipulatorZ.IPojoAttribute.len =[%d]",
					wManipulatorZ.getIPojoAttributeValue().length());

			boolean wSame = wManipulatorA.isIPojoAttributeSameAsIn(wManipulatorZ);
			// they are different
			assertFalse(wSame);
			logInfo(this, wMethod, "isIPojoAttributeSameAsIn=[%b] >>> assert false OK", wSame);

			if (!wSame) {
				wManipulatorA.replaceIPojoAttribute(wManipulatorZ);

				// after
				dumpAttributes(wManipulatorA);

				logInfo(this, wMethod, "DUMP ManipulatorA:\n%s", wManipulatorA.toString());

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
