package tech.cohorte.pico.tooling;

import static tech.cohorte.pico.tooling.CCTStringUtils.stringListToString;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * @author ogattaz
 *
 */
public class CCTExceptionUtils {

	public static final boolean WITH_CLASS = true;

	public static final boolean WITH_NUMBER = true;

	/**
	 * @param aThrowable
	 * @return
	 */
	public static List<String> eCauseMessagesInList(final Throwable aThrowable) {
		return eCauseMessagesInList(aThrowable, !WITH_NUMBER, !WITH_CLASS);
	}

	/**
	 * @param aThrowable
	 * @param aWithNumber
	 * @param aWithClassName
	 * @return
	 */
	public static List<String> eCauseMessagesInList(final Throwable aThrowable, final boolean aWithNumber,
			final boolean aWithClassName) {

		List<String> wList = new ArrayList<>();
		Throwable wThrowable = aThrowable;
		int wIdx = 0;
		StringBuilder wMess;
		String wLocalizedMessage;
		boolean wHaswLocalizedMessage;
		while (wThrowable != null) {
			wMess = new StringBuilder();
			if (aWithNumber) {
				wMess.append(String.format("(level %2d) ", wIdx));
			}
			wLocalizedMessage = wThrowable.getLocalizedMessage();

			wHaswLocalizedMessage = (wLocalizedMessage != null && !wLocalizedMessage.isEmpty());

			if (wHaswLocalizedMessage) {
				wMess.append(wLocalizedMessage);
			} else {
				// s'il n'y a pas de message on met la classe
				wMess.append(wThrowable.getClass().getName());
			}

			if (aWithClassName) {
				wMess.append(" : ").append(wThrowable.getClass().getName());
			}
			wList.add(wMess.toString());
			wIdx++;
			wThrowable = wThrowable.getCause();
		}
		return wList;
	}

	/**
	 * @param aThrowable
	 * @return
	 */
	public static String eCauseMessagesInString(final Throwable aThrowable) {

		return stringListToString(eCauseMessagesInList(aThrowable));

	}

	/**
	 * @param aThrowable
	 * @return
	 */
	public static String eStackInString(Throwable aThrowable) {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		aThrowable.printStackTrace(pw);
		// stack trace as a string
		return sw.toString();
	}

}
