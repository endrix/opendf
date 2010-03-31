/* -*-Java-*- */                                         

/*
 * Copyright (C) 2009  Anders Nilsson <anders.nilsson@cs.lth.se>
 *                                                              
 * This file is part of Actors model compiler.                      
 */                                                             

import xdfAST.Start;
import xdfAST.Instance;
import xdfAST.XDF;
import java.util.Arrays;
import java.util.LinkedList;
import java.io.PrintStream;
import java.io.FileNotFoundException;

public class ConstraintInfo extends XdfParser {
	public static void main(String args[]) {
		Start ast = parse(args);
		XDF xdf = (XDF) ast.getSpecification().getElement(0);

		System.out.println("Number of actors: "+xdf.numActors());
		System.out.println("Number of FIFOs: "+xdf.numFIFOs());
		System.out.println();
		for (Instance i : xdf.getInstances(new LinkedList<Instance>())) {
			System.out.println(i.name()+" "+i.classification());
			for (xlimAST.actor_port p : i.getPorts()) {
				// System.out.println("  "+p.name()+" "+Arrays.toString(p.tokenPattern()));
				System.out.println("  "+p.name()+" "+p.tokenPattern());
			}
		}
	}
}

