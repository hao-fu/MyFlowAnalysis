/*
 * reference: Gator 
 */

package playGator;

import presto.android.AnalysisEntrypoint;
import presto.android.Configs;
import soot.Scene;

public class EntryPointAnalysis extends AnalysisEntrypoint {
	// The nested class to implement singleton
	private static class SingletonHolder {
		private static final EntryPointAnalysis instance = new EntryPointAnalysis();
	}
	
	// Get THE instance
	public static final EntryPointAnalysis v() {
		return SingletonHolder.instance;
	}
	
	public void run() {
		// Print the statistic data of the target app 
		// Not sure about the diff between Classes and AppClasses
		System.out.println("[Stat] #Classes: " + Scene.v().getClasses().size() + 
				", #AppClasses: " + Scene.v().getApplicationClasses().size());
		
		// Sanity check?
	    if (!"1".equals(System.getenv("PRODUCTION"))) {
	        validate();
	     }
	    
	    // TODO instrument
	    
	    // Analysis
	    if (Configs.guiAnalysis) {
	    	
	    }
	}
}
