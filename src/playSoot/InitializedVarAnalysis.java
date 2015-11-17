package playSoot;

import java.util.HashSet;
import java.util.Map;

import playSoot.MyVeryBusyExprAnalysis.VeryBusyExprAnalysis;
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
import soot.jimple.DefinitionStmt;
import soot.toolkits.graph.DirectedGraph;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.scalar.ArraySparseSet;
import soot.toolkits.scalar.BackwardFlowAnalysis;
import soot.toolkits.scalar.FlowSet;
import soot.toolkits.scalar.ForwardFlowAnalysis;

public class InitializedVarAnalysis {
	public static void main(String[] args) {
		PackManager.v().getPack("jtp")
				.add(new Transform("jtp.myTransform", new BodyTransformer() {
					protected void internalTransform(Body body, String phase,
							Map options) {
						new InitVarAnalysis(new ExceptionalUnitGraph(body));
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
	public static class InitVarAnalysis extends
			ForwardFlowAnalysis<Object, Object> {
		
		@SuppressWarnings("unchecked")
		public InitVarAnalysis(DirectedGraph<?> exceptionalUnitGraph) {
			// use superclass's constructor
			super((DirectedGraph<Object>) exceptionalUnitGraph);
			doAnalysis();
		}

		/*
		 * 
		 * @see soot.toolkits.scalar.FlowAnalysis#flowThrough(java.lang.Object, java.lang.Object, java.lang.Object)
		 */
		@Override
		protected void flowThrough(Object in, Object node, Object out) {
			FlowSet inSet = (FlowSet)in, outSet = (FlowSet)out;
			Unit unit = (Unit) node;
			kill(inSet, unit, outSet);
			gen(outSet, unit);
		}
		
		/*
		 * rm var who has been assigned value
		 */
		private void kill(Object in, Object node, Object out) {
			FlowSet inSet = (FlowSet)in, outSet = (FlowSet)out;
			Unit unit = (Unit)node;
			outSet = inSet.clone();
			if (unit instanceof AssignStmt) {
				for (ValueBox defBox : unit.getDefBoxes()) {
					Value value = defBox.getValue();
					if (value instanceof Local) {
						outSet.remove(value);
					}
				}
			}
		}
		
		/*
		 * add var declared
		 */
		private void gen(Object out, Object node) {
			FlowSet outSet = (FlowSet)out;
			Unit unit = (Unit)node;
			if (unit instanceof DefinitionStmt && 
					!(unit instanceof AssignStmt)) {
				for (ValueBox defBox : unit.getDefBoxes()) {
					Value value = defBox.getValue();
					if (value instanceof Local) {
						outSet.add(value);
					}
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
	}

}
