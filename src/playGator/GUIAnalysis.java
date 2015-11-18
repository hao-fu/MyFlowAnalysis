/*
 * Gui analysis
 * reference: gator
 */
package playGator;

import presto.android.xml.XMLParser;


public class GUIAnalysis {
	private XMLParser xmlParser;
	
	// The nested class to implement singleton
	private static class SingletonHolder {
		private static final GUIAnalysis instance = new GUIAnalysis(XMLParser.Factory.getXMLParser());
	}
	
	// Get THE instance
	public static final GUIAnalysis v() {
		return SingletonHolder.instance;
	}
	
	public GUIAnalysis(XMLParser xmlParser) {
		this.xmlParser = xmlParser;
	}
	
	public void retrieveIds() {
		// the layout ids 
		for (int id : xmlParser.getApplicationLayoutIdValues()) {
			System.out.println(id);
		}
		
	}
	
	public void run() {
		System.out.println("[GUIAnalysis] Start");
		long startTime = System.nanoTime();
		
		// 0. retrieve ui ids 
		retrieveIds();
	}
}
