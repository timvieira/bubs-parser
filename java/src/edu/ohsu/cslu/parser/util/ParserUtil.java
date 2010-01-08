package edu.ohsu.cslu.parser.util;

import java.util.Collection;
import java.util.Iterator;
import java.util.StringTokenizer;

public class ParserUtil {

	// parse 'line' into tokens and return them in an array
	public static String[] tokenize(final String line) {
		final StringTokenizer st = new StringTokenizer(line);
		final int numTokens = st.countTokens();
		final String[] tokens = new String[numTokens];

		int i = 0;
		while (st.hasMoreTokens()) {
			tokens[i++] = st.nextToken();
		}

		return tokens;
	}

	public static String join(final Collection<String> s, final String delimiter) {
		final StringBuffer buffer = new StringBuffer();
		final Iterator<String> iter = s.iterator();
		while (iter.hasNext()) {
			buffer.append(iter.next());
			if (iter.hasNext()) {
				buffer.append(delimiter);
			}
		}
		return buffer.toString();
	}

	public static boolean isUpperCase(final String s) {
		return s == s.toUpperCase();
	}

	public static boolean isLowerCase(final String s) {
		return s == s.toLowerCase();
	}

	public static boolean containsDigit(final String s) {
		for (int i = 0; i < s.length(); i++) {
			if (Character.isDigit(s.charAt(i)))
				return true;
		}
		return false;
	}

	public class Pair<Type1, Type2> {
		public final Type1 one;
		public final Type2 two;

		public Pair(final Type1 a, final Type2 b) {
			one = a;
			two = b;
		}
	}

	public double safeLog(final double value) {
		if (value == 0) {
			return Double.NEGATIVE_INFINITY;
		}
		return Math.log(value);
	}
}
