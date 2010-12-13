/* -*-Java-*- */                                         

/*
 * Copyright (C) 2009,2010  Anders Nilsson <anders.nilsson@control.lth.se>
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
		// LinkedList<Integer> maxBufSizes = new LinkedList<Integer>();
		int instanceIndex = 0;

		for (Instance i : xdf.getInstances(new LinkedList<Instance>())) {
			// System.out.println(i.name()+" "+i.classification());
			i.setIndex(++instanceIndex);
			if (i.classification().equals("CSDF")) {
				// Phony example just to try merging, this condition
				// by no means ensure that the actors may be merged.
				// System.out.println("   Adding "+i.name()+" "+i.getIndex());
				instances.add(i);
			} else {
				System.out.println("Instance "+i.name()+" not CSDF?");
			}
		}
		int[] schedule = {1,2,1,3,2,1,3,2,1,4,4,5,3,2,3,4,5};
		int ix = 0,ixx = 0;
		int[] rates = {1,1,1,1,1,1,1,1,1,1,1,1,2,2,1,1,1,1,1,1,1};
		for (Connection c : ast.getConnections(new LinkedList<Connection>())) {
			c.setBufSize(rates[ixx++]);
			// if (c.getDest().isCSDF() && c.getSource().isCSDF()) {
				// c.setBufSize(3); //Phony implementation just for testing
			// 	schedule[ix++] = c.getSource().getIndex();
			// 	schedule[ix++] = c.getDest().getIndex();
			// } else {
				// c.setBufSize(2);
			// }
		}
		System.out.println("Added "+ix+" actors to schedule");

		ast.mergeActors("MergedActor_1",schedule);
		try {
			ast.getXlimInstance("MergedActor_1").
				prettyPrint("",new PrintStream("MergedActor_1.xlim"));
			String name = args[0].substring(0,args[0].indexOf('.'));
			ast.prettyPrint("",new PrintStream(name+"_new.xdf"));
		} catch (FileNotFoundException e) {
			System.out.println("Could not read Merged xlim file");
		}

		// Genereate build script
		System.out.println(ast.genBuild());

	}
}

