package net.sf.caltrop.hades.des.util;

import java.util.Collection;

public interface OutputBlockRecord {
	
	String  			getComponentName();

	Collection<String>	getBlockedOutputConnectors();
}
