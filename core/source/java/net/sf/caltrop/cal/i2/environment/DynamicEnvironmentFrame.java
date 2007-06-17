package net.sf.caltrop.cal.i2.environment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.caltrop.cal.i2.Environment;
import net.sf.caltrop.cal.i2.InterpreterException;
import net.sf.caltrop.cal.i2.ObjectSink;
import net.sf.caltrop.cal.i2.Type;

public class DynamicEnvironmentFrame extends AbstractEnvironment {
	
	
	@Override
	public void freezeLocal() {
		for (Object v : values) {
			value(v);
		}
	}

	@Override
	protected int localGet(Object var, ObjectSink s) {
		int i = vars.indexOf(var);
		if (i >= 0) {
			s.putObject(value(values.get(i)));
			return i;
		} else {
			return UNDEFINED;
		}
	}

	@Override
	protected void localGetByPos(int pos, ObjectSink s) {
		s.putObject(value(values.get(pos)));
	}

	@Override
	protected int localSet(Object var, Object value) {
		int i = 0;
		while (i < vars.size()) {
			if (vars.get(i).equals(var)) {
				values.set(i, value);  // TYPEFIXME: do type conversion
				return i;
			}
			i += 1;
		}
		return UNDEFINED;
	}

	@Override
	protected void localSetByPos(int varPos, Object value) {
		values.set(varPos, value);  // TYPEFIXME: do type conversion
	}
	
	@Override
	protected Object localGetVariableName(int varPos) {
		return vars.get(varPos);
	}
	
	@Override
	protected Type localGetVariableType(int varPos) {
		return types.get(varPos);
	}
	
	//
	//  DynamicEnvironmentFrame
	//
	
	public int  bind(Object var, Object value, Type type) {
		if (vars.contains(var)) {
			throw new InterpreterException("Variable defined multiple times: '" + var + "'.");
		}
		vars.add(var);
		values.add(value);
		types.add(type);
		return vars.size() - 1;
	}
	
	public Map	localBindings() {
		Map m = new HashMap();
		for (int i = 0; i < vars.size(); i++) {
			m.put(vars.get(i), values.get(i));
		}
		return m;
	}
	
	
	public DynamicEnvironmentFrame(Environment parent) {
		super(parent);
		this.vars = new ArrayList<Object>();
		this.values = new ArrayList<Object>();
		this.types = new ArrayList<Type>();
	}
	
	protected List<Object>	vars;
	protected List<Object>	values;
	protected List<Type>	types;
}
