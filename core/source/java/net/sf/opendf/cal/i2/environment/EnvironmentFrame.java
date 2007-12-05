package net.sf.caltrop.cal.i2.environment;

import net.sf.caltrop.cal.i2.Environment;
import net.sf.caltrop.cal.i2.ObjectSink;
import net.sf.caltrop.cal.i2.types.Type;

public class EnvironmentFrame extends AbstractEnvironment {
	
	@Override
	public void freezeLocal() {
		for (Object v : values) {
			value(v);
		}
	}

	@Override
	protected int localGet(Object var, ObjectSink s) {
		int i = 0;
		while (i < vars.length) {
			if (vars[i].equals(var)) {
				s.putObject(value(values[i]));
				return i;
			}
			i += 1;
		}
		return UNDEFINED;
	}

	@Override
	protected void localGetByPos(int pos, ObjectSink s) {
		s.putObject(value(values[pos]));
	}

	@Override
	protected int localSet(Object var, Object value) {
		int i = 0;
		while (i < vars.length) {
			if (vars[i].equals(var)) {
				localSetByPos(i, value);
				return i;
			}
			i += 1;
		}
		return UNDEFINED;
	}

	@Override
	protected void localSetByPos(int varPos, Object value) {
		if (types[varPos] != null) {
			values[varPos] = types[varPos].convert(value); 
		} else {
			values[varPos] = value; 
		}
	}
	
	@Override
	protected Object localGetVariableName(int varPos) {
		return vars[varPos];
	}
	
	@Override
	protected Type localGetVariableType(int varPos) {
		return types[varPos];
	}
	
	
	public EnvironmentFrame(Environment parent, Object [] vars, Object [] values, Type [] types) {
		super(parent);
		this.vars = vars;
		this.values = values;
		this.types = types;
		if (values != null) {
			for (int i = 0; i < values.length; i++) {
				if (types != null && types[i] != null && !(values[i] instanceof VariableContainer)) {
					values[i] = types[i].convert(values[i]); 
				}
			}
		}
	}
	
	protected EnvironmentFrame(Environment parent) {
		this (parent, null, null, null);
	}
		
	protected Object [] vars;
	protected Object [] values;
	protected Type   [] types;
}
