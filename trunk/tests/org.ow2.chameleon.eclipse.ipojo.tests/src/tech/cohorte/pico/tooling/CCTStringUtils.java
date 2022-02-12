package tech.cohorte.pico.tooling;

import java.util.List;

/**
 * @author ogattaz
 *
 */
public class CCTStringUtils {

	public static final String EMPTY = "";

	/**
	 * @param aString
	 * @return
	 */
	public static int indexOfFirstDigit(final String aString) {
		if (aString != null && !aString.isEmpty()) {
			int wLen = aString.length();
			for (int wIdx = 0; wIdx < wLen; wIdx++) {
				// '\u005Cu0030' through '\u005Cu0039', ISO-LATIN-1 digits ('0' through '9')
				if (Character.isDigit(aString.charAt(wIdx))) {
					return wIdx;
				}
			}
		}
		return -1;
	}

	/**
	 * @param aString
	 * @return
	 */
	public static int indexOfLastDigit(final String aString) {
		if (aString != null && !aString.isEmpty()) {
			int wLen = aString.length();
			for (int wIdx = wLen - 1; wIdx > -1; wIdx--) {
				// '\u005Cu0030' through '\u005Cu0039', ISO-LATIN-1 digits ('0' through '9')
				if (Character.isDigit(aString.charAt(wIdx))) {
					return wIdx;
				}
			}
		}
		return -1;
	}

	/**
	 * @param aValue
	 * @param aLen
	 * @param aLeadingChar
	 * @return
	 */
	public static String strAdjustLeft(String aValue, final int aLen, final char aLeadingChar) {

		if (aValue == null) {
			aValue = EMPTY;
		}

		final int wLen = aValue.length();
		if (wLen < aLen) {
			return aValue + strFromChar(aLeadingChar, aLen - wLen);
		} else if (wLen > aLen) {
			return aValue.substring(0, aLen);
		} else {
			return aValue;
		}
	}

	/**
	 * @param aValue
	 * @param aLen
	 * @param aLeadingChar
	 * @return
	 */
	public static String strAdjustRight(int aValue, int aLen, char aLeadingChar) {
		return strAdjustRight(String.valueOf(aValue), aLen, aLeadingChar);
	}

	/**
	 * @param aValue
	 * @param aLen
	 * @param aLeadingChar
	 * @return
	 */
	public static String strAdjustRight(String aValue, int aLen, char aLeadingChar) {
		int wLen = aValue.length();
		if (wLen < aLen)
			return strFromChar(aLeadingChar, aLen - wLen) + aValue;
		else if (wLen > aLen)
			return aValue.substring(aValue.length() - aLen);
		else
			return aValue;
	}

	/**
	 * @param aChar
	 * @param aLen
	 * @return
	 */
	public static String strFromChar(char aChar, int aLen) {
		if (aLen < 1)
			return "";
		if (aLen == 1)
			return String.valueOf(aChar);
		char[] wBuffer = new char[aLen];
		for (int wI = 0; wI < aLen; wI++) {
			wBuffer[wI] = aChar;
		}
		return String.valueOf(wBuffer);
	}

	/**
	 * @param aStringList
	 * @return
	 */
	public static String stringListToString(final List<String> aStringList) {

		return stringListToString(aStringList, ",");
	}

	/**
	 * @param strings
	 * @param sep
	 * @return
	 */
	public static String stringListToString(final List<String> aStringList, final String aSeparator) {

		if (aStringList == null || aStringList.size() == 0) {
			return EMPTY;
		}
		final StringBuilder wSB = new StringBuilder(256);
		int wI = 0;
		for (final String wStr : aStringList) {
			if (wI > 0) {
				wSB.append(aSeparator);
			}
			wSB.append(wStr);
			wI++;
		}
		return wSB.toString();
	}

	/**
	 * eg. org.apache.felix.main-5.4.0.jar => 5.4.0.jar
	 * 
	 * @param aString
	 * @return
	 */
	public static String trimLeftNoDigit(final String aString) {

		int wPosFirstDigit = indexOfFirstDigit(aString);
		if (wPosFirstDigit > 0) {
			return aString.substring(wPosFirstDigit);
		}
		return aString;
	}

	/**
	 * @param aString
	 * @return
	 */
	public static String trimNoDigit(final String aString) {

		return trimLeftNoDigit(trimRightNoDigit(aString));
	}

	/**
	 * @param aString
	 * @return
	 */
	public static String trimRightNoDigit(final String aString) {

		int wPosLastDigit = indexOfLastDigit(aString);
		if (wPosLastDigit > 0) {
			return aString.substring(0, wPosLastDigit + 1);
		}
		return aString;
	}

	/**
	 * @param aObject
	 * @param aMax
	 * @return
	 */
	public static String truncatedToString(final Object aObject, final int aMax) {

		// if the argument is null, then a string equal to "null"; otherwise, the value
		// of obj.toString() is returned.
		String wDump = String.valueOf(aObject);

		return (wDump.length() > aMax) ? wDump.substring(0, aMax) + "... truncated." : wDump;
	}

	/**
	 * 
	 */
	private CCTStringUtils() {
		super();
	}

}
