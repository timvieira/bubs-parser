package edu.ohsu.cslu.parser.util;

public final class Log {
	public static final int verbosity = 999;
	public static void info(int level, String msg) {
		if (verbosity >= level) {
			System.err.println(msg);
		}
	}
}
