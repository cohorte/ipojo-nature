package tech.cohorte.pico.tooling;

import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * @author ogattaz
 *
 */
public class CCTJulUtils {

	public static final String FILTER_JUL_NAME_CATALINA = "org.apache.catalina.*";

	public static final String FILTER_JUL_NAME_JETTY = "org.eclipse.jetty*";

	public static final String FILTER_JUL_NAME_SHIRO = "org.apache.shiro*";

	public static final String NO_FILTER = null;

	public static final String SIMPLE_FORMATTER_FORMAT_PROPERTY = "java.util.logging.SimpleFormatter.format";

	protected static SimpleFormatter sSimpleFormatter = new SimpleFormatter();

	/**
	 * Build a description of a Logger
	 *
	 * <pre>
	 * LoggerName=[org.apache.felix.gogo.runtime.threadio.ThreadIOImpl ](51) Level
	 * =[? ] UseParentH=[true] Parent=[root]
	 * LoggerName=[org.chohorte.isolate.logger.svc+2 ](33) Level =[ALL ]
	 * UseParentH=[false] Parent=[root]
	 *
	 * <pre>
	 * 
	 * @param aSB
	 * @param aLoggerName The name of the logger
	 * @param aLogger     The instance of Jul logger.
	 * @return
	 *
	 *
	 */
	public static StringBuilder addDescriptionInSB(final StringBuilder aSB, final String aLoggerName,
			final Logger aLogger) {

		String wLoggerName = aLoggerName;
		if (wLoggerName == null) {
			wLoggerName = (aLogger != null) ? aLogger.getName() : "null";
		}
		// if "root" logger => set label "root" after the size calculation !
		if (wLoggerName.isEmpty()) {
			wLoggerName = "root";
		}

		aSB.append(String.format(" JULogger:[%-70s]", wLoggerName));

		if (aLogger == null) {
			aSB.append(" - LOGGER IS NULL");
		} else {

			aSB.append(
					String.format(" Level:[%-7s]", (aLogger.getLevel() != null) ? aLogger.getLevel().getName() : "?"));

			aSB.append(String.format(" UseParentH=[%b]", aLogger.getUseParentHandlers()));

			if (aLogger.getParent() != null) {
				final String wParentName = aLogger.getParent().getName();
				aSB.append(String.format(" Parent:[%s]", (!wParentName.isEmpty() ? wParentName : "root")));
			}

			final Handler[] wHandlers = aLogger.getHandlers();
			for (final Handler wHandler : wHandlers) {
				if (wHandler == null) {
					aSB.append("\n\t- Handler is null.");
				} else {
					String wFormatterClassName = "?";
					final Formatter wFormatter = wHandler.getFormatter();
					if (wFormatter != null) {
						wFormatterClassName = wFormatter.getClass().getSimpleName();
					}
					aSB.append(String.format("\n\t- Handler=[%25s] Formatter=[%s_]",
							wHandler.getClass().getSimpleName(), wFormatterClassName));
				}
			}
		}
		return aSB;
	}

	/**
	 * @param wSB
	 * @param aLogger
	 * @param aLoggerIdx
	 * @return
	 */
	public static StringBuilder addDumpCurrentLoggerInSB(final StringBuilder wSB, final Logger aLogger,
			final int aLoggerIdx) {
		return addDumpCurrentLoggerInSB(wSB, aLogger.getName(), aLoggerIdx);
	}

	/**
	 * @param wSB
	 * @param aLoggerName
	 * @param aLoggerIdx
	 * @return
	 */
	public static StringBuilder addDumpCurrentLoggerInSB(final StringBuilder wSB, final String aLoggerName,
			final int aLoggerIdx) {

		if (aLoggerName == null) {
			wSB.append(String.format("\n(%3d) LoggerName is null.", aLoggerIdx));
		}
		//
		else {
			wSB.append(String.format("\n(%3d) ", aLoggerIdx));
			final Logger wLogger = getLogManager().getLogger(aLoggerName);
			addDescriptionInSB(wSB, aLoggerName, wLogger);
		}

		return wSB;
	}

	/**
	 * <pre>
	 * (  0) LoggerName=[root                                                                  ]( 0) UseParentHandlers=[true]
	 * 	- Level =[INFO]
	 * 	- Handler=[           ConsoleHandler] Formatter=[CActivityFormaterHuman_543609822]
	 * (  1) LoggerName=[/                                                                     ]( 1) UseParentHandlers=[true] Parent=[root]
	 * (  2) LoggerName=[cohorte.isolate.aggregator.ISOL-ATEA-GGRE-GATO-R000+2                 ](53) UseParentHandlers=[false] Parent=[root]
	 * 	- Level =[ALL]
	 * 	- Handler=[     CActivityFileHandler] Formatter=[CActivityFormaterHuman_543609822]
	 * (  3) LoggerName=[global                                                                ]( 6) UseParentHandlers=[true] Parent=[root]
	 * (  4) LoggerName=[javax.ws.rs.ext.FactoryFinder                                         ](29) UseParentHandlers=[true] Parent=[root]
	 * (  5) LoggerName=[org.apache.felix.gogo.runtime.threadio.ThreadIOImpl                   ](51) UseParentHandlers=[true] Parent=[root]
	 * (  6) LoggerName=[org.apache.shiro.util.ThreadContext                                   ](35) UseParentHandlers=[true] Parent=[root]
	 * (  7) LoggerName=[org.eclipse.jetty.http.HttpFields                                     ](33) UseParentHandlers=[true] Parent=[root]
	 * (  8) LoggerName=[org.eclipse.jetty.http.HttpGenerator                                  ](36) UseParentHandlers=[true] Parent=[root]
	 * ...
	 * ( 20) LoggerName=[org.eclipse.jetty.server.handler.ContextHandler                       ](47) UseParentHandlers=[true] Parent=[root]
	 * ( 21) LoggerName=[org.eclipse.jetty.server.handler.ContextHandlerCollection             ](57) UseParentHandlers=[true] Parent=[root]
	 * ( 22) LoggerName=[org.eclipse.jetty.server.session                                      ](32) UseParentHandlers=[true] Parent=[root]
	 * ( 23) LoggerName=[org.eclipse.jetty.server.session.AbstractSessionIdManager             ](57) UseParentHandlers=[true] Parent=[org.eclipse.jetty.server.session]
	 * ...
	 * ( 38) LoggerName=[org.eclipse.jetty.util.thread.strategy.ExecutingExecutionStrategy     ](65) UseParentHandlers=[true] Parent=[root]
	 * ( 39) LoggerName=[org.glassfish.jersey.internal.Errors                                  ](36) UseParentHandlers=[true] Parent=[root]
	 * ( 40) LoggerName=[org.glassfish.jersey.internal.OsgiRegistry                            ](42) UseParentHandlers=[true] Parent=[root]
	 * ...
	 * ( 52) LoggerName=[org.glassfish.jersey.servlet.WebComponent                             ](41) UseParentHandlers=[true] Parent=[root]
	 * ( 53) LoggerName=[org.jvnet.hk2.logger                                                  ](20) UseParentHandlers=[true] Parent=[root]
	 * ( 54) LoggerName=[sun.net.www.protocol.http.HttpURLConnection                           ](43) UseParentHandlers=[true] Parent=[root]
	 * </pre>
	 *
	 * @return
	 */
	public static StringBuilder addDumpCurrentLoggersInSB(final StringBuilder aSB, final String aLoggerNameFilter) {

		final List<String> wSortedwNames = getLoggerNames();

		int wLoggerIdx = 0;
		for (final String wLoggerName : wSortedwNames) {

			addDumpCurrentLoggerInSB(aSB, wLoggerName, wLoggerIdx);
			wLoggerIdx++;
		}
		return aSB;
	}

	/**
	 * @param aLogger
	 * @param aLoggerIdx
	 * @return
	 */
	public static String dumpCurrentLogger(final Logger aLogger) {
		return addDumpCurrentLoggerInSB(new StringBuilder(), aLogger.getName(), 1).toString();
	}

	/**
	 * @return
	 */
	public static String dumpCurrentLoggers() {
		return addDumpCurrentLoggersInSB(new StringBuilder(), null).toString();
	}

	/**
	 * @param aLoggerNameFilter The filter to apply. Test the string equality by
	 *                          default. If the last char is a star "*", the filter
	 *                          is used as prefix.
	 * @return
	 */
	public static String dumpCurrentLoggers(final String aLoggerNameFilter) {
		return addDumpCurrentLoggersInSB(new StringBuilder(), aLoggerNameFilter).toString();
	}

	/**
	 * @param aLoggerName
	 * @param aLoggerNameFilter The filter to apply. Test the string equality by
	 *                          default. If the last char is a star "*", the filter
	 *                          is used as prefix.
	 * @return
	 */
	private static boolean filterLogger(final String aLoggerName, final String aLoggerNameFilter) {

		if (aLoggerName == null || aLoggerName.isEmpty() || aLoggerNameFilter == null || aLoggerNameFilter.isEmpty()) {
			return true;
		}

		if (aLoggerNameFilter.endsWith("*")) {
			final String wFilterPrefix = aLoggerNameFilter.substring(0, aLoggerNameFilter.length() - 2);
			return aLoggerName.startsWith(wFilterPrefix);
		}
		return aLoggerName.equals(aLoggerNameFilter);
	}

	/**
	 * @return The sorted list of the names of the Jul loggers
	 */
	public static List<String> getLoggerNames() {
		final Enumeration<String> wNames = getLogManager().getLoggerNames();

		final List<String> wSortedNames = Collections.list(wNames);

		Collections.sort(wSortedNames);

		return wSortedNames;

	}

	/**
	 * @param aLoggerNameFilter The filter to apply. Test the string equality by
	 *                          default. If the last char is a star "*", the filter
	 *                          is used as prefix.
	 * @return The sorted list of the names of the Jul loggers
	 */
	public static List<String> getLoggerNames(final String aLoggerNameFilter) {

		final List<String> wSortedNames = getLoggerNames();

		if (aLoggerNameFilter == null || aLoggerNameFilter.isEmpty()) {
			return wSortedNames;
		}

		for (final String wLoggerName : wSortedNames) {
			if (!filterLogger(wLoggerName, aLoggerNameFilter)) {
				wSortedNames.remove(wLoggerName);
			}
		}
		return wSortedNames;
	}

	/**
	 * @return the current Jul Manager
	 */
	public static LogManager getLogManager() {
		return LogManager.getLogManager();
	}

	/**
	 * @return the "main" Jul logger
	 */
	public static Logger getRootLogger() {
		return getLogManager().getLogger("");
	}

	/**
	 * 
	 * @return the static instance of SimpleFormatter attached to this class
	 */
	public static SimpleFormatter getSimpleFormatterInstance() {
		return sSimpleFormatter;
	}

	/**
	 * @return the value of the system property
	 *         "java.util.logging.SimpleFormatter.format"
	 */
	public static String getSimpleFormatterJvmProperty() {

		return System.getProperty(SIMPLE_FORMATTER_FORMAT_PROPERTY);
	}

	/**
	 * @param aLogger    The Jul logger to set
	 * @param aFormatter The Jul line formater to apply
	 * @return The number of modified handler
	 */
	public static int setFormatter(final Logger aLogger, final Formatter aFormatter) {

		int wNbSet = 0;
		final Handler[] wHandlers = aLogger.getHandlers();

		if (wHandlers != null && wHandlers.length > 0) {
			for (final Handler wHandler : wHandlers) {
				if (wHandler instanceof ConsoleHandler) {
					wHandler.setFormatter(aFormatter);
					wNbSet++;
				} else if (wHandler instanceof FileHandler) {
					wHandler.setFormatter(aFormatter);
					wNbSet++;
				}
			}
		}
		return wNbSet;
	}

	/**
	 * @param aLogger
	 * @return the number of modified handler
	 */
	public static int setSimpleFormatter(final Logger aLogger) {
		return setFormatter(aLogger, sSimpleFormatter);
	}

	/**
	 * @return the number of modified handler
	 */
	public static int setSimpleFormatterOfRootLooger() {
		return setFormatter(getRootLogger(), sSimpleFormatter);
	}

	/**
	 * @param aLogger
	 * @return
	 */
	public static String toString(final Logger aLogger) {
		final String wLoggerName = (aLogger != null) ? aLogger.getName() : "logger null";
		return addDescriptionInSB(new StringBuilder(), wLoggerName, aLogger).toString();
	}
}
