/* -*-Java-*- */                                         

/*
 * Copyright (C) 2009  Anders Nilsson <anders.nilsson@cs.lth.se>
 *                                                              
 * This file is part of Actors model compiler.                      
 */                                                             

import xdfAST.Instance;
import xdfAST.Port;
import xdfAST.Start;
import java.util.HashSet;
import java.util.LinkedList;
import java.io.PrintStream;
import java.io.FileNotFoundException;

public class ManageXDF extends XdfParser {
	public static void main(String args[]) {
		Start ast = parse(args);

		LinkedList<Instance> s = ast.getInstances(new LinkedList<Instance>());
 		System.out.println("\n\n");

		for (Instance i: s){
			System.out.println(i.name()+" "+i.id());
		}
		for (Port p : ast.getPorts(new HashSet<Port>())) {
			System.out.println("  "+p.name()+" "+p.kind());
		}
	}
}

