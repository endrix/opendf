/* 
 * Copyright (c) Ericsson AB, 2009
 * Author: Carl von Platen (carl.von.platen@ericsson.com)
 * All rights reserved.
 *
 * License terms:
 *
 * Redistribution and use in source and binary forms, 
 * with or without modification, are permitted provided 
 * that the following conditions are met:
 *     * Redistributions of source code must retain the above 
 *       copyright notice, this list of conditions and the 
 *       following disclaimer.
 *     * Redistributions in binary form must reproduce the 
 *       above copyright notice, this list of conditions and 
 *       the following disclaimer in the documentation and/or 
 *       other materials provided with the distribution.
 *     * Neither the name of the copyright holder nor the names 
 *       of its contributors may be used to endorse or promote 
 *       products derived from this software without specific 
 *       prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND 
 * CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, 
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF 
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE 
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR 
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, 
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT 
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; 
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) 
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN 
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR 
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS 
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package eu.actorsproject.xlim.absint;

import eu.actorsproject.xlim.XlimOperation;
import eu.actorsproject.xlim.dependence.ValueNode;


/**
 * Evaluates constraints that are specific to the Interval domain
 */
public class IntervalConstraintEvaluator extends ConstraintEvaluator<Interval> {

	public IntervalConstraintEvaluator() {
		register("$eq", new EqHandler());
		register("$ge", new GeHandler());
		register("$gt", new GtHandler());
		register("$le", new LeHandler());
		register("$lt", new LtHandler());
		register("$ne", new NeHandler());
	}

	
	@Override
	public Interval getAbstractValue(boolean assertedValue) {
		int valueAsInt=assertedValue? 1 : 0;
		return new Interval(valueAsInt,valueAsInt);
	}


	protected abstract class ConditionHandler extends ConstraintHandler {
		@Override
		public boolean evaluate(XlimOperation xlimOp, 
				                ValueNode node, 
				                Interval aValue, 
				                BagOfConstraints<Interval> bag) {
			ValueNode nodeX=xlimOp.getInputPort(0).getValue();
			ValueNode nodeY=xlimOp.getInputPort(1).getValue();
			
			if (aValue.mayContain(0))
				if (aValue.mayContain(1))
					return true; // no constraint on 'node'
				else
					return refuteCondition(nodeX,nodeY,bag);
			else if (aValue.mayContain(1))
				return assertCondition(nodeX,nodeY,bag);
			else
				return false; // no value possible
		}
		
		protected abstract boolean assertCondition(ValueNode nodeX, 
				                                   ValueNode nodeY, 
				                                   BagOfConstraints<Interval> bag);
		
		protected abstract boolean refuteCondition(ValueNode nodeX, 
                                                   ValueNode nodeY, 
                                                   BagOfConstraints<Interval> bag);
	}
	
	protected class EqHandler extends ConditionHandler {
		protected boolean assertCondition(ValueNode nodeX, 
				                          ValueNode nodeY, 
				                          BagOfConstraints<Interval> bag) {
			Interval x=bag.get(nodeX);
			Interval y=bag.get(nodeY);			
			Interval result=x.intersect(y);
			
			return bag.putConstraint(nodeX,result)
		           && bag.putConstraint(nodeY,result);
		}

		protected boolean refuteCondition(ValueNode nodeX, 
				                          ValueNode nodeY, 
				                          BagOfConstraints<Interval> bag) {
			Interval x=bag.get(nodeX);
			Interval y=bag.get(nodeY);
			
			Interval newX=setDifference(x, y);
			Interval newY=setDifference(y, x);
			
			return bag.putConstraint(nodeX,newX)
	           && bag.putConstraint(nodeY,newY);
		}
		
		private Interval setDifference(Interval i1, Interval i2) {
			if (i1.isEmpty())
				return i1;
			else if (i2.isSingleton()) {
				// i2 = { x }
				long lo1=i1.getLo();
				long hi1=i1.getHi();
				long x=i2.getLo();
				
				if (lo1==x) {
					return new Interval(x+1,hi1);
				}
				else if (hi1==x) {
					return new Interval(lo1,x-1);
				}
			}
			// otherwise: return i1 as is
			return i1;
		}
	}
	
	protected class NeHandler extends EqHandler {
		@Override
		protected boolean assertCondition(ValueNode nodeX, 
                                          ValueNode nodeY, 
                                          BagOfConstraints<Interval> bag) {
			return super.refuteCondition(nodeX, nodeY, bag); 
		}
		
		@Override
		protected boolean refuteCondition(ValueNode nodeX,
                                          ValueNode nodeY, 
                                          BagOfConstraints<Interval> bag) {
			return super.assertCondition(nodeX, nodeY, bag); 
		}
	}
	
	protected abstract class RelopHandler extends ConditionHandler {
		
		protected boolean assertLessThan(ValueNode nodeX, 
				                         ValueNode nodeY, 
				                         BagOfConstraints<Interval> bag) {
			Interval x=bag.get(nodeX);
			Interval y=bag.get(nodeY);
			
			if (x.isEmpty() || y.isEmpty())
				return false;
			
			long yHi=y.getHi()-1;
			long xLo=x.getLo()+1;

			if (yHi<x.getHi()) {
				x=new Interval(x.getLo(), yHi);
				if (bag.putConstraint(nodeX,x)==false)
					return false;
			}
			
			if (xLo>y.getLo()) {
				y=new Interval(xLo, y.getHi());
				if (bag.putConstraint(nodeY,y)==false)
					return false;
			}
			return true;
		}
		
		protected boolean assertLessThanEqual(ValueNode nodeX,
				                              ValueNode nodeY,
				                              BagOfConstraints<Interval> bag) {
			Interval x=bag.get(nodeX);
			Interval y=bag.get(nodeY);
			
			if (x.isEmpty() || y.isEmpty())
				return false;
			
			long xLo=x.getLo();
			long yHi=y.getHi();
			
			if (yHi<x.getHi()) {
				x=new Interval(xLo,yHi);
				if (bag.putConstraint(nodeX,x)==false)
					return false;
			}
			
			if (xLo>y.getLo()) {
				y=new Interval(xLo, yHi);
				if (bag.putConstraint(nodeY,y)==false)
					return false;
			}
			return true;
		}
	}
	
	protected class LtHandler extends RelopHandler {
		@Override
		protected boolean assertCondition(ValueNode nodeX, 
				                          ValueNode nodeY, 
				                          BagOfConstraints<Interval> bag) {
			return assertLessThan(nodeX,nodeY,bag);
		}

		@Override
		protected boolean refuteCondition(ValueNode nodeX, 
				                          ValueNode nodeY, 
				                          BagOfConstraints<Interval> bag) {
			// not(x<y) same as (y<=x)
			return assertLessThanEqual(nodeY,nodeX,bag);
		}
	}

	protected class LeHandler extends RelopHandler {
		@Override
		protected boolean assertCondition(ValueNode nodeX, 
				                          ValueNode nodeY, 
				                          BagOfConstraints<Interval> bag) {
			return assertLessThanEqual(nodeX,nodeY,bag);
		}

		@Override
		protected boolean refuteCondition(ValueNode nodeX, 
				                          ValueNode nodeY, 
				                          BagOfConstraints<Interval> bag) {
			// not(x<=y) same as (y<x)
			return assertLessThan(nodeY,nodeX,bag);
		}
	}

	protected class GtHandler extends RelopHandler {
		@Override
		protected boolean assertCondition(ValueNode nodeX, 
				                          ValueNode nodeY, 
				                          BagOfConstraints<Interval> bag) {
			// (x>y) same as (y<x)
			return assertLessThan(nodeY,nodeX,bag);
		}

		@Override
		protected boolean refuteCondition(ValueNode nodeX, 
				                          ValueNode nodeY, 
				                          BagOfConstraints<Interval> bag) {
			// not(x>y) same as (x<=y)
			return assertLessThanEqual(nodeX,nodeY,bag);
		}
	}

	protected class GeHandler extends RelopHandler {
		@Override
		protected boolean assertCondition(ValueNode nodeX, 
				                          ValueNode nodeY, 
				                          BagOfConstraints<Interval> bag) {
			// (x>=y) same as (y<=x)
			return assertLessThanEqual(nodeY,nodeX,bag);
		}

		@Override
		protected boolean refuteCondition(ValueNode nodeX, 
				                          ValueNode nodeY, 
				                          BagOfConstraints<Interval> bag) {
			// not(x>=y) = (x<y)
			return assertLessThan(nodeX,nodeY,bag);
		}
	}
}
