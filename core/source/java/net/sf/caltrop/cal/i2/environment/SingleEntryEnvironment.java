package net.sf.caltrop.cal.i2.environment;

import net.sf.caltrop.cal.i2.Environment;
import net.sf.caltrop.cal.i2.ObjectSink;
import net.sf.caltrop.cal.i2.OperandStack;
import net.sf.caltrop.cal.i2.Type;
import net.sf.caltrop.cal.i2.UndefinedInterpreterException;

public class SingleEntryEnvironment extends AbstractEnvironment {
	
	@Override
	public void freezeLocal() {
		value(value);
	}

	@Override
	protected int localGet(Object var, ObjectSink s) {
		if (this.var.equals(var)) {
			s.putObject(value(this.value));
			return 0;
		} else {
			return UNDEFINED;
		}
	}

	@Override
	protected void localGetByPos(int pos, ObjectSink s) {
		if (pos == 0) {
			s.putObject(value(this.value));
		} else {
			throw new UndefinedInterpreterException("Illegal variable position for single entry environment: " + pos);
		}
	}
	
	@Override
	protected int localSet(Object var, Object value) {
		if (this.var.equals(var)) {
			this.value = value;           // FIXME: do something about type
			return 0;
		} else {
			return UNDEFINED;
		}
	}
	
	@Override
	protected void localSetByPos(int varPos, Object value) {
		if (varPos == 0) {
			this.value = value;
		} else {
			throw new UndefinedInterpreterException("Illegal variable position for single entry environment: " + varPos);
		}
	}
	
	@Override
	protected Object localGetVariableName(int varPos) {
		if (varPos == 0) {
			return var;
		} else {
			throw new UndefinedInterpreterException("Illegal variable position for single entry environment: " + varPos);
		}
	}
	
	@Override
	protected Type localGetVariableType(int varPos) {
		if (varPos == 0) {
			return type;
		} else {
			throw new UndefinedInterpreterException("Illegal variable position for single entry environment: " + varPos);
		}
	}
	
	public SingleEntryEnvironment (Environment parent, Object var, Object value, Type type) {
		super (parent);
		this.var = var;
		this.value = value;
		this.type = type;
	}
	
	protected Object var;
	private Object value;
	private Type type;
}
