package edu.berkeley.nlp.util;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.AbstractList;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.RandomAccess;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * @author Dan Klein
 */
public class CollectionUtils {

    public static <K, V> void addToValueList(Map<K, List<V>> map, K key, V value) {
        List<V> valueList = map.get(key);
        if (valueList == null) {
            valueList = new ArrayList<V>();
            map.put(key, valueList);
        }
        valueList.add(value);
    }

    public static <K, V> List<V> getValueList(Map<K, List<V>> map, K key) {
        List<V> valueList = map.get(key);
        if (valueList == null)
            return Collections.emptyList();
        return valueList;
    }

}
