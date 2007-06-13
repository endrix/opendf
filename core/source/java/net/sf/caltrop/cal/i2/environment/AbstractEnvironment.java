package net.sf.caltrop.cal.i2.environment;

import net.sf.caltrop.cal.i2.Environment;
import net.sf.caltrop.cal.i2.ObjectSink;
import net.sf.caltrop.cal.i2.OperandStack;
import net.sf.caltrop.cal.i2.Type;
import net.sf.caltrop.cal.i2.UndefinedInterpreterException;
import net.sf.caltrop.cal.i2.UndefinedVariableException;

abstract public class AbstractEnvironment implements Environment {

	public Object getByName(Object var) {
		lookupByName(var, tmpSink);
		return tmp;
	}

	public long lookupByName(Object var, ObjectSink s) {
		int res = localGet(var, s);
		
		if (res >= 0) {
			return makePos(0, res);
		} else {
			switch (res) {
			case (int)NOPOS:
				return NOPOS;
			case (int)CONSTANT:
				return CONSTANT;
			case (int) UNDEFINED:
				if (parent == null)
					throw new UndefinedVariableException(var);
				long pres = parent.lookupByName(var, s);
				if (pres >= 0) {
					return makePos(posFrame(pres) + 1, posVar(pres));
				} else {
					return pres;
				}
			default:
 				throw new UndefinedInterpreterException("Bad return value from localGet: " + res);
			}
		}
	}

	public void lookupByPosition(int frame, int varPos, ObjectSink s) {
		assert frame >= 0;
		assert varPos >= 0;
		
		if (frame == 0) {
			localGetByPos(varPos, s);
		} else  {
			assert parent != null;

			parent.lookupByPosition(frame - 1, varPos, s);
		}
	}
	
	public long setByName(Object var, Object value) {
		int res = localSet(var, value);
		
		if (res >= 0) {
			return makePos(0, res);
		} else {
			switch (res) {
			case (int)NOPOS:
				return NOPOS;
			case (int) UNDEFINED:
				if (parent == null)
					throw new UndefinedVariableException(var);
				long pres = parent.setByName(var, value);
				if (pres >= 0) {
					return makePos(posFrame(pres) + 1, posVar(pres));
				} else {
					return pres;
				}
			default:
 				throw new UndefinedInterpreterException("Bad return value from localSet: " + res);
			}
		}
	}
	
	public void setByPosition(int frame, int varPos, Object value) {
		assert frame >= 0;
		assert varPos >= 0;
		
		if (frame == 0) {
			localSetByPos(varPos, value);
		} else  {
			assert parent != null;

			parent.setByPosition(frame - 1, varPos, value);
		}
	}
	
	public Object  getVariableName(int frame, int varPos) {
		assert frame >= 0;
		assert varPos >= 0;
		
		if (frame == 0) {
			return localGetVariableName(varPos);
		} else  {
			assert parent != null;

			return parent.getVariableName(frame - 1, varPos);
		}
	}

	public Type    getVariableType(int frame, int varPos) {
		assert frame >= 0;
		assert varPos >= 0;
		
		if (frame == 0) {
			return localGetVariableType(varPos);
		} else  {
			assert parent != null;

			return parent.getVariableType(frame - 1, varPos);
		}		
	}
	

	
	public void freezeAll() {
		if (parent != null) {
			parent.freezeAll();
		}
		freezeLocal();
	}
	
	abstract public void  freezeLocal();
	
	abstract protected  int   localGet(Object var, ObjectSink s);
	
	abstract protected  void  localGetByPos(int pos, ObjectSink s);
	
	abstract protected  int   localSet(Object var, Object value);
	
	abstract protected  void  localSetByPos(int varPos, Object value);
	
	abstract protected  Object localGetVariableName(int varPos);

	abstract protected  Type localGetVariableType(int varPos);

	
	protected Object value(Object value) {
		if (value instanceof VariableContainer) {
			return ((VariableContainer)value).value();
		}
		return value;  // FIXME: handle VariableContainer for lazy evaluation
	}

	public AbstractEnvironment(Environment parent) {
		this.parent = parent;
	}
	

	protected Environment  parent;
	
	final static int UNDEFINED = -11;
	
	protected final static int  posFrame(long pos) {
		return (int)(pos >> 32);
	}
	
	protected final static int  posVar(long pos) {
		return (int)pos;
	}
	
	protected final static long  makePos(int frame, int var) {
		return (((long)frame) << 32) | (long)var; 
	}
	
	private static ObjectSink tmpSink = new ObjectSink() {
		public void putObject(Object value) { tmp = value; }
	};
	
	private static Object tmp;
}
