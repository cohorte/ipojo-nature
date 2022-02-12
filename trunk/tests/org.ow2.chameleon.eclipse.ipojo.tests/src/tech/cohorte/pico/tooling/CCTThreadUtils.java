package tech.cohorte.pico.tooling;

/**
 * @author ogattaz
 *
 */
public class CCTThreadUtils {

	/**
	 * @param aDuration in milli-second
	 * @return false if interupted, true if the sleeping is complete
	 */
	public static boolean sleep(final long aDuration) {
		try {
			Thread.sleep(aDuration);
			return true;
		} catch (InterruptedException e) {
			return false;
		}
	}

}
