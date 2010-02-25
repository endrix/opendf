/* -*-Java-*- */                                         

/*
 * Copyright (C) 2009  Anders Nilsson <anders.nilsson@cs.lth.se>
 *                                                              
 * This file is part of Actors model compiler.                      
 */                                                             

import xdfAST.Connection;
import xdfAST.Instance;
import xdfAST.Start;
import xdfAST.XDF;
import java.util.HashSet;
import java.util.LinkedList;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;

public class MergeActors extends XdfParser {
	public static void main(String args[]) {
		Start ast = parse(args);
		XDF xdf = (XDF) ast.getSpecification().getElement(0);
		LinkedList<Instance> instances = new LinkedList<Instance>();
		LinkedList<Integer> maxBufSizes = new LinkedList<Integer>();
		int instanceIndex = 0;

		for (Instance i : xdf.getInstances(new HashSet<Instance>())) {
			// System.out.println(i.name()+" "+i.classification());
			if (i.classification().equals("CSDF")) {
				// Phony example just to try merging, this condition
				// by no means ensure that the actors may be merged.
				i.setIndex(++instanceIndex);
				// System.out.println("   Adding "+i.name()+" "+i.getIndex());
				instances.add(i);
			}
		}
		int[] schedule = new int[instances.size()];
		int ix = 0;
		for (Connection c : ast.getConnections(new HashSet<Connection>())) {
			if (c.getDest().isCSDF() && c.getSource().isCSDF()) {
				c.setBufSize(3); //Phony implementation just for testing
				schedule[ix++] = c.getSource().getIndex();
				schedule[ix++] = c.getDest().getIndex();
			}
		}
		System.out.println("Added "+ix+" actors to schedule");

		ast.mergeActors("MergedActor",schedule);
		try {
			ast.getXlimInstance("MergedActor").prettyPrint("",new PrintStream("MergedActor.xlim"));
			String name = args[0].substring(0,args[0].indexOf('.'));
			ast.prettyPrint("",new PrintStream(name+"_new.xdf"));
		} catch (FileNotFoundException e) {
			System.out.println("Could not read Merged xlim file");
		}
	}
}
