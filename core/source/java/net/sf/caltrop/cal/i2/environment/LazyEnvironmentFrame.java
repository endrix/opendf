package net.sf.caltrop.cal.i2.environment;

import net.sf.caltrop.cal.i2.Environment;
import net.sf.caltrop.cal.i2.Evaluator;
import net.sf.caltrop.cal.i2.Type;
import net.sf.caltrop.cal.interpreter.ast.Decl;
import net.sf.caltrop.cal.interpreter.ast.Expression;

public class LazyEnvironmentFrame extends EnvironmentFrame {
	
	public LazyEnvironmentFrame(Environment parent, Decl [] decls, Evaluator evaluator) {
		super (parent);
		
		this.vars = new Object [decls.length];
		this.values = new Object [decls.length];
		this.types = new Type [decls.length];
		
		for (int i = 0; i < decls.length; i++) {
			vars[i] = decls[i].getName();
			Expression expr = decls[i].getInitialValue();
			values[i] = (expr != null) ? new Thunk(expr, evaluator, this) : null;
			types[i] = null; // TYPEFIXME: construct type 
		}
	}

}
