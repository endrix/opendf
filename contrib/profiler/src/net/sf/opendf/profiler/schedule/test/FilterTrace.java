package net.sf.opendf.profiler.schedule.test;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.PrintWriter;

import net.sf.opendf.profiler.data.Trace;
import net.sf.opendf.profiler.io.TraceParser;
import net.sf.opendf.profiler.util.Lib;

/**
 * 
 * @author jornj
 */
public class FilterTrace {
	
	public static void main(String [] args) throws Exception {
		
		if (args.length == 0 || args.length % 3 != 0) {
			printUsage();
			return;
		}
		
		for (int i = 0; i < args.length; i += 3) {
			InputStream is = new FileInputStream(args[i]);
			int mask = Integer.parseInt(args[i + 1]);
			
			System.out.print("Read '" + args[i] + "' [" + mask + "]... ");
			Trace t = new TraceParser(is, mask).parse();
			System.out.print("done. Write to '" + args[i + 2] + "'... ");
			is.close();
			
			PrintWriter pw = new PrintWriter(new FileOutputStream(args[i + 2]));
			Lib.writeTrace(t, pw);
			pw.close();
			System.out.println("done.");
		}
	}
	
	static void printUsage() {
		System.out.println("FilterTrace inTrace1 mask1 outTrace1 ... inTraceN maskN outTraceN");
	}

}
