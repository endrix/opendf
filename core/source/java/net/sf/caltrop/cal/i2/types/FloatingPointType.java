package net.sf.caltrop.cal.i2.types;

import java.util.Map;

/**
 * Instances of FloatingPointType types are floating-point representations of real numbers.
 * 
 * @author jornj
 *
 */

public abstract class FloatingPointType extends RealType implements Type {
	
	abstract public int  mantissaLength();
	abstract public int  exponentLength();
}
