package net.sf.caltrop.cal.i2.environment;

import net.sf.caltrop.cal.i2.Environment;
import net.sf.caltrop.cal.i2.ObjectSink;
import net.sf.caltrop.cal.i2.Type;

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
				values[i] = value;  // TYPEFIXME: do type conversion
				return i;
			}
			i += 1;
		}
		return UNDEFINED;
	}

	@Override
	protected void localSetByPos(int varPos, Object value) {
		values[varPos] = value; 
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
	}
	
	protected EnvironmentFrame(Environment parent) {
		this (parent, null, null, null);
	}
		
	protected Object [] vars;
	protected Object [] values;
	protected Type   [] types;
}
