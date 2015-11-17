/*
 * Warning:
 * This can only be executed correctly when using SOOTCLASSES　
 * and put the /bin/ as External Class folder suggested as 
 * http://stackoverflow.com/questions/20282481/loading-java-class-files-for-soot-dynamically-in-eclipse.
 * Not working anymore if use soot-trunk.jar as lib
 */

package playSoot;

import java.io.File;
import java.util.List;

import soot.Body;
import soot.Local;
import soot.NormalUnitPrinter;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.UnitPrinter;
import soot.options.Options;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.scalar.FlowSet;

public class MyMain {
	public static void main(String[] args) {
		args = new String[] {"playSoot.MyClass"};
		
		if (args.length == 0) {
			System.out.println("Usage: java RunLiveAnalysis class_to_analyse");
			System.exit(0);
		}
		
		String sep = File.separator;
		String pathSep = File.pathSeparator;
		String path = System.getProperty("java.home") + sep + "lib" + sep
				+ "rt.jar";
		path += pathSep + "." + sep + "bin";
		Options.v().set_soot_classpath(path);
		
		// 载入MyClass类
		SootClass tgtClass = Scene.v().loadClassAndSupport(args[0]);
		// 把它作为我们要分析的类
		tgtClass.setApplicationClass();
		Scene.v().loadNecessaryClasses();
		// 找到它的myMethod函数
		SootMethod method = tgtClass.getMethodByName("testTaintForwardVar");
		// 获得它的函数体
		Body body = method.retrieveActiveBody();
		// 生成函数的control flow graph
		UnitGraph cfg = new ExceptionalUnitGraph(body);
		// 执行我们的分析
		TaintForwardAnalysis.TaintForwardVarAnalysis ta = new TaintForwardAnalysis.TaintForwardVarAnalysis(cfg);
		// iterate over the results
		for (Unit unit : cfg) {
			//System.out.println(unit);
			List<Local> before = ta.getLiveLocalsBefore(unit);
			List<Local> after = ta.getLiveLocalsAfter(unit);
			UnitPrinter up = new NormalUnitPrinter(body);
			up.setIndent("");
			
			System.out.println("---------------------------------------");		
			unit.toString(up);			
			System.out.println(up.output());
			if (!before.isEmpty()) {
				if (unit.toString().contains("sink")) {
					System.out.println("found a sink!");
				}
			}
			System.out.print("Taint in: {");
			sep = "";
			for (Local l : before) {
				System.out.print(sep);
				System.out.print(l.getName() + ": " + l.getType());
				sep = ", ";
			}
			System.out.println("}");
			System.out.print("Taint out: {");
			sep = "";
			for (Local l : after) {
				System.out.print(sep);
				System.out.print(l.getName() + ": " + l.getType());
				sep = ", ";
			}			
			System.out.println("}");			
			System.out.println("---------------------------------------");
		}
	}

}
