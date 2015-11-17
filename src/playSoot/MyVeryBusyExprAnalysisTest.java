/*
 * Warning:
 * This can only be executed correctly when using SOOTCLASSES　
 * and put the /bin/ as External Class folder suggested as 
 * http://stackoverflow.com/questions/20282481/loading-java-class-files-for-soot-dynamically-in-eclipse.
 * Not working anymore if use soot-trunk.jar as lib
 */


package playSoot;
import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import soot.Body;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.scalar.FlowSet;


public class MyVeryBusyExprAnalysisTest {
	
	/*
	 * 执行我们写的分析
	 */
	@Before
	public void setUp() throws Exception {
		// 载入MyClass类
		SootClass tgtClass = Scene.v().loadClassAndSupport("playSoot.MyClass");
		// 把它作为我们要分析的类
		tgtClass.setApplicationClass();
		// 找到它的myMethod函数
		SootMethod method = tgtClass.getMethodByName("myMethod");
		// 获得它的函数体
		Body body = method.retrieveActiveBody();
		// 生成函数的cfg
		UnitGraph cfg = new ExceptionalUnitGraph(body);
		// 执行我们的分析
		MyVeryBusyExprAnalysis.VeryBusyExprAnalysis an = new MyVeryBusyExprAnalysis.VeryBusyExprAnalysis(cfg);
		// iterate over the results
		for (Unit unit: cfg) {
			FlowSet in = (FlowSet) an.getFlowBefore(unit);
			FlowSet out = (FlowSet) an.getFlowAfter(unit);
		}
				
	}

	@Test
	public void test() {
	}

}
