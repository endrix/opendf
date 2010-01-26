/* -*-Java-*- */                                         

/*
 * Copyright (C) 2009  Anders Nilsson <anders.nilsson@cs.lth.se>
 *                                                              
 * This file is part of Actors model compiler.                      
 */                                                             


import xlimAST.Start;

public class SDFAnalysis extends XlimParser {
	public static void main(String args[]) {
		Start ast = parse(args);

		System.out.println(ast.isSDF());
	}
}

