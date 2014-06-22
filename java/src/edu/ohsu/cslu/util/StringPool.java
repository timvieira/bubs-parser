/*
 * Copyright 2010-2014, Oregon Health & Science University
 * 
 * This file is part of the BUBS Parser.
 * 
 * The BUBS Parser is free software: you can redistribute it and/or 
 * modify  it under the terms of the GNU Affero General Public License 
 * as published by the Free Software Foundation, either version 3 of 
 * the License, or (at your option) any later version.
 * 
 * The BUBS Parser is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with the BUBS Parser. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Further documentation and contact information is available at
 *   https://code.google.com/p/bubs-parser/ 
 */
package edu.ohsu.cslu.util;

import java.io.Serializable;
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
public class StringPool implements Serializable {

    private static final long serialVersionUID = 1L;

    private final HashMap<String, String> internMap = new HashMap<String, String>(50000);

    public String intern(final String s) {
        final String internedString = internMap.get(s);
        if (internedString != null) {
            return internedString;
        }
        internMap.put(s, s);
        return s;
    }
}
