/*
 * reference: gator
 */
package playGator;


public class GUIAnalysis {
	// The nested class to implement singleton
	private static class SingletonHolder {
		private static final GUIAnalysis instance = new GUIAnalysis();
	}
	
	// Get THE instance
	public static final GUIAnalysis v() {
		return SingletonHolder.instance;
	}
}
