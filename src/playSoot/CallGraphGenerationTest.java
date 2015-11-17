/*
 * Modified from https://mailman.cs.mcgill.ca/pipermail/soot-list/2009-February/002241.html
 */

package playSoot;

import java.util.Map;

import org.junit.Test;

import soot.G;
import soot.Main;
import soot.PackManager;
import soot.Scene;
import soot.SceneTransformer;
import soot.Transform;
import soot.jimple.toolkits.callgraph.CallGraph;

public class CallGraphGenerationTest {

	@Test
	public void testCallGraphGeneration() throws Exception {
		G.reset();
		
		CallGraphFetcher callGraphFetcher = new CallGraphFetcher();
		PackManager.v().getPack("wjtp").add(new Transform("wjtp.cgfetcher", 
callGraphFetcher));
		
		//String args = "-w " + OmgClass.class.getName() + " -v";
		String args = "-w " + MyClass.class.getName() + " -v";
		System.out.println("CallGraphGenerationTest.testCallGraphGeneration() invoking soot with "+args);
		Main.main(args.split(" "));
	}
	
	private static class CallGraphFetcher extends SceneTransformer {

		private CallGraph callGraph;

		@Override
		protected void internalTransform(String phaseName, Map options) {
			System.out.println("CallGraphFetcher.internalTransform() fetching call graph from scene");
			this.callGraph = Scene.v().getCallGraph();
		}
		
		public CallGraph getCallGraph() {
			return callGraph;
		}
	}
	
	private static class OmgClass {
		
		private int a;
		private int b;
		@SuppressWarnings("unused")
		private int stop;
		
		public OmgClass(int a, int b) {
			this.a = a;
			this.b = b;
		}
		
		public void omfg(int c) {
			for (int i=a; i<c; i++) {
				System.out.println(c+b);
				omfg(--c);
			}
		}
		
		public static void main(String[] args){
			OmgClass it = new OmgClass(1, 10);
			it.omfg(6);
		}
	}
}
