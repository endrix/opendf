/**
 * 
 */
package eu.actorsproject.xlim.schedule;

import java.util.HashMap;
import java.util.Map;

import eu.actorsproject.xlim.XlimInputPort;
import eu.actorsproject.xlim.XlimOperation;
import eu.actorsproject.xlim.XlimStateVar;
import eu.actorsproject.xlim.absint.DemandContext;
import eu.actorsproject.xlim.absint.Evaluator;
import eu.actorsproject.xlim.absint.Interval;
import eu.actorsproject.xlim.absint.IntervalDomain;
import eu.actorsproject.xlim.absint.IntervalWidening;
import eu.actorsproject.xlim.absint.LinearExpression;
import eu.actorsproject.xlim.absint.LinearExpressionDomain;
import eu.actorsproject.xlim.dependence.DependenceSlice;
import eu.actorsproject.xlim.dependence.Location;
import eu.actorsproject.xlim.dependence.PhiOperator;
import eu.actorsproject.xlim.dependence.TestOperator;
import eu.actorsproject.xlim.dependence.ValueNode;
import eu.actorsproject.xlim.dependence.ValueOperator;
import eu.actorsproject.xlim.util.OperationHandler;
import eu.actorsproject.xlim.util.OperationPlugIn;

/**
 * Determines a collection of (Interval) widening operators given
 * a) the set of variables (ValueNodes) that are relevant to the scheduling decisions
 * b) the tests performed in the decision tree
 * c) possible tests performed in the actions
 * d) initial values and constant assignments performed in actions (and state updates in action nodes)
 */
public class WideningGenerator {

	private IntervalDomain mIntervalDomain;
	private Map<ValueNode, IntervalWidening> mWideningOperators = new HashMap<ValueNode, IntervalWidening>();
	private LinearExpressionDomain mLinearExpressionDomain = new LinearExpressionDomain(new Evaluator());
	private DemandContext<LinearExpression> mLinearExpressions = new DemandContext<LinearExpression>(mLinearExpressionDomain);
	private OperatorVisitor mOperatorVisitor = new OperatorVisitor();
	private boolean mTrace=false;
	
	public WideningGenerator(Iterable<ValueNode> variables, IntervalDomain domain) {
		mIntervalDomain = domain;
		//mLinearExpressionDomain.setTrace(mTrace);
		
		for (ValueNode node: variables) {
			Location location=node.getLocation();
			LinearExpression linexp=null;
			
			if (location==null || location.hasSource()) {
				// Not an actor port
				IntervalWidening w=new IntervalWidening(node.getType());

				if (location!=null) {
					XlimStateVar stateVar=location.getSource().asStateVar();
					if (stateVar!=null) {
						addInitialValue(stateVar, w);
					}
				}
	
				if (mTrace) {
					String name=(location!=null)? (location.getDebugName() + " (node "+node.getUniqueId()+")") : node.getUniqueId();
					System.out.println("Initial widening operator of "+name+": "+w);
				}
				mWideningOperators.put(node, w);
				linexp=new LinearExpression(node);
			}
			
			mLinearExpressions.put(node, linexp);
		}
	}
	
	private void addInitialValue(XlimStateVar stateVar, IntervalWidening w) {
		Interval aValue=mIntervalDomain.initialState(stateVar.asStateLocation());
		w.addStartPoint(aValue.getLo());
		w.addEndPoint(aValue.getHi());
	}
	
	public Map<ValueNode,IntervalWidening> getWideningOperators() {
		return mWideningOperators;
	}
	
	public void processDecisionSlice(DependenceSlice decision) {
		for (ValueOperator op: decision.getValueOperators()) {
			op.evaluate(mLinearExpressions, mLinearExpressionDomain);
			op.accept(mOperatorVisitor, null);
		}
		
		if (mTrace) {
			System.out.println("After processDecisionSlice:");
			printWideningOperators();
		}
	}
	
	private void printWideningOperators() {
	
		for (Map.Entry<ValueNode, IntervalWidening> entry: mWideningOperators.entrySet()) {
			ValueNode node=entry.getKey();
			IntervalWidening w=entry.getValue();
			Location location=node.getLocation();
			String name=(location!=null)? (location.getDebugName() + " (node "+node.getUniqueId()+")") : node.getUniqueId();
			
			System.out.println("Widening operator of "+name+": "+w);
		}
	}
	private class OperatorVisitor extends OperationPlugIn<Handler> implements ValueOperator.Visitor<Object,Object> {

		public OperatorVisitor() {
			super(new Handler());
			
			Handler eqHandler=new EqHandler();
			registerHandler("$eq", eqHandler);
			registerHandler("$ne", eqHandler);
			
			Handler ltHandler=new LtHandler();
			registerHandler("$lt", ltHandler);
			registerHandler("$ge", ltHandler);
			
			Handler gtHandler=new GtHandler();
			registerHandler("$gt", gtHandler);
			registerHandler("$le", gtHandler);
		}

		@Override
		public Object visitOperation(XlimOperation xlimOp, Object dummy) {
			Handler handler=getOperationHandler(xlimOp);
			handler.visit(xlimOp);
			return null;
		}

		@Override
		public Object visitPhi(PhiOperator phi, Object dummy) {
			return null;
		}

		@Override
		public Object visitTest(TestOperator test, Object dummy) {
			return null;
		}
	}
	
	// Default handler
	private class Handler implements OperationHandler {

		@Override
		public boolean supports(XlimOperation op) {
			return true;
		}
		
		void visit(XlimOperation xlimOp) {
		}		
	}
	
	private abstract class RelopHandler extends Handler {
	
		@Override
		void visit(XlimOperation xlimOp) {
			LinearExpression linexp1=getLinearExpression(xlimOp.getInputPort(0));
			LinearExpression linexp2=getLinearExpression(xlimOp.getInputPort(1));
			
			if (mTrace) {
				System.out.println(xlimOp);
		
				String aValue=(linexp1!=null)? linexp1.toString() : "null";
				System.out.println(xlimOp.getInputPort(0).getValue().getUniqueId()+": "+aValue);
				
				aValue=(linexp2!=null)? linexp2.toString() : "null";
				System.out.println(xlimOp.getInputPort(1).getValue().getUniqueId()+": "+aValue);
			}
			
			if (linexp1!=null && linexp2!=null) {
				if (linexp2.isConstant()) {
					if (!linexp1.isConstant()) {
						// scale*x + offset relop constant  iff  scale*x relop constant-offset 
						long scale=linexp1.getScale();
						long numerator=linexp2.getConstant()-linexp1.getOffset();
						IntervalWidening w=mWideningOperators.get(linexp1.getVariable());
						boolean reverseRelation=(scale<0);
						
						if (w==null) {
							System.out.println(linexp1);
						}
						visit(w, numerator, scale, reverseRelation);
					}
				}
				else if (linexp1.isConstant()) {
					// constant relop scale*x + offset   iff  constant-offset relop scale*x
					long scale=linexp2.getScale();
					long numerator=linexp1.getConstant()-linexp2.getOffset();
					IntervalWidening w=mWideningOperators.get(linexp2.getVariable());
					boolean reverseRelation=(scale>0);
					
					if (w==null) {
						System.out.println(linexp1);
					}
					visit(w, numerator, scale, reverseRelation);
				}
			}
		}
	
		private LinearExpression getLinearExpression(XlimInputPort in) {
			ValueNode node=in.getValue();
			return mLinearExpressions.get(node);
		}
		
		// does x relop numerator/scale (or numerator/scale relop x, if reverseRelation is true)
		abstract void visit(IntervalWidening w, long numerator, long scale, boolean reverseRelation);
		
		
		// variable < numerator/scale, which can be tightened by rounding upwards
		
		void visitLt(IntervalWidening w, long numerator, long scale) {
			boolean changed=w.addStartPoint(LinearExpression.divCeil(numerator, scale));
			
			if (mTrace) {
				String comment=changed? "" : " (not updated)";
				System.out.println("Widening operator: "+w+comment);
			}
		}
		
		// variable > numerator/scale, which can be tightened by rounding downwards
		
		void visitGt(IntervalWidening w, long numerator, long scale) {
			boolean changed=w.addEndPoint(LinearExpression.divFloor(numerator, scale));
			
			if (mTrace) {
				String comment=changed? "" : " (not updated)";
				System.out.println("Widening operator: "+w+comment);
			}
		}		
	}
	
	// Handler of $eq and $ne
	private class EqHandler extends RelopHandler {
		
		@Override
		void visit(IntervalWidening w, long numerator, long scale, boolean reverseRelation) {
			boolean changed=false;
			
			if (numerator % scale == 0) {
				// It is only possible to find a point x such that 
				//   scale*x  == numerator
				// if scale divides numerator
				changed = w.addConstant(numerator/scale);
			}
			
			if (mTrace) {
				String comment=changed? "" : " (not updated)";
				System.out.println("Widening operator: "+w+comment);
			}
		}		
	}
	
	private class LtHandler extends RelopHandler {

		// does x < numerator/scale (or numerator/scale < x)
		
		@Override
		void visit(IntervalWidening w, long numerator, long scale, boolean reverseRelation) {
			if (!reverseRelation)
				visitLt(w, numerator, scale);
			else
				visitGt(w, numerator, scale); // becomes x > numerator/scale when scale < 0
		}
	}
	
	private class GtHandler extends RelopHandler {

		// does x > numerator/scale  (or numerator/scale > x) 
		
		@Override
		void visit(IntervalWidening w, long numerator, long scale, boolean reverseRelation) {
			if (!reverseRelation)
				visitGt(w, numerator, scale);
			else
				visitLt(w, numerator, scale);  // becomes x < numerator/scale when scale < 0
		}
	}
}
