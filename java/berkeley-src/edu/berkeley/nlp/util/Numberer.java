package edu.berkeley.nlp.util;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * Gives unique integer serial numbers to a family of objects, identified by a name space. A Numberer is like a
 * collection of Indexes, and for many purposes it is more straightforward to use an Index, but Numberer can be useful
 * precisely because it maintains a global name space for numbered object families, and provides facilities for mapping
 * across numberings within that space. At any rate, it's widely used in some existing packages.
 * 
 * TODO Use shorts instead of ints?
 * 
 * @author Dan Klein
 */
public class Numberer implements Serializable {

    private static final long serialVersionUID = 1L;

    private static Map<String, Numberer> numbererMap = new HashMap<String, Numberer>();

    private int total;
    private Int2ObjectOpenHashMap<String> intToObject;
    private Object2IntOpenHashMap<String> objectToInt;
    private boolean locked = false;

    public Numberer() {
        total = 0;
        intToObject = new Int2ObjectOpenHashMap<String>();
        objectToInt = new Object2IntOpenHashMap<String>();
        objectToInt.defaultReturnValue(-1);
    }

    public static Map<String, Numberer> getNumberers() {
        return numbererMap;
    }

    /**
     * You need to call this after deserializing Numberer objects to restore the global namespace, since static objects
     * aren't serialized.
     */
    public static void setNumberers(final Map<String, Numberer> numbs) {
        numbererMap = numbs;
    }

    public static Numberer getGlobalNumberer(final String type) {
        Numberer n = numbererMap.get(type);
        if (n == null) {
            n = new Numberer();
            numbererMap.put(type, n);
        }
        return n;
    }

    /**
     * Get a number for an object in namespace type. This looks up the Numberer for <code>type</code> in the global
     * namespace map (creating it if none previously existed), and then returns the appropriate number for the key.
     */
    public static int number(final String type, final String o) {
        return getGlobalNumberer(type).number(o);
    }

    public static Object object(final String type, final int n) {
        return getGlobalNumberer(type).symbol(n);
    }

    /**
     * For an Object <i>o</i> that occurs in Numberers of type <i>sourceType</i> and <i>targetType</i>, translates the
     * serial number <i>n</i> of <i>o</i> in the <i>sourceType</i> Numberer to the serial number in the
     * <i>targetType</i> Numberer.
     */
    public static int translate(final String sourceType, final String targetType, final int n) {
        return getGlobalNumberer(targetType).number(getGlobalNumberer(sourceType).symbol(n));
    }

    public int total() {
        return total;
    }

    public void lock() {
        locked = true;
    }

    public boolean hasSeen(final Object o) {
        return objectToInt.keySet().contains(o);
    }

    public Set<String> objects() {
        return objectToInt.keySet();
    }

    public int size() {
        return objectToInt.size();
    }

    public int number(final String o) {
        int i = objectToInt.getInt(o);
        if (i < 0) {
            if (locked) {
                throw new NoSuchElementException("no object: " + o);
            }
            i = total;
            total++;
            objectToInt.put(o, i);
            intToObject.put(i, o);
        }
        return i;
    }

    public String symbol(final int n) {
        return intToObject.get(n);
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer();
        sb.append("[");
        for (int i = 0; i < total; i++) {
            sb.append(i);
            sb.append("->");
            sb.append(symbol(i));
            if (i < total - 1) {
                sb.append(", ");
            }
        }
        sb.append("]");
        return sb.toString();
    }
}
