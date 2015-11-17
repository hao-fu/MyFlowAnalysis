/*
 * https://github.com/miwong/SimpleLeakAnalysis/blob/master/src/LeakAnalysis.java
 */

package playSoot;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import soot.MethodOrMethodContext;
import soot.Scene;
import soot.SceneTransformer;
import soot.SootMethod;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.ReachableMethods;
import soot.jimple.toolkits.callgraph.Sources;
import soot.jimple.toolkits.callgraph.TransitiveTargets;


public class LeakAnalysis extends SceneTransformer {
	private CallGraph mCallGraph;
	
	private static final List<String> sourceAPIs = Arrays.asList(
		"<android.provider.Browser: android.database.Cursor getAllVisitedUrls(android.content.ContentResolver)>",
		
		//Query (should not be here)
		"<android.content.ContentResolver: android.database.Cursor query(android.net.Uri,java.lang.String[],java.lang.String,java.lang.String[],java.lang.String)>"
	);
	
	private static final List<String> sinkAPIs = Arrays.asList(
		//SMS
		"<android.telephony.SmsManager: void sendTextMessage(java.lang.String,java.lang.String,java.lang.String,android.app.PendingIntent,android.app.PendingIntent)>",
		
		//Internet
		//"<org.apache.http.client.HttpClient: org.apache.http.HttpResponse execute(org.apache.http.client.methods.HttpUriRequest)>",
		"<org.apache.http.impl.client.AbstractHttpClient: org.apache.http.HttpResponse execute(org.apache.http.client.methods.HttpUriRequest)>",
		"<java.net.URLConnection: java.io.InputStream getInputStream()>",
		//"<java.net.URL: java.net.URLConnection openConnection()>",
		"<java.net.Socket: void connect(java.net.SocketAddress)>",
		"<java.net.Socket: java.io.InputStream getInputStream()>"		
	);
	
	private static final List<String> contentURIs = Arrays.asList(
		""
	);
	
	private static final List<String> intentFilters = Arrays.asList(
		"RECEIVE_SMS"
	);
	
	private static final String contentQueryMethod = "<android.content.ContentResolver: android.database.Cursor query(android.net.Uri,java.lang.String[],java.lang.String,java.lang.String[],java.lang.String)>";
	
	public void internalTransform(String phaseName, Map options) {
		//CHATransformer.v().transform();
		mCallGraph = Scene.v().getCallGraph();
		
		// Problem with virtual functions/abstract classes
		/*
		SootMethod leakMethod = Scene.v().getMethod("<com.utoronto.miwong.leaktest.WebHistoryToWebLeaker: void leak()>");
		Targets temp_targets = new Targets(mCallGraph.edgesOutOf(leakMethod));
		while (temp_targets.hasNext()) {
			MethodOrMethodContext temp = temp_targets.next();
			System.out.println(temp.toString());
		}
		*/
		/*
		SootMethod leakMethod = Scene.v().getMethod(contentQueryMethod);
		
		//Targets temp_targets = new Targets(mCallGraph.edgesOutOf(leakMethod));
		Sources temp_targets = new Sources(mCallGraph.edgesInto(leakMethod));
		System.out.println(leakMethod.toString());
		while (temp_targets.hasNext()) {
			MethodOrMethodContext temp = (SootMethod)temp_targets.next();
			System.out.println(temp.toString());
		}	
		*/
		
		TransitiveTargets trans = new TransitiveTargets(mCallGraph);
		
		List<SootMethod> entries = Scene.v().getEntryPoints();
		
		for (SootMethod entry : entries) {
			System.out.println("Processing entrypoint: " + entry.toString());
			Iterator<MethodOrMethodContext> targets = trans.iterator(entry);
			
			List<MethodOrMethodContext> currentEntry = new ArrayList<MethodOrMethodContext>();
			currentEntry.add((MethodOrMethodContext)entry);
			ReachableMethods reachable = new ReachableMethods(mCallGraph, currentEntry);
			reachable.update();
			
			while (targets.hasNext()) {
				SootMethod current = (SootMethod)targets.next();

				//System.out.println("Possible source: " + current.toString());

				if (sourceAPIs.contains(current.toString())) {
					//System.out.println("Uses source:\t" + current.toString());
					checkTransitiveSources(current, current.toString(), reachable);
				}
				
				//if (current.toString() == sinkAPIs[0][0]) {
				//	System.out.println("Uses sink:\t" + current.toString());
				//}
			}
		}
	}
	
	private void checkTransitiveSources(SootMethod method, String apiSource, ReachableMethods reachable) {
		TransitiveTargets trans = new TransitiveTargets(mCallGraph);
		Sources sources = new Sources(mCallGraph.edgesInto(method));
		
		while (sources.hasNext()) {
			SootMethod source = (SootMethod)sources.next();
			
			if (!reachable.contains(source)) {
				continue;
			}
			
			//Iterator<MethodOrMethodContext> callees = new Targets(mCallGraph.edgesOutOf(source));
			Iterator<MethodOrMethodContext> callees = trans.iterator(source);
			
			while (callees.hasNext()) {
				SootMethod callee = (SootMethod)callees.next();
				
				//System.out.println("Calls: " + callee.toString());
				
				if (sinkAPIs.contains(callee.toString())){
					System.out.println("Leak Detected!");
					System.out.println("Source:" + apiSource);
					System.out.println("Sink:\t" + callee.toString());
					System.out.println("Method:\t" + source.toString());
					System.out.println("");
				}
			}

			checkTransitiveSources(source, apiSource, reachable);
		}
	}
	
	/*
	public void internalTransform(String phaseName, Map options) {
		CHATransformer.v().transform();
		CallGraph cg = Scene.v().getCallGraph();
		
		SootMethod src = Scene.v().getMainClass().getMethodByName("onCreate");
		//SootMethod src = Scene.v().getMethod("<com.utoronto.miwong.leaktest.MainActivity: void leakToSMSDirectly(android.view.View)>");
		//SootMethod src = Scene.v().getMethod("<android.telephony.SmsManager: void sendTextMessage(java.lang.String,java.lang.String,java.lang.String,android.app.PendingIntent,android.app.PendingIntent)>");

		Iterator<MethodOrMethodContext> targets = new Targets(cg.edgesOutOf(src));
		//Iterator<MethodOrMethodContext> targets = new Sources(cg.edgesInto(src));
		//TransitiveTargets trans = new TransitiveTargets(cg);
		//Iterator<MethodOrMethodContext> targets = trans.iterator(src);

		while (targets.hasNext()) {
			SootMethod tgt = (SootMethod)targets.next();
			//System.out.println(src + " may call " + tgt);
			System.out.println(tgt);
		}
	}
	*/
}