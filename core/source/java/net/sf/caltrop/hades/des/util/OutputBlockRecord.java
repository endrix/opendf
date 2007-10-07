package net.sf.caltrop.hades.des.util;

import java.util.Collection;
import java.util.Map;
import java.util.List;

public interface OutputBlockRecord {
	
	String  			getComponentName();

	Collection<String>	getBlockedOutputConnectors();
	
	Map<String, Collection<Object>>   getBlockingSourceMap();
	
	long				getStepNumber();
	
	double				getTime();
}
