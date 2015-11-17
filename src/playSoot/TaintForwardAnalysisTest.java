package playSoot;

import static org.junit.Assert.*;

import java.io.File;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

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

public class TaintForwardAnalysisTest {
	
	// 获得它的函数体
	Body body; 
	// 生成函数的control flow graph
	UnitGraph cfg; 
	TaintForwardAnalysis.TaintForwardVarAnalysis ta;
	String sep;
	
	@Before
	public void setUp() throws Exception {
		String[] args = new String[] {"playSoot.MyClass"};
		
		if (args.length == 0) {
			System.out.println("Usage: java RunLiveAnalysis class_to_analyse");
			System.exit(0);
		}
		
		sep = File.separator;
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
		body = method.retrieveActiveBody();
		// 生成函数的control flow graph
		cfg = new ExceptionalUnitGraph(body);
		// 执行我们的分析
		ta = new TaintForwardAnalysis.TaintForwardVarAnalysis(cfg);
	}


	@Test
	public void test() {
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
