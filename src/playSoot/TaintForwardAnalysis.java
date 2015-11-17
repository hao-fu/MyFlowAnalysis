/*
 * A simple Taint Forward Analysis
 * @author: Hao
 */

package playSoot;

import java.util.List;
import java.util.Map;

import soot.Body;
import soot.BodyTransformer;
import soot.G;
import soot.Local;
import soot.PackManager;
import soot.Transform;
import soot.Unit;
import soot.Value;
import soot.ValueBox;
import soot.jimple.AssignStmt;
import soot.toolkits.graph.DirectedGraph;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.scalar.ArraySparseSet;
import soot.toolkits.scalar.FlowSet;
import soot.toolkits.scalar.ForwardFlowAnalysis;

public class TaintForwardAnalysis {
	public static void main(String[] args) {
		PackManager.v().getPack("jtp")
				.add(new Transform("jtp.myTransform", new BodyTransformer() {
					protected void internalTransform(Body body, String phase,
							Map options) {
						new TaintForwardVarAnalysis(new ExceptionalUnitGraph(body));
						// use G.v().out instead of System.out so that Soot can
						// redirect this output to the Eclipse console
						G.v().out.println(body.getMethod());
					}

				}));

		soot.Main.main(args);
	}
	
	/*
	 * perform an init var analysis using Soot. 
	 * nature: forward, may
	 * lattice element: the set of possibly uninit vars 
	 */
	public static class TaintForwardVarAnalysis extends
			ForwardFlowAnalysis<Object, Object> {
		
		@SuppressWarnings("unchecked")
		public TaintForwardVarAnalysis(DirectedGraph<?> exceptionalUnitGraph) {
			// use superclass's constructor
			super((DirectedGraph<Object>) exceptionalUnitGraph);
			doAnalysis();
		}

		@Override
		protected void flowThrough(Object in, Object node, Object out) {
			FlowSet inSet = (FlowSet)in, outSet = (FlowSet)out;
			Unit unit = (Unit) node;
			if (!gen(inSet, unit, outSet)) {
				kill(unit, outSet);
			}
		}
		
		/*
		 * rm tainted var who has been assigned value from un-tainted
		 */
		private void kill(Object node, Object out) {
			FlowSet	outSet = (FlowSet)out;
			Unit unit = (Unit)node;
			if (unit instanceof AssignStmt) {
				for (ValueBox defBox : unit.getDefBoxes()) {
					Value value = defBox.getValue();
					if (value instanceof Local && outSet.contains(value)) {
						System.out.println("Kill here! " + unit);
						outSet.remove(value);
					}
				}
			}
		}
		
		/*
		 * add vars possibly tainted 
		 */
		private boolean gen(Object in, Object node, Object out) {
			FlowSet inSet = (FlowSet)in,
					outSet = (FlowSet)out;
			Unit unit = (Unit)node;
			copy(inSet, outSet);
			boolean hasTainted = false;
			if (unit instanceof AssignStmt) {
				// if returned by source()
				if (((AssignStmt) unit).containsInvokeExpr()
						&& unit.toString().contains("source()")) {
					System.out.print("found Source! " + unit);
					addDefBox(unit, outSet);
					hasTainted = true;
				}
				// if x := y, where y is tainted
				for (ValueBox useBox : unit.getUseBoxes()) {
					Value useVal = useBox.getValue();
					if (inSet.contains(useVal)) {
						addDefBox(unit, outSet);
						hasTainted = true;
						break;
					}
				}		
			}
			
			return hasTainted;
		}
		
		private void addDefBox(Unit unit, FlowSet outSet) {
			for (ValueBox defBox : unit.getDefBoxes()) {
				Value value = defBox.getValue();
				if (value instanceof Local) {
					outSet.add(value);
				}
			}
		}

		@Override
		protected void copy(Object src, Object dest) {
			FlowSet srcSet = (FlowSet)src,
					destSet = (FlowSet)dest;
			srcSet.copy(destSet);
		}

		@Override
		protected Object entryInitialFlow() {
			return new ArraySparseSet();
		}

		@Override
		protected void merge(Object in1, Object in2, Object out) {
			FlowSet inSet1 = (FlowSet)in1, 
					inSet2 = (FlowSet)in2,
					outSet = (FlowSet)out;
			inSet1.union(inSet2, outSet);
		}

		@Override
		protected Object newInitialFlow() {
			return new ArraySparseSet();
		}
		
		@SuppressWarnings("unchecked")
		public List<Local> getLiveLocalsAfter(Unit s) {
			// ArraySparseSet returns a unbacked list of elements!
			return ((ArraySparseSet) getFlowAfter(s)).toList();
		}

		@SuppressWarnings("unchecked")
		public List<Local> getLiveLocalsBefore(Unit s) {
			// ArraySparseSet returns a unbacked list of elements!
			return ((ArraySparseSet) getFlowBefore(s)).toList();
		}
	}

}
