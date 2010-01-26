package net.sf.opendf.profiler.schedule;

import java.io.PrintWriter;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 
 * @author jornj
 */
public class XmlScheduleOutput implements ScheduleOutput {

	public void start() {
		pw.println("<schedule>");
	}

	public void executeStep(Object stepID, Time start, Duration duration) {
		beginStep(stepID, start, duration);
		endStep();
	}

	public void beginStep(Object stepID, Time start, Duration duration) {

		pw.println("  <step ID='" + stepID
				 + "' start='" + start
				 + "' duration='" + duration + "'>");
	}
	
	public void endStep() {
		pw.println("  </step>");
	}
	
	public void  attribute(Object key, Object value) {
		if (isStructured(value)) {
			pw.println("  <a key='" + key + "'>");
			writeStructure(value);
			pw.println("  </a>");
		} else {
			pw.println("  <a key='" + key + "' value='" + value + "'/>");
		}
	}
	
	public void finish(Time tm) {
		pw.println("  <a key='completion-time' value='" + tm.toString() + "'/>");
		pw.println("</schedule>");
		pw.flush();
	}
	
	public XmlScheduleOutput(PrintWriter pw) {
		this.pw = pw;
	}
	
	private PrintWriter pw;
	
	private boolean isStructured(Object a) {
		return a instanceof Collection || a instanceof Map;
	}
	
	private void  writeStructure(Object a) {
		if (a instanceof List || a instanceof Set) {
			if (a instanceof List)
				pw.println("    <list>");
			else
				pw.println("    <set>");
			
			for (Iterator i = ((Collection)a).iterator(); i.hasNext(); ) {
				Object e = i.next();
				writeStructure(e);
			}
			
			if (a instanceof List)
				pw.println("    </list>");
			else
				pw.println("    </set>");
		} else if (a instanceof Map) {
			for (Iterator i = ((Map)a).keySet().iterator(); i.hasNext(); ) {
				Object k = i.next();
				Object v = ((Map)a).get(k);
				pw.println("    <pair>");
				writeStructure(k);
				writeStructure(v);
				pw.println("    </pair>");
			}			
		} else {
			pw.println("    <atomic value='" + a + "'/>");
		}
	}

}
