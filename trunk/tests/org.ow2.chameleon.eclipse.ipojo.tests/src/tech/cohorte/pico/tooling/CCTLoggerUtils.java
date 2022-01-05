package tech.cohorte.pico.tooling;

import static tech.cohorte.pico.tooling.CCTStringUtils.strAdjustRight;
import static tech.cohorte.pico.tooling.CCTTextLineUtils.generateLineBeginEnd;
import static tech.cohorte.pico.tooling.CCTTextLineUtils.generateLineFull;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 
 * @author ogattaz
 *
 */
public class CCTLoggerUtils {

	// Logger Root
	private static Logger sCurrentLogger = Logger.getLogger("");

	private static final Object sCurrentLoggerLocker = new Object();

	/**
	 * 
	 * <pre>
	 * DATE(1)                   LEVEL(4) THREAD(3)         SOURCE(2): INSTANCE + METHOD                            LINE (5) + (6)
	 * <------- 24 car ------->..<-7c-->..<---- 16c ----->..<--------------------- 54c -------------------------->..<------------------ N characters  -------...
	 *                                                      <--------- 27c------------>..<----------25c ---------->
	 * Logger File
	 * 2019/02/12; 10:58:05:630; FINE   ;    SSEMonitor(1); SSEMachineRequestsMaps_6830;                 sendIddle; begin
	 * 2019/02/12; 10:58:05:630; FINE   ;    SSEMonitor(1); se.CSSEMachineRequests_9877;                 sendIddle; key=[(01,105)(cb6c8485-258a-496c-93a8-40aff9f997b7)] ...
	 * 2019/02/12; 10:58:05:630; FINE   ;    SSEMonitor(1); SSEMachineRequestsMaps_6830;                 sendIddle; end. NbSentIddle=[0]
	 * Logger console
	 * 2019/02/12; 15:59:28:339;   Infos;             main; apps.impl.CTestLogging_0842;                    doTest; SimpleFormatter current format=[%1$tY/%1$tm/%1$td; %1$tH:%1$tM:%1$tS:%1$tL; %4$7.7s; %3$16.016s; %2$54.54s; %5$s%6$s%n]
	 * 2019/02/12; 15:59:28:344;   Infos;             main; apps.impl.CTestLogging_0842;                    doTest; SimpleFormatter jvm property  =[%1$tY/%1$tm/%1$td; %1$tH:%1$tM:%1$tS:%1$tL; %4$7.7s; %3$16.016s; %2$54.54s; %5$s%6$s%n]
	 * 2019/02/12; 15:59:28:345;   Infos;             main; apps.impl.CTestLogging_0842;                    doTest; IsSimpleFormatterFormatValid=[true] / JulLogger: Name=[] Level=[ALL] 
	 * 2019/02/12; 15:59:28:346;   Infos;             main; apps.impl.CTestLogging_0842;                    doTest; logInfo: Ligne log info
	 * </pre>
	 * 
	 * 
	 * <pre>
	 * SimpleFormat=[%1$tY/%1$tm/%1$td; %1$tH:%1$tM:%1$tS:%1$tL; %4$7.7s; %3$16.016s; %2$54.54s; %5$s%6$s%n]
	 * </pre>
	 */
	public static final String SIMPLE_FORMATTER_FORMAT = "%1$tY/%1$tm/%1$td; %1$tH:%1$tM:%1$tS:%1$tL; %4$7.7s; %3$16.016s; %2$54.54s; %5$s%6$s%n";

	/**
	 * @param e
	 * @return
	 */
	public static String dumpStackTrace(Throwable e) {
		StringWriter errors = new StringWriter();
		e.printStackTrace(new PrintWriter(errors));
		return errors.toString();
	}

	/**
	 * @param aWho
	 * @return
	 */
	private static String formatSourceClass(final Object aWho) {

		if (aWho == null) {
			return "null who";
		}

		String wClassName = aWho.getClass().getSimpleName();
		int wInstanceHash = aWho.hashCode();

		if (aWho instanceof Class) {
			wClassName = ((Class<?>) aWho).getSimpleName();
			wInstanceHash = 0;
		}

		String wInstanceId = strAdjustRight(wInstanceHash, 4, '0');

		String wSourceClass = String.format("%s_%s", wClassName, wInstanceId);

		return strAdjustRight(wSourceClass, 27, ' ');
	}

	/**
	 * @param aWho
	 * @return
	 */
	private static String formatSourceMethod(final String aWhat) {

		return strAdjustRight(aWhat, 25, ' ');
	}

	/**
	 * @return
	 */
	public static Logger getCurrentLogger() {
		synchronized (sCurrentLoggerLocker) {
			return sCurrentLogger;
		}
	}

	/**
	 * @return
	 */
	public static Level getCurrentLoggerLevel() {
		return getCurrentLogger().getLevel();
	}

	/**
	 * @param aLevel
	 * @param aText
	 * @return
	 */
	public static String log(final Object aWho, final String aWhat, final Level aLevel, final String aText) {

		String wSource = String.format("%s; %s", formatSourceClass(aWho), formatSourceMethod(aWhat));

		// logp(Level level, String sourceClass , String sourceMethod, String msg)
		sCurrentLogger.logp(aLevel, wSource, "xxxx", aText);

		return aText;
	}

	/**
	 * @param aLevel
	 * @param aText
	 * @return
	 */
	public static String logBanner(final Object aWho, final String aWhat, final Level aLevel, final String aText) {
		log(aWho, aWhat, aLevel, generateLineFull('#', 80));
		log(aWho, aWhat, aLevel, generateLineBeginEnd('#', 80));
		log(aWho, aWhat, aLevel, generateLineBeginEnd('#', 80, aText));
		log(aWho, aWhat, aLevel, generateLineBeginEnd('#', 80));
		log(aWho, aWhat, aLevel, generateLineFull('#', 80));
		return aText;
	}

	/**
	 * @param aLevel
	 * @param aFormat
	 * @param aArgs
	 * @return
	 */
	public static String logBanner(final Object aWho, final String aWhat, final Level aLevel, final String aFormat,
			final Object... aArgs) {
		return logBanner(aWho, aWhat, aLevel, String.format(aFormat, aArgs));
	}

	/**
	 * @param aText
	 * @return
	 */
	public static String logInfo(final Object aWho, final String aWhat, final String aText) {

		return log(aWho, aWhat, Level.INFO, aText);
	}

	/**
	 * @param aFormat
	 * @param aArgs
	 * @return
	 */
	public static String logInfo(final Object aWho, final String aWhat, final String aFormat, final Object... aArgs) {

		return log(aWho, aWhat, Level.INFO, String.format(aFormat, aArgs));
	}

	/**
	 * @param aWho
	 * @param aWhat
	 * @return
	 */
	public static String logInfoBegin(final Object aWho, final String aWhat) {
		return logInfoBegin(aWho, aWhat, "");
	}

	/**
	 * @param aWho
	 * @param aWhat
	 * @param aFormat
	 * @param aArgs
	 * @return
	 */
	public static String logInfoBegin(final Object aWho, final String aWhat, final String aFormat,
			final Object... aArgs) {
		return logInfo(aWho, aWhat, "---- Begin ---------- " + aFormat, aArgs);
	}

	/**
	 * @param aWho
	 * @param aWhat
	 * @return
	 */
	public static String logInfoEnd(final Object aWho, final String aWhat) {
		return logInfoEnd(aWho, aWhat, "");
	}

	/**
	 * @param aWho
	 * @param aWhat
	 * @param aFormat
	 * @param aArgs
	 * @return
	 */
	public static String logInfoEnd(final Object aWho, final String aWhat, final String aFormat,
			final Object... aArgs) {
		return logInfo(aWho, aWhat, "---- End   ---------- " + aFormat, aArgs);

	}

	/**
	 * @param aText
	 * @return
	 */
	public static String logSevere(final Object aWho, final String aWhat, final String aText) {

		return log(aWho, aWhat, Level.SEVERE, aText);
	}

	/**
	 * @param aFormat
	 * @param aArgs
	 * @return
	 */
	public static String logSevere(final Object aWho, final String aWhat, final String aFormat, final Object... aArgs) {

		return log(aWho, aWhat, Level.SEVERE, String.format(aFormat, aArgs));
	}

	/**
	 * @param e
	 * @return
	 */
	public static String logSevere(final Object aWho, final String aWhat, final Throwable e) {

		return log(aWho, aWhat, Level.SEVERE, dumpStackTrace(e));
	}

	/**
	 * @param aLogger
	 */
	static boolean setCurrentLogger(final Logger aLogger) {

		boolean wUpdated = false;

		synchronized (sCurrentLoggerLocker) {
			wUpdated = (sCurrentLogger != aLogger);
			if (wUpdated) {
				sCurrentLogger = aLogger;
			}
		}
		if (wUpdated) {
			logInfo(CCTLoggerUtils.class, "setCurrentLogger", "Current Logger set with Logger=[%s]", aLogger.getName());
		}
		return wUpdated;
	}

	/**
	 * never instanciate a Helper
	 */
	private CCTLoggerUtils() {
		super();
	}
}
