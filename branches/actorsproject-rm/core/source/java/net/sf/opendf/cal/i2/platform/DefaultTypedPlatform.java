package net.sf.opendf.cal.i2.platform;

import net.sf.opendf.cal.i2.Configuration;
import net.sf.opendf.cal.i2.Environment;
import net.sf.opendf.cal.i2.configuration.DefaultTypedConfiguration;
import net.sf.opendf.cal.i2.util.ImportHandler;
import net.sf.opendf.cal.i2.util.ImportMapper;
import net.sf.opendf.cal.i2.util.Platform;

public class DefaultTypedPlatform extends DefaultUntypedPlatform implements Platform {

	public Configuration configuration() {
		return theConfiguration;
	}

	public static final Platform thePlatform = new DefaultTypedPlatform();
	public static final Configuration theConfiguration = new DefaultTypedConfiguration();

}
