package net.sf.caltrop.cal.i2.platform;

import net.sf.caltrop.cal.i2.Configuration;
import net.sf.caltrop.cal.i2.Environment;
import net.sf.caltrop.cal.i2.util.ImportHandler;
import net.sf.caltrop.cal.i2.util.ImportMapper;
import net.sf.caltrop.cal.i2.util.Platform;
import net.sf.caltrop.cal.i2.util.ReplacePrefixImportMapper;

public class DefaultUntypedPlatform implements Platform {

	public Configuration configuration() {
		// TODO Auto-generated method stub
		return null;
	}

	public Environment createGlobalEnvironment() {
		// TODO Auto-generated method stub
		return null;
	}

	public Environment createGlobalEnvironment(Environment parent) {
		// TODO Auto-generated method stub
		return null;
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


}
