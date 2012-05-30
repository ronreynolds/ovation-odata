package ovation.odata.util;

import java.lang.reflect.Array;

public class StringUtils {
	/**
	 * @param o - the object to convert to String
	 * @return a String representation of the object, properly handling if the object is an array of objects
	 */
	public static String toString(Object o) {
		if (o == null) return String.valueOf(o);
		Class<?> c = o.getClass();
		if (c.isArray()) {
			int len = Array.getLength(o);
			StringBuilder buf = new StringBuilder(len * 10);	// SWAG
			for (int x = 0; x < len; ++x) {
				Object elem = Array.get(o, x);
				buf.append(',').append(toString(elem));
			}
	        String s = buf.toString();
	        return "[" + (s.length() > 1 ? s.substring(1) : "<empty>") + "]";
		}
		
		return String.valueOf(o);
	}
	
	public static <T> String toString(Iterable<T> iter) {
	    StringBuilder buf = new StringBuilder();
	    for (T t : iter) {
            buf.append(',').append(toString(t));
	    }
	    String s = buf.toString();
        return "[" + (s.length() > 1 ? s.substring(1) : "<empty>") + "]";
	}
	
}
