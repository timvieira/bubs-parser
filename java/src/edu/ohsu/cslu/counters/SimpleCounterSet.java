package edu.ohsu.cslu.counters;

import java.util.HashMap;

public class SimpleCounterSet<E> {
	public HashMap<E, SimpleCounter<E>> items;

	public SimpleCounterSet() {
		items = new HashMap<E, SimpleCounter<E>>();
	}

	public void increment(final E numerator, final E denominator) {
		increment(numerator, denominator, 1);
	}

	public void increment(final E numerator, final E denominator, final float amount) {
		if (items.containsKey(denominator) == false) {
			items.put(denominator, new SimpleCounter<E>());
		}
		items.get(denominator).increment(numerator, amount);
		// System.out.println("inc: " + numerator + " | " + denominator + "  adding " + amount);
	}

	public float getCount(final E numerator, final E denominator) {
		if (items.containsKey(denominator) == false) {
			return 0; // should we smooth these too?
		}
		return items.get(denominator).getCount(numerator);
	}

	public float getProb(final E numerator, final E denominator) {
		if (items.containsKey(denominator) == false) {
			return 0;
		}
		return items.get(denominator).getProb(numerator);
	}

	public void smoothAddConst(final float constToAdd, final int numTotalElements) {
		for (final SimpleCounter<E> counter : items.values()) {
			counter.smoothAddConst(constToAdd, numTotalElements);
		}
	}

	public void smoothAddConst(final double constToAdd, final int numTotalElements) {
		smoothAddConst((float) constToAdd, numTotalElements);
	}
}
