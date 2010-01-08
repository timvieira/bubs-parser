package edu.ohsu.cslu.counters;

import java.util.HashMap;
import java.util.Set;
import java.util.Map.Entry;

public class SimpleCounter<E> {
	// using floats instead of ints so we can support partial counts and smoothing
	private HashMap<E, Float> counts;
	private float total;
	private float unseenElementCount;

	public SimpleCounter() {
		counts = new HashMap<E, Float>();
		total = 0;
		unseenElementCount = 0;
	}

	public void increment(final E key) {
		this.increment(key, 1);
	}

	public void increment(final E key, final float amount) {
		total += amount;
		final Float oldValue = counts.get(key);
		if (oldValue == null) {
			counts.put(key, new Float(amount));
		} else {
			counts.put(key, oldValue + amount);
		}
	}

	public float getCount(final E key) {
		final Float value = counts.get(key);
		if (value == null) {
			return unseenElementCount;
		}
		return value;
	}

	public float getProb(final E key) {
		return getCount(key) / total;
	}

	public Set<Entry<E, Float>> entrySet() {
		return counts.entrySet();
	}

	public float getTotalCount() {
		return total;
	}

	public void smoothAddConst(final float constToAdd, final int numTotalElements) {
		unseenElementCount = constToAdd;
		final int numUnseenElements = numTotalElements - counts.size();
		total += numUnseenElements * unseenElementCount;

		for (final E element : counts.keySet()) {
			increment(element, constToAdd);
		}
	}

}
