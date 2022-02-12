package tech.cohorte.pico.tooling;

import java.lang.reflect.Method;

/**
 * Getting the name of the currently executing method
 * 
 * The fastest way I found is that:
 * 
 * https://stackoverflow.com/questions/442747/getting-the-name-of-the-currently-executing-method
 * 
 * 
 * @author ogattaz
 *
 */
public class CCTMethodUtils {

	// save it static to have it available on every call
	private static Method m;

	static {
		try {
			// public StackTraceElement[] getStackTrace() {
			m = Throwable.class.getDeclaredMethod("getStackTrace");
			m.setAccessible(true);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * @param depth
	 * @param aClass
	 * @param aArgClass
	 * @return
	 */
	public static Method getMethod(final int depth, final Class<?> aClass, final Class<?>... aArgClass) {

		try {
			String wMethodName = getMethodName(depth + 1);
			return aClass.getMethod(wMethodName, aArgClass);
		} catch (NoSuchMethodException | SecurityException e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * @param depth
	 * @return
	 */
	public static String getMethodName(final int depth) {
		try {
			StackTraceElement[] elements = (StackTraceElement[]) m.invoke(new Throwable());
			return elements[depth].getMethodName();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
}
