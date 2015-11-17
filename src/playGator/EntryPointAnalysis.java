/*
 * reference: Gator 
 */

package playGator;

import soot.Scene;

public class EntryPointAnalysis {
	// the nested class to implement singleton
	private static class SingletonHolder {
		private static final EntryPointAnalysis instance = new EntryPointAnalysis();
	}
	
	// get THE instance
	public static final EntryPointAnalysis v() {
		return SingletonHolder.instance;
	}
	
	private EntryPointAnalysis() {
		
	}
	
	public void run() {
		// print the statistic data of the target app 
		// not sure about the diff between Classes and AppClasses
		System.out.println("[Stat] #Classes: " + Scene.v().getClasses().size() + 
				", #AppClasses: " + Scene.v().getApplicationClasses().size());
	}
}
