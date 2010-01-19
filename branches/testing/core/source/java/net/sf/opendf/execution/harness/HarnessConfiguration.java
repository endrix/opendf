package net.sf.opendf.execution.harness;

import java.util.Collection;

public class HarnessConfiguration {
	
	Collection<String>   inputPorts() {
	
		return null;
	}
	
	Collection<String>  outputPorts() {
		
		return null;
	}
	
	
	InputSource.Factory getInputSourceFactory(String inputPort) {
		
		return null;
	}
	
	OutputSink.Factory getOutputSinkFactory(String outputPort) {
		
		return null;
	}
	

}
