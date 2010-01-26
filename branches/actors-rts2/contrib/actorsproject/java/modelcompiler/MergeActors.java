/* -*-Java-*- */                                         

/*
 * Copyright (C) 2009  Anders Nilsson <anders.nilsson@cs.lth.se>
 *                                                              
 * This file is part of Actors model compiler.                      
 */                                                             

import xdfAST.Start;
import xdfAST.Instance;
import java.util.HashSet;
import java.io.PrintStream;
import java.io.FileNotFoundException;

public class MergeActors extends XdfParser {
	public static void main(String args[]) {
		Start ast = parse(args);

// 		ast.genSSR(System.out);
// 		HashSet<Instance> l = ast.genStaticSchedule(new HashSet<Instance>());
// 		System.out.println("\n\n");

// 		for (Instance i: l){
// 			do {
// 				System.out.print(i.name()+" -> ");
// 				i = i.next;
// 			} while (i != null);
// 			System.out.println();
// 		}

		ast.mergeActors();
		try {
			String name = args[0].substring(0,args[0].indexOf('.'));
			ast.prettyPrint("",new PrintStream(name+"_new.xdf"));
		} catch (FileNotFoundException e) {
			System.out.println("Could not generate modified xdf file");
		}
	}
}

