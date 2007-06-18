package net.sf.caltrop.cal.i2.types;

import java.util.Map;

public abstract class FloatingPointType extends RealType implements Type {
	
	abstract public int  mantissaLength();
	abstract public int  exponentLength();
}
