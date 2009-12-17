package edu.ohsu.cslu.parser.util;

public interface ScoreFunction {
	public boolean train();
	public float score(Object o);
	public void writeToFile(String fileName);
	public void readFromFile(String fileName);
}
