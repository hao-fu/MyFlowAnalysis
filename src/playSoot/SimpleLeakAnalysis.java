package playSoot;
/* Soot - a J*va Optimization Framework
 * Copyright (C) 1997-1999 Raja Vallee-Rai
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the
 * Free Software Foundation, Inc., 59 Temple Place - Suite 330,
 * Boston, MA 02111-1307, USA.
 */

/*
 * Modified by the Sable Research Group and others 1997-1999.  
 * See the 'credits' file distributed with Soot for the complete list of
 * contributors.  (Soot is distributed at http://www.sable.mcgill.ca/soot)
 */


import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import soot.Body;
import soot.PackManager;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Transform;
import soot.Value;
import soot.ValueBox;
import soot.jimple.InvokeExpr;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Sources;
import soot.options.Options;

public class SimpleLeakAnalysis
{
	private static final String[] activityCallbacks = {"onCreate", "onStart", "onResume", "onRestart", "onPause", "onStop", "onDestroy"};
	private static final String[] serviceCallbacks = {"onCreate", "onStart", "onResume", "onRestart", "onPause", "onStop", "onDestroy"};	
	
	private String mAppFolder;
	private String mSdkFolder;

	public static final void main(String[] args) throws FileNotFoundException, IOException
	{
		SimpleLeakAnalysis analysis = new SimpleLeakAnalysis();
		analysis.run(args);
	}
	
	public final void run(String[] args) throws FileNotFoundException, IOException
	{		
		if (args.length < 1 || args[0].equals("--help")) {
			System.out.println("Usage: SimpleLeakAnalysis <main class to be analyzed> [options]");
			System.out.println("Required libraries:  soot, android SDK");
			return;
		}
		
		if (args[0].equals("--list") && args.length == 2) {
			SootClass mClass = Scene.v().loadClassAndSupport(args[1]);
			Scene.v().loadNecessaryClasses();
			printClassMethods(mClass);
			return;
		} else {
			mSdkFolder = "/home/michelle/android-sdk-linux";
			mAppFolder = args[0];
			
			String androidLib = mSdkFolder + "/platforms/android-16/android.jar";
			
			if (args.length > 1) {
				for (int i = 1; i < args.length; i += 2) {
					if (args[i].equals("--android-sdk")) {
						mSdkFolder = args[i + 1];
						androidLib = mSdkFolder + "/platforms/android-16/android.jar";
					} 
					else if (args[i].equals("--android-lib")) {
						androidLib = args[i + 1];
					}
					else {
						System.out.println("Invalid argument.");
						return;
					}
				}
			}
			
			// Obtain list of activities and services in application
			List<String> activities = new ArrayList<String>();
			List<String> services = new ArrayList<String>();
			
			try {
				DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
	            DocumentBuilder docBuilder = dbf.newDocumentBuilder();
	            Document manifest = docBuilder.parse(new File(mAppFolder + "//AndroidManifest.xml"));
	            
	            NodeList manifestNode = manifest.getElementsByTagName("manifest");
	            NamedNodeMap manifestAttr = manifestNode.item(0).getAttributes();
	            String packageName = manifestAttr.getNamedItem("package").getNodeValue();
	            
	            NodeList activityNodes = manifest.getElementsByTagName("activity");
	            
	            for (int i = 0; i < activityNodes.getLength(); i++) {
	            	Node activity = activityNodes.item(i);
	            	String activityName = activity.getAttributes().getNamedItem("android:name").getNodeValue();
	            	
	            	if (activityName.startsWith(".")) {
	            		activityName = packageName + activityName;
	            	}
	            	
	            	activities.add(activityName);
	            }
	            
	            NodeList serviceNodes = manifest.getElementsByTagName("service");
	            
	            for (int i = 0; i < serviceNodes.getLength(); i++) {
	            	Node service = serviceNodes.item(i);
	            	String serviceName = service.getAttributes().getNamedItem("android:name").getNodeValue();
	            	
	            	if (serviceName.startsWith(".")) {
	            		serviceName = packageName + serviceName;
	            	}
	            	
	            	services.add(serviceName);
	            }

			} catch (Exception err) {
				System.out.println("Error in obtaining activities: " + err);
			}
			
			// Obtain every possible UI event handler from layout XML file
			List<String> uiCallbacks = new ArrayList<String>();
			
			try {		
				DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
	            DocumentBuilder docBuilder = dbf.newDocumentBuilder();
				File layoutFolder = new File(mAppFolder + "//res/layout");

				for (File layoutFile : layoutFolder.listFiles()) {
					if (layoutFile.getName().endsWith(".xml")) {
			            Document layout = docBuilder.parse(layoutFile);
			            
			            // TODO: add support for other UI elements/events
			            NodeList buttons = layout.getElementsByTagName("Button");
			            
			            for (int i = 0; i < buttons.getLength(); i++) {
			            	Node node = buttons.item(i);
			            	NamedNodeMap nodeAttr = node.getAttributes();
			            	
			            	if (nodeAttr != null) {
			            		Node onclick = nodeAttr.getNamedItem("android:onClick");
			            		if (onclick != null) {
			            			uiCallbacks.add(onclick.getNodeValue());
			            		}
			            	}
			            }
					}
				}
				
			} catch (Exception err) {
				System.out.println("Error in obtaining UI event handlers: " + err);
			}
			
			// Whole-program mode
			// Set class path: classes needed by app and the android.jar file (hard-coded for now)
			String classPath = androidLib;
			//classPath += ":" + mSdkFolder + "/platforms/android-16/data/layoutlib.jar";
			classPath += ":" + mAppFolder + "/bin/classes";
			classPath += ":" + mAppFolder + "/libs";
			
			File libsFolder = new File(mAppFolder + "/libs");
			if (libsFolder != null) {
				for (File libsFile : libsFolder.listFiles()) {
					String path = libsFile.getAbsolutePath();
					
					if (path.endsWith(".jar") || path.endsWith(".zip")) {
						classPath += ":" + path;
					}
				}
			}

			String[] argsList = {"-w",
								 //"-verbose",
								 //"-allow-phantom-refs",
								 //"-full-resolver",
								 //"-f", "jimple",
					 			 //"-p", "cg", "all-reachable",
								 //"-p", "cg", "safe-newinstance",
								 //"-p", "cg", "safe-forname",
								 "-p", "cg", "verbose:true",
								 "-cp", classPath};
			
			Options.v().parse(argsList);
			
			PackManager.v().getPack("wjtp").add(new Transform("wjtp.LeakAnalysis", new LeakAnalysis()));
			
			// Add entry points to scene
			List<SootMethod> entryPoints = new ArrayList<SootMethod>();

			for (String activity : activities) {
				SootClass mainClass = Scene.v().forceResolve(activity, SootClass.BODIES);
				mainClass.setApplicationClass();
				Scene.v().loadNecessaryClasses();
			
				// Add activity callbacks as entry points
				for (String callback : activityCallbacks) {
					if (mainClass.declaresMethodByName(callback)) {
						entryPoints.add(mainClass.getMethodByName(callback));
					}
				}
				
				// Add UI event handlers as entry points
				for (String callback : uiCallbacks) {
					if (mainClass.declaresMethodByName(callback)) {
						entryPoints.add(mainClass.getMethodByName(callback));
					}
				}
				
				// Check for implementers of OnClickListener and add as entrypoints
				// TODO: check for other UI listeners (e.g. OnDragListener, etc.)
				SootClass listenerInterface = Scene.v().getSootClass("android.view.View$OnClickListener");
				List<SootClass> listenerClasses = Scene.v().getActiveHierarchy().getImplementersOf(listenerInterface);
				
				for (SootClass listener : listenerClasses) {
					entryPoints.add(listener.getMethodByName("onClick"));
				}
			}
			
			for (String service: services) {				
				// Add service callbacks as entry points
				SootClass mainClass = Scene.v().forceResolve(service, SootClass.BODIES);
				mainClass.setApplicationClass();
				Scene.v().loadNecessaryClasses();
				
				for (String callback : serviceCallbacks) {
					if (mainClass.declaresMethodByName(callback)) {
						entryPoints.add(mainClass.getMethodByName(callback));
					}
				}
			}
			
			/*
			SootClass contentResolver = Scene.v().forceResolve("android.content.ContentResolver", SootClass.BODIES);
			//System.out.println(contentResolver.getMethods().toString());
			SootMethod queryMethod = Scene.v().getMethod("<android.content.ContentResolver: android.database.Cursor query(android.net.Uri,java.lang.String[],java.lang.String,java.lang.String[],java.lang.String)>");
			List<SootClass> resolverClasses = Scene.v().getActiveHierarchy().getDirectSubclassesOf(contentResolver);
			System.out.println(resolverClasses.size());
			List<SootMethod> queryMethods = Scene.v().getActiveHierarchy().resolveAbstractDispatch(contentResolver, queryMethod);
			System.out.println(queryMethods.size());
			*/
			
			//SootClass leakerClass = Scene.v().getSootClass("com.utoronto.miwong.leaktest.WebHistoryToWebLeaker");
			//leakerClass.setApplicationClass();
			
			Scene.v().setEntryPoints(entryPoints);

			PackManager.v().runPacks();
			//PackManager.v().writeOutput();
		}
	}

	private void printPossibleCallers(SootMethod target) {
		CallGraph cg = Scene.v().getCallGraph();
		Sources sources = new Sources(cg.edgesInto(target));
		while (sources.hasNext()) {
			SootMethod src = (SootMethod)sources.next();
			System.out.println(target + " might be called by " + src);
		}
	}

	/* Doesn't use whole program mode */
	private void printClassMethods(SootClass mclass) {
		System.out.println(mclass.toString());
		//out = new BufferedWriter(new FileWriter(FILE));

		List<SootMethod> methods = mclass.getMethods();
		Iterator<SootMethod> iter = methods.iterator();

		while (iter.hasNext()) {
			SootMethod m = iter.next();
			if (!m.isConcrete()) {
				continue;
			}

			System.out.println("\t" + m.toString());

			Body b = m.retrieveActiveBody();
			Iterator<ValueBox> iter_v = b.getUseBoxes().iterator();
			while (iter_v.hasNext()) {
				Value v = iter_v.next().getValue();

				if (v instanceof InvokeExpr) {
					InvokeExpr iv = (InvokeExpr) v;
					System.out.println("\t\t" + iv.getMethod().toString());
				}
			}
		}
	}
	
	/*
	private void printClassMethodsCallGraph() {
		List<String> argsList = new ArrayList<String>(Arrays.asList("-w", "-main-class"));
		argsList.addAll(activities);

		PackManager.v().getPack("wjtp").add(new Transform("wjtp.myTrans", new SceneTransformer() {

			protected void internalTransform(String phaseName, Map options) {
				CHATransformer.v().transform();
				CallGraph cg = Scene.v().getCallGraph();
				
				//SootMethod src = Scene.v().getMethod("<android.telephony.SmsManager: void sendTextMessage(java.lang.String,java.lang.String,java.lang.String,android.app.PendingIntent,android.app.PendingIntent)>");
				//SootClass a = Scene.v().getSootClass("com.utoronto.miwong.leaktest.MainActivity");
				SootMethod src = Scene.v().getMainClass().getMethodByName("leakToSMSDirectly");

				Iterator<MethodOrMethodContext> targets = new Targets(cg.edgesOutOf(src));
				//Iterator<MethodOrMethodContext> targets = new Targets(cg.edgesInto(src));

				while (targets.hasNext()) {
					SootMethod tgt = (SootMethod)targets.next();
					System.out.println(src + " may call " + tgt);
				}
			}

		}));
		
		args = argsList.toArray(new String[0]);
		soot.Main.main(args);
	}
	*/
}