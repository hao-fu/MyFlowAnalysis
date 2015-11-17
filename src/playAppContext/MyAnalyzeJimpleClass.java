package playAppContext;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.List;
import java.util.Set;

import soot.MethodOrMethodContext;
import soot.PackManager;
import soot.Scene;
import soot.SceneTransformer;
import soot.SootClass;
import soot.SootMethod;
import soot.Transform;
import soot.jimple.infoflow.android.AnalyzeJimpleClass;
import soot.jimple.infoflow.android.data.AndroidMethod;
import soot.jimple.infoflow.data.SootMethodAndClass;
import soot.jimple.toolkits.callgraph.ReachableMethods;
import soot.util.HashMultiMap;
import soot.util.MultiMap;

public class MyAnalyzeJimpleClass extends AnalyzeJimpleClass {
	// 用于存储已找到的callback接口
	Set<SootClass> callbackClasses = new HashSet<>();
	// 从自定义的callBacklist.txt读取我们所列的所有call back接口
	private final Set<String> androidCallbacks;
	// 存储当前需要处理的callback
	private MultiMap<String, SootMethodAndClass> callbackWorklist;
	// 存储已经遇到过得callback函数, <所在类， 函数signature>
	private final Map<String, Set<SootMethodAndClass>> callbackMethods =
			new HashMap<String, Set<SootMethodAndClass>>();

	public MyAnalyzeJimpleClass(Set<String> entryPointClasses) throws IOException {
		super(entryPointClasses);
		//this.config = config;
		//this.entryPointClasses = entryPointClasses;
		this.androidCallbacks = loadAndroidCallbacks();
	}
	
	private Set<String> loadAndroidCallbacks() throws IOException {
		Set<String> androidCallbacks = new HashSet<String>();
		String fileName = "AndroidCallbacks.txt";
		// read callbacks from the file
		return androidCallbacks;
	}

	public void collectCallBackMethodsIncremental() {
		Transform transform = new Transform("wjtp.ajc", new SceneTransformer() {
			@Override
			protected void internalTransform(String phaseName,
					Map<String, String> options) {
				// worklist algorithm
				// Process the worklist from last time: 来源只有worklist
				MultiMap<String, SootMethodAndClass> workListCopy = 
						new HashMultiMap<String, SootMethodAndClass>(callbackWorklist);
				// iterate class name (keys) through workListCopy
				for (String className : workListCopy.keySet()) {
					// 明明是method的集合， 却叫做entryClasses, 注意与entryPointsClasses区分
					List<MethodOrMethodContext> entryClasses = new LinkedList<MethodOrMethodContext>();
					// iterate methods within the class
					for (SootMethodAndClass am : workListCopy.get(className)) {
						entryClasses.add(Scene.v().getMethod(am.getSignature()));
					}
					// 每次worklist更新点在此：entryClasses/worklist（即寻找可抵达函数的入口函数）在不断推进
					// 直到达到fix-point
					analyzeRechableMethods(Scene.v().getSootClass(className), entryClasses);
					callbackWorklist.remove(className);
				}
			}		
		});
		// 加入到Soot的phase里, 一会执行
		PackManager.v().getPack("wjtp").add(transform);
	}
	
	private void analyzeRechableMethods(SootClass lifecycleElement, List<MethodOrMethodContext> methods) {
		// 传入的entryClasses作为入口函数，通过BFS call graph的方法查找所有入口函数可抵达的函数
		ReachableMethods rm = new ReachableMethods(Scene.v().getCallGraph(), methods);
		rm.update();
		
		// 遍历找出来的可抵达函数
		// Scan for listeners in the class hierarchy
		Iterator<MethodOrMethodContext> reachableMethods = rm.listener();
		while (reachableMethods.hasNext()) {
			SootMethod method = reachableMethods.next().method();
			//Analyzes the given method and looks for callback registrations
			analyzeMethodForCallbackRegistrations(lifecycleElement, method);
			//analyzeMethodForDynamicBroadcastReceiver(method);
		}
	}
	
	private void analyzeMethodForCallbackRegistrations(SootClass lifecycleElement, SootMethod method){
		// 找出method中实现的callback接口，加入到callbackClasses中
		// 对每个callBackClasees
		// Analyze all found callback classes
		for (SootClass callbackClass : callbackClasses)
			//Analyzes the given class to find callback methods
			analyzeClass(callbackClass, lifecycleElement);
	}
	
	private void analyzeClass(SootClass sootClass, SootClass lifecycleElement) {
		// Do not analyze system classes
		
		// Check for callback handlers implemented via interfaces
		analyzeClassInterfaceCallbacks(sootClass, sootClass, lifecycleElement);
	}
	
	private void analyzeClassInterfaceCallbacks(SootClass baseClass, SootClass sootClass,
			SootClass lifecycleElement) {
		// 先通过baseClass排除不可能条件
		
		// Do we implement one of the well-known interfaces?
				//for (SootClass i : collectAllInterfaces(sootClass)) {
					//if (androidCallbacks.contains(i.getName()))
						//for (SootMethod sm : i.getMethods())
							//checkAndAddMethod(getMethodFromHierarchyEx(baseClass,
									//sm.getSubSignature()), lifecycleElement);
				//}
	}
	
	private void checkAndAddMethod(SootMethod method, SootClass baseClass) {
		AndroidMethod am = new AndroidMethod(method);
		boolean isNew;
		if (this.callbackMethods.containsKey(baseClass.getName()))
			// 用.add时会判断要加入的是否在callbackMethods里，并返回结果
			// 函数在所在的类是否已经出现在了callbackMethod，不在isNew为true
			isNew = this.callbackMethods.get(baseClass.getName()).add(am);
		else {
			Set<SootMethodAndClass> methods = new HashSet<SootMethodAndClass>();
			isNew = methods.add(am);
			this.callbackMethods.put(baseClass.getName(), methods);
		}
		
		// 如果method原先不在callbackMethods, 把它加入callbakWorklist里
		if (isNew)
			if (this.callbackWorklist.containsKey(baseClass.getName()))
					this.callbackWorklist.get(baseClass.getName()).add(am);
			else {
				Set<SootMethodAndClass> methods = new HashSet<SootMethodAndClass>();
				isNew = methods.add(am);
				this.callbackWorklist.putAll(baseClass.getName(), methods);
			}
	}

}
