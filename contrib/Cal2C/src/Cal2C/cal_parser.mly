%{
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

open Cal2c_util

module E = Cal2c_util.Exception.Make (struct let module_name = "Cal_parser" end)
open E

type actor_decl =
	| Action of Calast.action
	| Decl of Calast.decl
	| Priority of string list list

let unnamed_count = ref 0

let has_error = ref false

let print_pos ps msg =
	has_error := true;
	Printf.printf "Line %i, column %i: %s\n%!"
		ps.Lexing.pos_lnum (ps.Lexing.pos_cnum - ps.Lexing.pos_bol)
		msg
		
let var_of_id id =
	if id = "Integer" then (
		let ps = symbol_end_pos () in
		print_pos ps "\"Integer\" function should be spelled Integers"
	);
	Calast.Var id

let generic_type = (IS.singleton 0, Calast.Type.TV 0)

let function_declaration return_type name params locals body =
	let non_empty_params =
		match params with
			| [] -> [{ Calast.d_name = "()"; d_type = (IS.empty, Calast.Type.TP Calast.Type.Unit);
				d_value = None }]
			| params -> params
	in
	let (_, return_type) =
     List.fold_right
     (fun decl (cnt, (s, t)) ->
			 let param_type = decl.Calast.d_type in
			 let (cnt, param_is, param_t) =
				match param_type with
				| (_, Calast.Type.TV 0) ->
					(cnt + 1, IS.singleton cnt, Calast.Type.TV cnt)
				| (_, Calast.Type.TL (None, Calast.Type.TV 0)) ->
					(cnt + 1, IS.singleton cnt, Calast.Type.TL (None, Calast.Type.TV cnt))
				| (is, t) -> (cnt, is, t)
			 in
			 let s = IS.union s param_is in
			 let t = Calast.Type.TA (param_t, Calast.TypeSchemeSet.empty, t) in
			  (cnt, (s, t))
     )
     non_empty_params (1, return_type)
  in
	
	{ Calast.d_name = name;
		d_type = return_type;
		d_value = Some (Calast.Function(params, locals, body));
	}

%}
/* File cal_parser.mly */
%token ARROW
%token COLON
%token COLON_EQUAL
%token COMMA
%token DIFFERENT
%token DOT
%token DOUBLE_COLON
%token DOUBLE_DASH_ARROW
%token DOUBLE_DOT
%token DOUBLE_EQUAL_ARROW
%token EQUAL
%token GE
%token GT
%token LBRACE
%token LBRACKET
%token LE
%token LT
%token LPAREN
%token MINUS
%token PLUS
%token RBRACE
%token RBRACKET
%token RPAREN
%token SEMICOLON
%token SHARP
%token TIMES

%token <int> INT
%token <float> FLOAT
%token <string> ID
%token <string> STRING
%token EOF

%token ACTION
%token ACTOR
%token ALL
%token AND
%token ANY
%token AT
%token AT_STAR
%token BEGIN
%token CHOOSE
%token CONST
%token DELAY
%token DIV
%token DO
%token DOM
%token ELSE
%token END
%token ENDACTION
%token ENDACTOR
%token ENDCHOOSE
%token ENDFOREACH
%token ENDFUNCTION
%token ENDIF
%token ENDINITIALIZE
%token ENDLAMBDA
%token ENDLET
%token ENDNETWORK
%token ENDPRIORITY
%token ENDPROC
%token ENDPROCEDURE
%token ENDSCHEDULE
%token ENDWHILE
%token ENTITIES
%token FALSE
%token FOR
%token FOREACH
%token FSM
%token FUNCTION
%token GUARD
%token IF
%token IMPORT
%token IN
%token INITIALIZE
%token LAMBDA
%token LET
%token MAP
%token MOD
%token MULTI
%token MUTABLE
%token NETWORK
%token NOT
%token NULL
%token OLD
%token OR
%token PRIORITY
%token PROC
%token PROCEDURE
%token REGEXP
%token REPEAT
%token RNG
%token SCHEDULE
%token STRUCTURE
%token THEN
%token TIME
%token TRUE
%token VAR
%token WHILE

%token TYPE_BOOL
%token TYPE_FLOAT
%token TYPE_INT
%token TYPE_LIST
%token TYPE_STRING

/* Operator precedence */
%nonassoc IF
%nonassoc ELSE

/* From low to high precedence */
%left	COMMA
%right COLON_EQUAL
%left OR
%left AND
%left EQUAL DIFFERENT
%left LT LE GT GE
%left PLUS MINUS
%left DIV MOD TIMES SLASH
%nonassoc NOT SHARP DOM RNG UMINUS
%left LPAREN RPAREN LBRACKET RBRACKET

%start network
%type <bool * Calast.network> network
%start actor
%type <bool * Calast.actor> actor
%%
action:
	ACTION actionBody
	{
		let action =
			{$2 with Calast.a_name = Printf.sprintf "untagged_action_%i" !unnamed_count}
		in
		incr unnamed_count;
		action
	}
| qualID COLON ACTION actionBody
	{
		{$4 with Calast.a_name = $1}
	};

actionBody:
	actionInputPatternsOpt DOUBLE_EQUAL_ARROW outputExpressionsOpt
	guardOpt delayOpt varDeclSectionOpt doStatementsOpt endAction
	{
		{Calast.a_decls = $6;
		a_delay = $5;
		a_guards = $4;
		a_inputs = $1;
		a_name = "";
		a_outputs = $3;
		a_stmts = $7
		}
	};

actionInputPattern:
	LBRACKET ids RBRACKET repeatClauseOpt { ("", $2, $4) }
| ID COLON LBRACKET ids RBRACKET repeatClauseOpt { ($1, $4, $6) };

actionInputPatternsRev:
	actionInputPattern { [$1] }
| actionInputPatternsRev COMMA actionInputPattern { $3 :: $1 };

actionInputPatterns: actionInputPatternsRev { List.rev $1 };

actionInputPatternsOpt:
	{ [] }
| actionInputPatterns { $1 };

doStatementsOpt:
	{ Calast.Unit }
| DO statements { $2 };

/* Actor */
actor: imports ACTOR ID calTypeParsOpt LPAREN parsOpt RPAREN ioSignature COLON
actorBody endActor EOF
	{
		unnamed_count := 0;
		let (actions, locals, priorities, fsm) = $10 in
		let (inputs, outputs) = $8 in
		(!has_error, { Calast.ac_name = $3;
			ac_actions = actions;
			ac_fsm = fsm;
			ac_inputs = inputs;
			ac_locals = locals;
			ac_outputs = outputs;
			ac_parameters = $6;
			ac_priorities = priorities })
	};

actorBody:
	actorDeclsOpt {
		let (actions, decls, priorities) = $1 in
		let actions = List.rev actions in
		let decls = List.rev decls in
		let priorities = List.flatten (List.rev priorities) in
		(actions, decls, priorities, None)
		}
| actorDeclsOpt schedule actorDeclsOpt
{ let (actions1, decls1, priorities1) = $1 and
			(actions2, decls2, priorities2) = $3
	in
	let actions = List.rev actions1 @ List.rev actions2 in
	let decls = List.rev decls1 @ List.rev decls2 in
	let priorities = List.flatten (List.rev priorities1 @ List.rev priorities2) in
	(actions, decls, priorities, $2) };

actorDeclsOpt:
	{ ([], [], []) }
| actorDeclsOpt actorDecl {
	let (actions, decls, priorities) = $1 in
	match $2 with
		| None -> (actions, decls, priorities)
		| Some thing ->
			match thing with
				| Action action -> (action :: actions, decls, priorities)
				| Decl decl -> (actions, decl :: decls, priorities)
				| Priority priority -> (actions, decls, priority :: priorities) };

actorDecl:
	action { Some (Action $1) }
| initializationAction { None }
| priorityOrder { Some (Priority $1) }
| topVarDecl { Some (Decl $1) };
	
attributeDecl:
	ID EQUAL expression SEMICOLON { () }
| ID COLON calType SEMICOLON { () };

attributeDeclsRev:
	{ [()] }
| attributeDeclsRev attributeDecl { $2 :: $1 };

attributeDecls: attributeDeclsRev { List.rev $1 };

attributeSection: LBRACE attributeDecls RBRACE { () };

attributeSectionOpt:
	{ () }
| attributeSection { () };
	
beginProcDecl:
	BEGIN { () }
| DO { () };

calType:
	calTypeId { $1 }
| calTypeId LBRACKET calTypePars RBRACKET
	{ match $1 with
		| (_, Calast.Type.TL _) ->
			let (is, t) = List.hd $3 in
			(is, Calast.Type.TL (None, t))
		| t -> t }
| calTypeId LPAREN calTypeAttrs RPAREN { $1 };

calTypeId:
	ID {
		match $1 with
			| "bool" -> (IS.empty, Calast.Type.TP Calast.Type.Bool)
			| "float" -> (IS.empty, Calast.Type.TP Calast.Type.Float)
			| "int" -> (IS.empty, Calast.Type.TP Calast.Type.Int)
			| "list"
			| "List" -> (IS.singleton 0, Calast.Type.TL (None, Calast.Type.TV 0))
			| "string" -> (IS.empty, Calast.Type.TP Calast.Type.String)
			| _ ->
  			let ps = symbol_end_pos () in
	  		print_pos ps ("Invalid type: " ^ $1);
	  		generic_type };
		
calTypeAttr:
	ID COLON calType { () }
| ID EQUAL expression { () };

calTypeAttrsRev:
	calTypeAttr { [()] }
| calTypeAttrsRev COMMA calTypeAttr { $3 :: $1 };

calTypeAttrs: calTypeAttrsRev { List.rev $1 };

calTypePar:
	calType { $1 }
| calTypeId LT calType { $1 };

calTypeParsRev:
	{ [] }
| calTypeParsRev calTypePar { $2 :: $1 };

calTypePars: calTypeParsRev { List.rev $1 };

calTypeParsOpt:
	{ () }
| LBRACKET calTypePars RBRACKET { () };

connector:
	entityRef { $1 }
| entityRef DOT entityRef { $1 ^ "." ^ $3 };

delayOpt:
	{ Calast.Literal (Calast.Integer 0) }
| DELAY expression { $2 };

endAction:
	endActionKeyword { () }
| endActionKeyword SEMICOLON {
	let ps = symbol_end_pos () in
	print_pos ps "Unnecessary ;";
	};

endActionKeyword:
	END { () }
| ENDACTION { () };

endActor:
	END { () }
| ENDACTOR { () };

endFunDecl:
	END { () }
| ENDFUNCTION { () };

endIf:
	END { () }
| ENDIF { () };
	
endInitialize:
	END { () }
| ENDINITIALIZE { () };

endNetwork:
	END { () }
| ENDNETWORK { () };
	
endPriority:
	END { () }
| ENDPRIORITY { () };

endProcDecl:
	END { () }
| ENDPROCEDURE { () };

endWhile:
	END { () }
| ENDWHILE { () };

entityDecl:
	ID EQUAL entityExpr SEMICOLON
	{
		{ Calast.e_name = $1;
		e_expr = $3;
		e_filename = "";
		e_child = Calast.Actor Calast.empty_actor }
	};
| calType ID EQUAL entityExpr SEMICOLON
	{
		{ Calast.e_name = $2;
		e_expr = $4;
		e_filename = "";
		e_child = Calast.Actor Calast.empty_actor }
	};

entityDeclsRev:
	{ [] }
| entityDeclsRev entityDecl { $2 :: $1 };

entityDecls: entityDeclsRev { List.rev $1 };

entityExpr:
	ID LPAREN entityParsOpt RPAREN attributeSectionOpt
	{ Calast.DirectInst ($1, $3) }
| IF expression THEN entityExpr ELSE entityExpr END
	{ Calast.CondInst ($2, $4, $6) }
| LBRACKET { failwith "Cal_parser.entityExpr: generator???" };

entityPar:
  error expression {
	let ps = symbol_end_pos () in
	print_pos ps "parameter name missing";
	("", $2) };
| ID EQUAL expression { ($1, $3) };
| ID DOUBLE_COLON expression { ($1, $3) };

entityParsRev:
	entityPar { [$1] }
| entityParsRev COMMA entityPar { $3 :: $1 };

entityPars: entityParsRev { List.rev $1 };

entityParsOpt:
	{ [] }
| entityPars { $1 };

entityRef: ID entityRefExpressions { $1 };

entityRefExpressions:
	{ () }
| LBRACKET expression RBRACKET entityRefExpressions { () };

entitySectionOpt:
	{ [] }
| ENTITIES entityDecls { $2 };

expression:
	expressionUnary { $1 }

| expression AND expression { Calast.BinaryOp ($1, Calast.And, $3) }
| expression OR expression { Calast.BinaryOp ($1, Calast.Or, $3) }
| expression EQUAL expression { Calast.BinaryOp ($1, Calast.Equal, $3) }
| expression DIFFERENT expression { Calast.BinaryOp ($1, Calast.NotEqual, $3) }
| expression LT expression { Calast.BinaryOp ($1, Calast.LessThan, $3) }
| expression LE expression { Calast.BinaryOp ($1, Calast.LessThanOrEqual, $3) }
| expression GT expression { Calast.BinaryOp ($1, Calast.GreaterThan, $3) }
| expression GE expression { Calast.BinaryOp ($1, Calast.GreaterThanOrEqual, $3) }
| expression PLUS expression { Calast.BinaryOp ($1, Calast.Plus, $3) }
| expression MINUS expression { Calast.BinaryOp ($1, Calast.Minus, $3) }
| expression DIV expression { Calast.BinaryOp ($1, Calast.Div, $3) }
| expression MOD expression { Calast.BinaryOp ($1, Calast.Mod, $3) }
| expression TIMES expression { Calast.BinaryOp ($1, Calast.Times, $3) }
| expression SLASH expression { Calast.BinaryOp ($1, Calast.Div, $3) };

expressionUnary:
	expressionPostfix { $1 }

| NOT expressionPostfix { Calast.UnaryOp (Calast.Not, $2) }
| MINUS expressionPostfix %prec UMINUS { Calast.UnaryOp (Calast.UMinus, $2) }
| SHARP expressionPostfix { failwith "Cal_parser.primaryExpression: # operator not implemented" }
| DOM expressionPostfix { failwith "Cal_parser.primaryExpression: dom operator not implemented" }
| RNG expressionPostfix { failwith "Cal_parser.primaryExpression: rng operator not implemented" };

expressionPostfix:
	expressionPrimary { $1 }
| expressionPostfix expressionBracket { Calast.Indexer ($1, $2) }
| expressionPostfix expressionParen { Calast.Application ($1, $2) }
| expressionPostfix DOT ID {
	match $1 with
		| Calast.Var id ->
			Calast.Var (id ^ "_" ^ $3)
		| e ->
			let ps = symbol_end_pos () in
			print_pos ps "Unexpected . in file";
			e };

expressionParen:
	LPAREN RPAREN { [] }
| LPAREN expressions RPAREN { $2 };

expressionBracket:
	LBRACKET expressionsBracket RBRACKET { $2 };

expressionsBracket:
	expression { $1 }
| expressionsBracket COMMA expression { Calast.Indexer ($1, $3) };

expressionPrimary:
	ID { var_of_id $1 }
| OLD ID { var_of_id ("_old_" ^ $2) }
| expressionLiteral { Calast.Literal $1 }
| INT DOUBLE_DOT INT
  { Calast.Application
	(Calast.Var "Integers",
	[Calast.Literal (Calast.Integer $1);
	Calast.Literal (Calast.Integer $3)]) }
| LPAREN expression RPAREN { $2 }
| LBRACKET expressionListContent RBRACKET { Calast.List $2 }
| IF expression THEN expression ELSE expression endIf { Calast.If ($2, $4, $6) };

expressionLiteral:
	INT { Calast.Integer $1 }
| FLOAT { Calast.Real $1 }
| STRING { Calast.String $1 }
| TRUE { Calast.Boolean true }
| FALSE { Calast.Boolean false }
| NULL { Calast.Null };

expressionGenerator:
	FOR ID IN expression
	{
		{ Calast.d_name = $2;
			d_type = generic_type;
			d_value = Some $4 }
	}
| FOR calType ID IN expression
	{ 
		{ Calast.d_name = $3;
			d_type = $2;
			d_value = Some $5 }
	};

expressionGenerators: expressionGeneratorsRev { List.rev $1 };

expressionGeneratorsRev:
	expressionGenerator { [$1] }
| expressionGeneratorsRev COMMA expressionGenerator { $3::$1 };
	
expressionListContent:
	{ Calast.Comprehension [] }
| expressions { Calast.Comprehension $1 }
| expressions COLON expressionGenerators { Calast.Generator ($1, $3) };

expressionsRev:
	expression { [$1] }
| expressionsRev COMMA expression { $3 :: $1 };
	
expressions: expressionsRev { List.rev $1 };

formalPar:
	ID
	{
		{ Calast.d_name = $1;
			d_type = generic_type;
			d_value = None; }
	}
| calType ID
	{
		{ Calast.d_name = $2;
			d_type = $1;
			d_value = None; }
	};

formalParsRev:
	formalPar { [$1] }
| formalParsRev COMMA formalPar { $3 :: $1 };

formalPars: formalParsRev { List.rev $1 };

formalParsOpt:
	{ [] }
| formalPars { $1 }; 

funDecl: FUNCTION ID LPAREN formalParsOpt RPAREN funReturnTypeOpt varDeclSectionOpt COLON
 expression endFunDecl
{ function_declaration $6 $2 $4 $7 $9 };

funReturnTypeOpt:
	{ generic_type }
| DOUBLE_DASH_ARROW calType { $2 };

groupImport: IMPORT ALL qualID { () } ;

guardOpt:
	{ [] }
| GUARD expressions { $2 };

ids:
	ID { {Calast.d_name = $1; d_type = generic_type; d_value = None} }
| ids COMMA ID
	{
		failwith "ID LIST NOT SUPPORTED"
	};

import:
	singleImport { () }
| groupImport { () };

importsRev:
	{ [()] }
| importsRev import SEMICOLON { $2 :: $1 };

imports: importsRev { List.rev $1 };

initializationAction:
	INITIALIZE initializationBody { () }
| qualID INITIALIZE initializationBody { () };

initializationBody:
	DOUBLE_EQUAL_ARROW outputExpressionsOpt guardOpt varDeclSectionOpt delayOpt
	DO statements endInitialize { $7 };

ioSignature: portDeclsOpt DOUBLE_EQUAL_ARROW portDeclsOpt { ($1, $3) };

/* Network */
network: NETWORK qualID calTypeParsOpt LPAREN parsOpt RPAREN ioSignature COLON imports
 topVarDeclSectionOpt networks entitySectionOpt structureSectionOpt endNetwork EOF
	{
		let (inputs, outputs) = $7 in
		(!has_error, { Calast.n_entities = $12;
			n_inputs = inputs;
			n_locals = $10;
			n_name = $2;
			n_outputs = outputs;
			n_parameters = $5;
			n_structure = $13 })
		};

networksRev:
	{ [] }
| networksRev network { $2 :: $1 };

networks: networksRev { List.rev $1 };

idOpt:
	{ "" }
| ID COLON { $1 };

outputExpression:
	idOpt LBRACKET expressions RBRACKET repeatClauseOpt
	{ match $3 with
		| [e] ->
			let decl =
				{Calast.d_name = $1;
				d_type = generic_type;
				d_value = Some e}
			in
			(decl, $5)
		| list ->
			let decl =
				{Calast.d_name = $1;
				d_type = generic_type;
				d_value = Some (Calast.List (Calast.Comprehension list))}
			in
			(decl, $5) };

outputExpressionsRev:
	outputExpression { [$1] }
| outputExpressionsRev COMMA outputExpression { $3 :: $1 }; 

outputExpressions: outputExpressionsRev { List.rev $1 };

outputExpressionsOpt:
	{ [] }
| outputExpressions { $1 };

parsOpt:
	{ [] }
| pars { $1 };

parsRev:
	parameter { [$1] }
| parsRev COMMA parameter { $3 :: $1 };

pars: parsRev { List.rev $1 };

parameter: parameterName parameterValueOpt { {$1 with Calast.d_value = $2} };

parameterName:
	ID { {Calast.d_name = $1; d_type = generic_type; d_value = None} }
| calType ID { {Calast.d_name = $2; d_type = $1; d_value = None} };

parameterValueOpt:
	{ None }
| EQUAL expression { Some $2 };

portDecl:
	ID portDeclExpressions
	{ 
		{Calast.d_name = $1;
		d_type = generic_type;
		d_value = None}
	}
| calType ID portDeclExpressions { {Calast.d_name = $2; d_type = $1; d_value = None} };

portDeclExpressions:
	{ [] }
| portDeclExpressions LPAREN expression RPAREN { [] };

portDeclsOpt:
	{ [] }
| portDecls { $1 };

portDeclsRev:
	portDecl { [$1] }
| portDeclsRev COMMA portDecl { $3 :: $1 };

portDecls: portDeclsRev { List.rev $1 };

priorityInequalities:
	{ [] }
| priorityInequalities priorityInequality SEMICOLON { (List.rev $2) :: $1 };

priorityInequality:
	qualID GT qualID { [$3; $1] }
| priorityInequality GT qualID { $3 :: $1 };

priorityOrder:
  PRIORITY priorityInequalities endPriority { List.rev $2 }; 

procDecl: PROCEDURE ID LPAREN formalParsOpt RPAREN varDeclSectionOpt beginProcDecl
 statements endProcDecl
{
	let (s, return_type) =
		(IS.empty, Calast.Type.TP Calast.Type.Unit)
	in
	function_declaration (s, return_type) $2 $4 $6 $8 };

qualID:
	ID { $1 }
| qualID DOT ID { $1 ^ "." ^ $3 } ;

repeatClauseOpt:
	{ Calast.Literal (Calast.Integer 1) }
| REPEAT expression { $2 };

schedule:
	scheduleFSM { Some $1 }
| scheduleRegexp { None };

scheduleEndKeyword:
	END { () }
| ENDSCHEDULE { () };

scheduleEnd:
	scheduleEndKeyword { () }
| scheduleEndKeyword SEMICOLON {
	let ps = symbol_end_pos () in
	print_pos ps "Unnecessary ;";
	};

scheduleFSM: SCHEDULE FSM ID COLON scheduleTransitions scheduleEnd
{ ($3, $5) };

scheduleRegexp: SCHEDULE REGEXP
{ let ps = symbol_end_pos () in
  print_pos ps "Unsupported schedule type: regexp" };

scheduleTransition:
	ID LPAREN qualID RPAREN DOUBLE_DASH_ARROW ID {
		($1, $3, $6) };

scheduleTransitions: scheduleTransitionsRev { List.rev $1 };

scheduleTransitionsRev:
	{ [] }
| scheduleTransition SEMICOLON scheduleTransitionsRev { $1 :: $3 };

singleImport:
	IMPORT qualID { () }
| IMPORT qualID EQUAL ID { () } ;

statements:
  statement { $1 }
| statements statement { Calast.Statements ($1, $2) };

statement:
  expressionPostfix COLON_EQUAL expression SEMICOLON { Calast.Assign ($1, $3) }
| expressionPostfix SEMICOLON { $1 };
| IF expression THEN statements stmtIfElseOpt endIf { Calast.If ($2, $4, $5) }
| WHILE expression DO statements endWhile { Calast.While ($2, $4) };

stmtIfElseOpt:
	{ Calast.Unit }
| ELSE statements { $2 };

structureSectionOpt:
	{ [] }
| STRUCTURE structureStmts { $2 };

structureStmt:
	connector DOUBLE_DASH_ARROW connector attributeSectionOpt SEMICOLON { ($1, $3) };

structureStmts:
	{ [] }
| structureStmts structureStmt { $2 :: $1 };
	
topVarDecl:
	error {
	let ps = symbol_end_pos () in
	print_pos ps "Missing ;";
	{ Calast.d_name = ""; d_type = generic_type; d_value = None } }
| MUTABLE varDeclType SEMICOLON { $2 }
| varDeclType SEMICOLON { $1 }
| funDecl { $1 }
| procDecl { $1 };

topVarDeclsOpt:
	{ [] }
| topVarDeclsOpt topVarDecl { $2 :: $1 };

topVarDeclSection: VAR topVarDeclsOpt { $2 };

topVarDeclSectionOpt:
	{ [] }
| topVarDeclSection { $1 };
;

varDecl:
	MUTABLE varDeclType { $2 }
| varDeclType { $1 }
| funDecl { $1 }
| procDecl { $1 };

varDeclType:
	ID varRest
	{
		{Calast.d_name = $1; d_type = generic_type; d_value = $2}
	}
| calType ID varRest
	{
		{Calast.d_name = $2; d_type = $1; d_value = $3}
	};

varRest:
	{ None }
| EQUAL expression { Some $2 }
| COLON_EQUAL expression { Some $2 };

varDecls: varDeclsRev { List.rev $1 };

varDeclsOpt:
	{ [] }
| varDecls { $1 };

varDeclsRev:
	varDecl { [$1] }
| varDeclsRev COMMA varDecl { $3 :: $1 };

varDeclSection: VAR varDeclsOpt { $2 };

varDeclSectionOpt:
	{ [] }
| varDeclSection { $1 };

%%
