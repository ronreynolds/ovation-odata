package ovation.odata.util;

import java.util.Arrays;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import junit.framework.Assert;

public class JUnitUtils {
	public static void assertEquals(byte[] expected, byte[] actual) {
		assertEquals(null, expected, actual);
	}
	public static void assertEquals(String msg, byte[] expected, byte[] actual) {
		if (expected == actual) return;	// handles identical arrays and both null
		Assert.assertEquals(msg, Arrays.toString(expected), Arrays.toString(actual));
	}
	
	public static void assertEquals(String msg, boolean expected, Boolean actual) {
		Assert.assertNotNull(msg, actual);
		Assert.assertEquals(msg, expected, actual.booleanValue());
	}
	
	public static void assertEquals(double[] expected, double[] actual) {
		assertEquals(null, expected, actual);
	}
	public static void assertEquals(String msg, double[] expected, double[] actual) {
		if (expected == actual) return;	// handles identical arrays and both null
		Assert.assertEquals(msg, Arrays.toString(expected), Arrays.toString(actual));
	}

    public static void assertEquals(DateTime expected, DateTime actual) {
        assertEquals(null, expected, actual);
    }
    /**
     * had to create our own cuz this makes no sense:
     * 
     * junit.framework.AssertionFailedError: ovation:///93298e5d-4819-4a55-8ebf-9b923e82d86a/#2-1001-1-5:1000048 
     * expected:<2012-04-06T02:18:17.109-05:00>
     *  but was:<2012-04-06T02:18:17.109-05:00>
     * unless DateTime.equals() isn't comparing anything or it's comparing the wrong things. :-/
     * 
     * @param msg
     * @param expected
     * @param actual
     */
    public static void assertEquals(String msg, DateTime expected, DateTime actual) {
        if (expected == actual) return; // handles identical arrays and both null
        if (expected == null || actual == null) Assert.assertEquals(msg, expected, actual); // guaranteed to fail
        Assert.assertEquals(msg, expected.toDateTime(DateTimeZone.UTC), actual.toDateTime(DateTimeZone.UTC)); // DateTime.equals() doesn't seem to account for timezones
    }
	
	public static <T> void assertEquals(T[] expected, T[] actual) {
		assertEquals(null, expected, actual);
	}
	public static <T> void assertEquals(String msg, T[] expected, T[] actual) {
		if (expected == actual) return;	// handles identical arrays and both null
		Assert.assertEquals(msg, Arrays.toString(expected), Arrays.toString(actual));
	}
}
