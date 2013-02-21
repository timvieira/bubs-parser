package edu.berkeley.nlp.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * TODO Inline remaining methods?
 * 
 * @author Dan Klein
 */
public class CollectionUtils {

    public static <K, V> void addToValueList(final Map<K, List<V>> map, final K key, final V value) {
        List<V> valueList = map.get(key);
        if (valueList == null) {
            valueList = new ArrayList<V>();
            map.put(key, valueList);
        }
        valueList.add(value);
    }

    public static <K, V> List<V> getValueList(final Map<K, List<V>> map, final K key) {
        final List<V> valueList = map.get(key);
        if (valueList == null)
            return Collections.emptyList();
        return valueList;
    }

}
