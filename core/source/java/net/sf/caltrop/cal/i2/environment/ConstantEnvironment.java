package net.sf.caltrop.cal.i2.environment;

import net.sf.caltrop.cal.i2.Environment;
import net.sf.caltrop.cal.i2.InterpreterException;
import net.sf.caltrop.cal.i2.ObjectSink;
import net.sf.caltrop.cal.i2.types.Type;

public class ConstantEnvironment implements Environment {

	public void freezeAll() {
		delegate.freezeAll();
	}

	public void freezeLocal() {
		delegate.freezeLocal();
	}

	public Object getByName(Object var) {
		return delegate.getByName(var);
	}

	public Object getByPosition(long pos) {
		return delegate.getByPosition(pos);
	}

	public Object getByPosition(int frame, int posVar) {
		return delegate.getByPosition(frame, posVar);
	}

	public Object getVariableName(int frame, int varPos) {
		return delegate.getVariableName(frame, varPos);
	}

	public Type getVariableType(int frame, int varPos) {
		return delegate.getVariableType(frame, varPos);
	}

	public long lookupByName(Object var, ObjectSink s) {

		delegate.lookupByName(var, s);
		return CONSTANT;
	}
	
	public void lookupByPosition(int frame, int varPos, ObjectSink s) {
		delegate.lookupByPosition(frame, varPos, s);
	}

	public long setByName(Object var, Object value) {
		throw new InterpreterException("Cannot set variable in ConstantEnvironment: '" + var + "'.");
	}

	public void setByPosition(long pos, Object value) {
		throw new InterpreterException("Cannot set variable in ConstantEnvironment.");
	}

	public void setByPosition(int frame, int varPos, Object value) {
		throw new InterpreterException("Cannot set variable in ConstantEnvironment.");
	}
	
	public ConstantEnvironment (Environment delegate) {
		this.delegate = delegate;
	}
	
	private Environment delegate;
}
