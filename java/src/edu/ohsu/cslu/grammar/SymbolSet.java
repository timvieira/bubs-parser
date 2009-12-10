package edu.ohsu.cslu.grammar;

import java.util.Hashtable;
import java.util.Vector;

public class SymbolSet {
	private Vector<String> symbolVector;
	private Hashtable<String,Integer> symbolHash;
	
	public SymbolSet() {
		symbolVector = new Vector<String>();
		symbolHash = new Hashtable<String,Integer>();
	}
	
	// get integer index of label string.  If it does not exist then
	// add it to the internal structures
	public int getIndex(String label){
		Integer index = symbolHash.get(label);
		if (index != null) {
			return index;
		} else {
			index=symbolVector.size();
			symbolHash.put(label, index.intValue());
			symbolVector.add(label);
			return index;
		}
	}
	
	public boolean hasLabel(String label) {
		return symbolHash.get(label) != null;
	}
	
	public String getString(int index){
		return symbolVector.get(index);
	}
	
	public int numSymbols() {
		return symbolVector.size();
	}
	
}
