/**
 * 
 */
package eu.actorsproject.xlim.absint;

import eu.actorsproject.xlim.XlimType;

/**
 * @author ecarvon
 *
 */
public class LinearExpressionDomain extends GenericDomain<LinearExpression> {

	public LinearExpressionDomain(Evaluator evaluator) {
		super(evaluator);
	}

	@Override
	public LinearExpression getAbstractValue(String constant, XlimType type) {
		if (supportsType(type)) {
			long k=Long.valueOf(constant);
			return new LinearExpression(k);
		}
		else
			return null;	
	}

	@Override
	public LinearExpression getUniverse(XlimType type) {
		return null;
	}

	protected boolean supportsType(XlimType type) {
		return type.isBoolean() || type.isInteger();
	}
}
