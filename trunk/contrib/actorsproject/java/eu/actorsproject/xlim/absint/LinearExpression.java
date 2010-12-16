/**
 * 
 */
package eu.actorsproject.xlim.absint;

import java.util.Collections;

import eu.actorsproject.util.XmlAttributeFormatter;
import eu.actorsproject.util.XmlElement;
import eu.actorsproject.xlim.XlimType;
import eu.actorsproject.xlim.dependence.Location;
import eu.actorsproject.xlim.dependence.ValueNode;

/**
 * Represents integer linear expressions (mOffset + mScale*v) in some value v, where the linear relation
 * holds for v:s in the interval [mMin, mMax].
 * 
 * As a special case it can also represent constant values (in which case mScale=0)
 */
public class LinearExpression implements Cloneable, AbstractValue<LinearExpression> {

	private long mOffset;
	private long mScale;
	private ValueNode mVariable;
	private long mMin;
	private long mMax;
	
	/**
	 * Creates a "basic" LinearExpression, which simply is 0 + 1*variable, 
	 * defined over the entire range of the variable.
	 *  
	 * @param variable  a ValueNode
	 */
	public LinearExpression(ValueNode variable) {
		mOffset = 0;
		mScale = 1;
		mVariable = variable;
		
		XlimType type = variable.getType();
		mMin = type.minValue();
		mMax = type.maxValue();
	}
	
	public LinearExpression(long constant) {
		mOffset = constant;
	}
	
	protected LinearExpression(long offset, long scale, ValueNode variable, long min, long max) {
		mOffset = offset;
		mScale = scale;
		mVariable = variable;
		mMin = min;
		mMax = max;
	}

	protected LinearExpression create(long offset, long scale, ValueNode variable, long min, long max) {
		if (min<=max) {
			if (scale==0) {
				if (offset==mOffset && mScale==0)
					return this;
				variable=null;
			}
			else
				assert(variable!=null);
			
			if (offset==mOffset && scale==mScale && variable==mVariable && min==mMin && max==mMax)
				return this;
			else
				return new LinearExpression(offset, scale, variable, min, max);
		}
		else
			return null;
	}
	
	@Override
	public LinearExpression clone() {
		return new LinearExpression(mOffset, mScale, mVariable, mMin, mMax);
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof LinearExpression) {
			LinearExpression e=(LinearExpression) obj;
			if (mOffset==e.mOffset && mScale==e.mScale) {
				return mScale==0
					   || mVariable==e.mVariable && mMin==e.mMin && mMax==e.mMax;
			}
		}
		return false;
	}
	
	@Override
	public int hashCode() {
		int h=(int) mOffset;
		
		if (mScale!=0) {
		  h += 1031*(int) mScale;
		  h += 4789*mVariable.hashCode();
		  h += 1089*(int) mMin;
		  h += 7253*(int) mMax;
		}
		
		return h;
	}

	/**
	 * @return true if the LinearExpression is constant/doesn't depend on a variable
	 */
	public boolean isConstant() {
		return (mScale==0);
	}

	/**
	 * @return the value of a constant LinearExpression (see isConstant())
	 */
	public long getConstant() {
		return mOffset;
	}
	
	/**
	 * @return the offset of a LinearExpression: offset + scale*x
	 */
	public long getOffset() {
		return mOffset;
	}
	
	/**
	 * @return the scale factor of a LinearExpression: offset + scale*x
	 */
	public long getScale() {
		return mScale;
	}
	
	/**
	 * @return the ValueNode, which represents the variable (x) of a LinearExpression: offset + scale*x
	 */
	public ValueNode getVariable() {
		return mVariable;
	}
	
	/**
	 * @return the minimum value of the variable (x) for which the LienarExpression (offset + scale*x) is valid
	 */
	public long getMinimumVariableValue() {
		return mMin;
	}

	/**
	 * @return the maximum value of the variable (x) for which the LienarExpression (offset + scale*x) is valid
	 */
	public long getMaximumVariableValue() {
		return mMax;
	}
	
	@Override
	public LinearExpression getAbstractValue() {
		return this;
	}

	@Override
	public boolean isEmpty() {
		return false;
	}

	/**
	 * @param aValue
	 * @return the union of this abstract value and aValue
	 * union() is used as confluence operator at joining flow paths,
	 * that is the result of two joining values is aValue1.union(aValue2)
	 */
	@Override
	public AbstractValue<LinearExpression> union(LinearExpression aValue) {
		// widening the defined interval of an expression is never useful,
		// we should thus do intersect at joining paths
		return intersect(aValue);
	}

	@Override
	public AbstractValue<LinearExpression> intersect(LinearExpression aValue) {
		if (mOffset==aValue.mOffset && mScale==aValue.mScale) {
			if (mScale==0) {
				return this;
			}
			else if (mVariable==aValue.mVariable){
				return create(mOffset, mScale, mVariable, Math.max(mMin,aValue.mMin), Math.min(mMax,aValue.mMax));
			}
		}
		return null;
	}

	protected long minValue() {
		if (mScale==0)
			return mOffset;
		else if (mScale>0)
			return mOffset + mMin*mScale;
		else
			return mOffset + mMax*mScale;
	}
	
	protected long maxValue() {
		if (mScale==0)
			return mOffset;
		else if (mScale>0)
			return mOffset + mMax*mScale;
		else
			return mOffset + mMin*mScale;
	}
	
	@Override
	public boolean mayContain(long constant) {
		if (isConstant())
			return getConstant()==constant;
		else
			return true;
	}

	@Override
	public AbstractValue<LinearExpression> add(LinearExpression aValue) {
		if (aValue.isConstant()) {
			return create(mOffset + aValue.getConstant(), mScale, mVariable, mMin, mMax);
		}
		else if (isConstant()) {
			return aValue.add(this);
		}
		else if (mVariable==aValue.mVariable) {
			long min = Math.max(mMin, aValue.getMinimumVariableValue());
			long max = Math.min(mMax, aValue.getMaximumVariableValue());
			
			return create(mOffset + aValue.getOffset(), mScale+aValue.getScale(), mVariable, min, max);
		}
		else
			return null;
	}

	@Override
	public AbstractValue<LinearExpression> subtract(LinearExpression aValue) {
		if (aValue.isConstant()) {
			return create(mOffset - aValue.getConstant(), mScale, mVariable, mMin, mMax);
		}
		else if (isConstant()) {
			return create(mOffset - aValue.getOffset(), aValue.getScale(), aValue.getVariable(), 
					      aValue.getMinimumVariableValue(), aValue.getMaximumVariableValue());
		}
		else if (mVariable==aValue.mVariable) {
			long min = Math.max(mMin, aValue.getMinimumVariableValue());
			long max = Math.min(mMax, aValue.getMaximumVariableValue());
			
			return create(mOffset - aValue.getOffset(), mScale-aValue.getScale(), mVariable, min, max);
		}
		else
			return null;
	}

	@Override
	public AbstractValue<LinearExpression> negate() {
		return create(-mOffset, -mScale, mVariable, mMin, mMax);
	}

	@Override
	public AbstractValue<LinearExpression> multiply(LinearExpression aValue) {
		if (aValue.isConstant()) {
			long k=aValue.getConstant();
			return create(mOffset*k, mScale*k, mVariable, mMin, mMax);
		}
		else if (isConstant()) {
			return aValue.multiply(this);
		}
		else
			return null;
	}

	@Override
	public AbstractValue<LinearExpression> divide(LinearExpression aValue) {
		if (aValue.isConstant()) {
			long k=aValue.getConstant();
			
			if (k==1) {
				return this;
			}
			else if (k==-1) {
				return negate();
			}
			else if (k!=0 && isConstant()) {
				return new LinearExpression(getConstant()/k);
			}
		}
		return null;
	}

	@Override
	public AbstractValue<LinearExpression> shiftLeft(LinearExpression aValue) {
		if (aValue.isConstant()) {
			long k = 1L << aValue.getConstant();
			
			return create(mOffset*k, mScale*k, mVariable, mMin, mMax);
		}
		else
			return null;
	}

	@Override
	public AbstractValue<LinearExpression> shiftRight(LinearExpression aValue) {
		if (aValue.isConstant() && aValue.getConstant()==0) {
			return this;
		}
		return null;
	}

	@Override
	public AbstractValue<LinearExpression> and(LinearExpression aValue) {
		if (aValue.isConstant()) {
			long k=aValue.getConstant();
			
			if (isConstant()) {
				return create(mOffset & k, 0, null, 0, 0);
			}
			else if (k==0) {
				// zero
				return new LinearExpression(0, 0, null, 0, 0);
			}
			else if ((k & 1)==1) {
				int width = 0;
				while ((k & 1)==1) {
					width++;
					k >>>= 1;
				}
				return restrictRange(0, (1L<<width)-1); 
			}
		}
		else if (isConstant()) {
			return aValue.and(this);
		}
		
		return null;
	}

	@Override
	public AbstractValue<LinearExpression> or(LinearExpression aValue) {
		if (aValue.isConstant()) {
			long k=aValue.getConstant();
			
			if (isConstant()) {
				return create(mOffset | k, 0, null, 0, 0);
			}
			else if (k==0) {
				// identity
				return this;
			}
		}
		else if (isConstant()) {
			return aValue.or(this);
		}
		
		return null;
	}

	@Override
	public AbstractValue<LinearExpression> xor(LinearExpression aValue) {
		if (aValue.isConstant()) {
			long k=aValue.getConstant();
			
			if (isConstant()) {
				return create(mOffset ^ k, 0, null, 0, 0);
			}
			else if (k==0) {
				// identity
				return this;
			}
		}
		else if (isConstant()) {
			return aValue.xor(this);
		}
		
		return null;
	}

	@Override
	public AbstractValue<LinearExpression> not() {
		// not(x) = negate(x) - 1
		// e.g.
		// not(14) = 0xff..ff1 = -15 = -14 - 1
		
		return create(-mOffset-1, -mScale, mVariable, mMin, mMax);
	}


	@Override
	public AbstractValue<LinearExpression> logicalComplement() {
		if (isConstant()) {
			assert(mOffset==0 || mOffset==1);
			return new LinearExpression(1-mOffset);
		}
		else
			return null;
	}

	@Override
	public AbstractValue<LinearExpression> equalsOperator(
			LinearExpression aValue) {
		if (isConstant() && aValue.isConstant()) {
			long k = (getConstant() == aValue.getConstant())? 1 : 0;
			return new LinearExpression(k);
		}
		else
			return null;
	}


	@Override
	public AbstractValue<LinearExpression> lessThanOperator(
			LinearExpression aValue) {
		if (isConstant() && aValue.isConstant()) {
			long k = (getConstant() < aValue.getConstant())? 1 : 0;
			return new LinearExpression(k);
		}
		else
			return null;
	}

	/**
	 * @param x
	 * @param y
	 * @return x/y rounded downwards
	 */
	public static long divFloor(long x, long y) {
		if ((x<0) != (y<0)) {
			// negative quotients are rounded upwards
			return (x - Math.abs(y) + 1)/y;
		}
		else
			return x/y; // positive quotients are rounded downwards already
	}

	/**
	 * @param x
	 * @param y
	 * @return x/y rounded upwards
	 */
	public static long divCeil(long x, long y) {
		if ((x<0) == (y<0)) {
			// positive quotients are rounded downwards
			return (x + Math.abs(y) - 1)/y;
		}
		else
			return x/y; // negative quotients are rounded upwards already
	}

	/**
	 * @param min  minimum value of this LinearExpression, for which the linear relation hold
	 * @param max  maximum value of this LinearExpression, for which the linear relation hold
	 * 
	 * @return a LinearExpression, with the valid range of the variable set such that the
	 *         expression is within [min,max]
	 */
	protected LinearExpression restrictRange(long min, long max) {
		assert(mScale!=0);
		
		// min <= mOffset + mScale*x <= max,  mMin <= x <= mMax
		
		if (mScale<0) {
			// swap min/max
			long temp=min;
			min = max;
			max = temp;			
		}
		
		// After potentially interchanging min and max:
		//
		// ceil((min-mOffset)/mScale) <= x <= floor(max-mOffset)/mScale,  mMin <= x <= mMax
		min = Math.max(mMin, divCeil(min-mOffset, mScale));
		max = Math.min(mMax, divFloor(max-mOffset, mScale));
		
		return create(mOffset, mScale, mVariable, min, max);
	}
	
	@Override
	public AbstractValue<LinearExpression> signExtend(int fromBit) {
		long min = -1L << fromBit;
		
		if (isConstant()) {
			// Bring the value into a contiguous unsigned range, mask, and bring it back
			long mask = ~(min<<1);
			long k = ((getConstant() - min) & mask) + min; 
			
			return create(k, 0, null, 0, 0);
		}
		else {
			long max = ~min;
			return restrictRange(min,max);
		}
	}

	@Override
	public AbstractValue<LinearExpression> zeroExtend(int fromWidth) {
		long max = (1L << fromWidth) - 1;
		
		if (isConstant()) {
			return create(getConstant() & max, 0, null, 0, 0);
		}
		else {
			return restrictRange(0,max);
		}
	}

	@Override
	public String getAttributeDefinitions(XmlAttributeFormatter formatter) {
		if (isConstant())
			return "value=\""+mOffset+"\"";
		else  {
			Location location=mVariable.getLocation();
			String sourceName=(location!=null)? " sourceName=\""+location.getDebugName()+"\"" : "";
			return " variable=\""+mVariable.getUniqueId()+"\""+sourceName+ " scale=\""+mScale+"\" offset=\""+mOffset
			       + "\" min=\""+mMin+"\" max=\""+mMax+"\"";
		}
	}

	@Override
	public Iterable<? extends XmlElement> getChildren() {
		return Collections.emptyList();
	}

	@Override
	public String getTagName() {
		return isConstant()? "constant" : "linearExpression";
	}
	
	@Override
	public String toString() {
		if (isConstant())
			return "Constant "+mOffset;
		else {
			Location location=mVariable.getLocation();
			String result=(location!=null)? location.getDebugName() : mVariable.getUniqueId();
			if (mScale!=1)
				result = mScale+"*"+result;
			if (mOffset>0)
				result += "+"+mOffset;
			else if (mOffset<0)
				result += mOffset;
			return "LinearExpression "+result+ " ["+mMin+","+mMax+"]"; 		
		}
	}
}
