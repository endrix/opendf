package net.sf.caltrop.cal.i2.platform;

import java.math.BigInteger;

import net.sf.caltrop.cal.i2.Configuration;
import net.sf.caltrop.cal.i2.Environment;
import net.sf.caltrop.cal.i2.Evaluator;
import net.sf.caltrop.cal.i2.Function;
import net.sf.caltrop.cal.i2.InterpreterException;
import net.sf.caltrop.cal.i2.configuration.DefaultUntypedConfiguration;
import net.sf.caltrop.cal.i2.environment.DynamicEnvironmentFrame;
import net.sf.caltrop.cal.i2.util.ImportHandler;
import net.sf.caltrop.cal.i2.util.ImportMapper;
import net.sf.caltrop.cal.i2.util.Platform;
import net.sf.caltrop.cal.i2.util.ReplacePrefixImportMapper;

public class DefaultUntypedPlatform implements Platform {

	public Configuration configuration() {
		return theConfiguration;
	}

	public Environment createGlobalEnvironment() {
		return createGlobalEnvironment(null);
	}

	public Environment createGlobalEnvironment(Environment parent) {
		DynamicEnvironmentFrame env = new DynamicEnvironmentFrame(parent);
		
		populateGlobalEnvironment(env);
		return env;
	}

	public ImportHandler[] getImportHandlers(ClassLoader loader) {
		// TODO Auto-generated method stub
		return null;
	}

	public ImportMapper []  getImportMappers() {
		return new ImportMapper [] {
				new ReplacePrefixImportMapper(new String [] {"caltrop", "lib"}, 
											  new String [] {"net", "sf", "caltrop", "cal", "lib"})
		};
    }


	public static final Platform      	thePlatform = new DefaultUntypedPlatform();
	public static final Configuration 	theConfiguration = new DefaultUntypedConfiguration();

	private static void populateGlobalEnvironment(DynamicEnvironmentFrame env) {
		
		env.bind("PI", Double.valueOf(Math.PI), null);   // TYPEFIXME
		
		env.bind("$add", new Function () {
			public void apply(int n, Evaluator evaluator) {
				if (n != 2)
					throw new InterpreterException("+ operator: Expected two arguments, received " + n + ".");
				Object a = evaluator.getValue(0);
				Object b = evaluator.getValue(1);
				if (a instanceof BigInteger && b instanceof BigInteger) {
					evaluator.replaceWithResult(2, ((BigInteger)a).add((BigInteger)b));
				} else {
					throw new InterpreterException("+ operator: Cannot handled arguments. (" + a + ", " + b+ ")");
				}
			}			
		}, null);		// TYPEFIXME
	}
}
