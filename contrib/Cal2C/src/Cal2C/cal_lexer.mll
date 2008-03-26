(*****************************************************************************)
(* CAL2C                                                                     *)
(* Copyright (c) 2007-2008, IETR/INSA of Rennes.                             *)
(* All rights reserved.                                                      *)
(*                                                                           *)
(* This software is governed by the CeCILL-B license under French law and    *)
(* abiding by the rules of distribution of free software. You can  use,      *)
(* modify and/ or redistribute the software under the terms of the CeCILL-B  *)
(* license as circulated by CEA, CNRS and INRIA at the following URL         *)
(* "http://www.cecill.info".                                                 *)
(*                                                                           *)
(* Matthieu WIPLIEZ <Matthieu.Wipliez@insa-rennes.fr                         *)
(*****************************************************************************)

(* File cal_lexer.mll *)
{
open Cal_parser
open Lexing
open Cal2c_util
exception Eof

module E = Cal2c_util.Exception.Make (struct let module_name = "Cal_lexer" end)
open E

(* String construction *)
let str = ref ""

(* Keywords *)
let keyword_table = SH.create 62
let _ =
  List.iter
    (fun (kwd, tok) -> SH.add keyword_table kwd tok)
    [
			("action", ACTION);
			("actor", ACTOR);
			("all", ALL);
			("and", AND);
			("any", ANY);
			("at", AT);
			("at*", AT_STAR);
			("begin", BEGIN);
			("choose", CHOOSE);
			("const", CONST);
			("delay", DELAY);
			("div", DIV);
			("do", DO);
			("dom", DOM);
			("else", ELSE);
			("end", END);
			("endaction", ENDACTION);
			("endactor", ENDACTOR);
			("endchoose", ENDCHOOSE);
			("endforeach", ENDFOREACH);
			("endfunction", ENDFUNCTION);
			("endif", ENDIF);
			("endinitialize", ENDINITIALIZE);
			("endlambda", ENDLAMBDA);
			("endlet", ENDLET);
			("endnetwork", ENDNETWORK);
			("endpriority", ENDPRIORITY);
			("endproc", ENDPROC);
			("endprocedure", ENDPROCEDURE);
			("endschedule", ENDSCHEDULE);
			("endwhile", ENDWHILE);
			("entities", ENTITIES);
			("false", FALSE);
			("for", FOR);
			("foreach", FOREACH);
			("fsm", FSM);
			("function", FUNCTION);
			("guard", GUARD);
			("if", IF);
			("import", IMPORT);
			("in", IN);
			("initialize", INITIALIZE);
			("lambda", LAMBDA);
			("let", LET);
			("map", MAP);
			("mod", MOD);
			("multi", MULTI);
			("mutable", MUTABLE);
			("network", NETWORK);
			("not", NOT);
			("null", NULL);
			("old", OLD);
			("or", OR);
			("priority", PRIORITY);
			("proc", PROC);
			("procedure", PROCEDURE);
			("regexp", REGEXP);
			("repeat", REPEAT);
			("rng", RNG);
			("schedule", SCHEDULE);
			("structure", STRUCTURE);
			("then", THEN);
			("time", TIME);
			("true", TRUE);
			("var", VAR);
			("while", WHILE);
    ]

(* Reserved identifiers *)
let reserved_identifiers = SH.create 14
let _ =
	List.iter
    (fun kwd -> SH.add reserved_identifiers kwd ())
    [
			"assign";
			"case";
			"default";
			"endinvariant";
			"endtask";
			"endtype";
			"ensure";
			"invariant";
			"now";
			"out";
			"protocol";
			"require";
			"task";
			"type"
		]

(* Newline *)
let newline lexbuf =
	let position = lexbuf.lex_curr_p in
	let position = 
		{position with
			pos_lnum = lexbuf.lex_curr_p.pos_lnum + 1;
			pos_bol = Lexing.lexeme_start lexbuf - 1}
	in
		
	lexbuf.lex_curr_p <- position

(* Matches either \ or $. Why so many backslashes? Because \ has to be escaped*)
(* in strings, so we get \\. \, | and $ also have to be escaped in regexps, *)
(* so we have \\\\ \\| \\$. *)
let re_id = Str.regexp "\\\\\\|\\$"
}

(* Numbers *)
let nonZeroDecimalDigit = ['1'-'9']

let decimalDigit = '0' | nonZeroDecimalDigit
let decimalLiteral = nonZeroDecimalDigit (decimalDigit)*

let hexadecimalDigit = decimalDigit | ['a'-'f'] | ['A'-'F']
let hexadecimalLiteral = '0' ('x'|'X') hexadecimalDigit (hexadecimalDigit)*

let octalDigit = ['0'-'7']
let octalLiteral = '0' (octalDigit)*

let integer = decimalLiteral | hexadecimalLiteral | octalLiteral

let exponent = ('e'|'E') ('+'|'-')? decimalDigit+
let real = decimalDigit+ '.' (decimalDigit)* exponent?
| '.' decimalDigit+ exponent?
| decimalDigit+ exponent

(* Identifiers *)
let char = ['a'-'z' 'A'-'Z']
let any_identifier = (char | '_' | decimalDigit | '$')+
let other_identifier =
	(char | '_') (char | '_' | decimalDigit | '$')*
	| '$' (char | '_' | decimalDigit | '$')+
let identifier = '\\' any_identifier '\\' | other_identifier

let newline = ('\010' | '\013' | "\013\010")

(* Token rule *)
rule token = parse
  | [' ' '\t'] {token lexbuf}
	| newline { newline lexbuf; token lexbuf }

	| "->" { ARROW }
	| ':' { COLON }
	| ":=" { COLON_EQUAL }
	| ',' { COMMA }
	| "!=" { DIFFERENT }
	| '/' { DIV }
	| '.' { DOT }
	| ".." { DOUBLE_DOT }
	| "::" { DOUBLE_COLON }
	| "-->" { DOUBLE_DASH_ARROW }
	| "==>" { DOUBLE_EQUAL_ARROW }
	| '=' { EQUAL }
	| ">=" { GE }
	| '>' { GT }
	| '{' { LBRACE }
	| '[' { LBRACKET }
	| "<=" { LE }
	| '<' { LT }
	| '(' { LPAREN }
	| '-' { MINUS }
	| '+' { PLUS }
	| '}' { RBRACE }
	| ']' { RBRACKET }
	| ')' { RPAREN }
	| ';' { SEMICOLON }
	| '#' { SHARP }
	| '*' { TIMES }

  | integer as lxm { INT (int_of_string lxm) }
  | real as lxm { FLOAT (float_of_string lxm) }
  | identifier as ident {
      try
        SH.find keyword_table ident
      with Not_found ->
				let ident = Str.global_replace re_id "_" ident in
				let ident =
					match ident with
						| "signed" -> "_cal_signed"
						| "OUT" -> "out"
						| "BTYPE" -> "btype"
						| "IN" -> "in"
						| _ -> ident
				in
				ID ident}
  | '"' { STRING (string lexbuf) }
  | "//" { single_line_comment lexbuf }
	| "/*" { multi_line_comment lexbuf }
  | eof { EOF }
and string = parse
	| "\\\"" { str := !str ^ "\\\""; string lexbuf }
	| '"' { let s = !str in str := ""; s }
	| _ as c { str := !str ^ (String.make 1 c); string lexbuf }
and single_line_comment = parse
  | newline { newline lexbuf; token lexbuf }
	| _ { single_line_comment lexbuf }
and multi_line_comment = parse
  | "*/" { token lexbuf }
	| newline { newline lexbuf; multi_line_comment lexbuf }
	| _ { multi_line_comment lexbuf }
