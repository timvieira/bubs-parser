package edu.ohsu.cslu.util;

import java.util.HashMap;

/**
 * Represents a pool of strings, each mapped to itself. Analogous to the JVM's internal string pool accessed with
 * {@link String#intern()}, but this pool can be cleared after use if desired.
 * 
 * @author Aaron Dunlop
 * @since Jan 11, 2011
 * 
 * @version $Revision$ $Date$ $Author$
 */
public class StringPool {

    private final HashMap<String, String> internMap = new HashMap<String, String>(1000);

    public String intern(final String s) {
        final String internedString = internMap.get(s);
        if (internedString != null) {
            return internedString;
        }
        internMap.put(s, s);
        return s;
    }
}
