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

module E = Exception.Make(struct let module_name = "Json_calast"
	end)

open E

open Printf

type xml =
	| Node of (string * Xmlm.attribute list * xml list * Xmlm.pos)
	| Text of (string * Xmlm.pos)

let field list key = List.assoc ("", key) list

let optfield list key = try Some (field list key) with | Not_found -> None

(** Matches either \ or $. Why so many backslashes? Because \ has to be
escaped in strings, so we get \\. \, | and $ also have to be escaped in regexps,
so we have \\\\ \\| \\$. *)
let re_id = Str.regexp "\\\\\\|\\$"

(** [validate_id id] makes sure [id] is not a C/C++ keyword. If [id] is a
C / C ++ keyword, it is converted to a valid identifier. *)
let validate_id id =
	let id = Str.global_replace re_id "_" id
	in
	match id with
	| "signed" -> "_cal_signed"
	| "OUT" -> "out"
	| "BTYPE" -> "btype"
	| "IN" -> "in"
	| id -> id

(** [partition elements key] returns a pair of lists [(l1, l2)], where l1 is
the list of all [elements] that have the name [key], and l2 is the list
of all [elements] that do not. The order of the elements in the input
list is preserved. *)
let partition elements key =
	let (yes, no) =
		List.fold_left
			(fun (yes, no) element ->
						match element with
						| Node (name, _, _, _) ->
								if name = key
								then ((element :: yes), no)
								else (yes, (element :: no))
						| Text _ -> raise Not_found)
			([], []) elements in
	let yes = List.rev yes in let no = List.rev no in (yes, no)

let failwithpos element msg =
	let (line, column) =
		match element with
		| Text (_, pos) -> pos
		| Node (_, _, _, pos) -> pos
	in
	failwith (sprintf "Line %i, column %i: %s" line column msg)

let expect_element element expected =
	match element with
	| Text (str, _) -> failwithpos element ("Unexpected text node: " ^ str)
	| Node (name, _, _, _) when name <> expected ->
			failwithpos element
			(sprintf "Expected \"%s\" node, got \"%s\"" expected name)
	| Node (name, attributes, children, _) -> (name, attributes, children)

(** [assert_empty list] makes sure that there is no more elements in [list].
Otherwise it prints its contents, and asserts false. *)
let assert_empty list =
	if list = []
	then ()
	else
		(List.iter
				(fun element ->
							match element with
							| Node (name, attributes, _, _) ->
									(print_endline name;
										List.iter
											(fun ((_, name), value) ->
														printf "attribute %s = \"%s\"\n" name value)
											attributes)
							| Text (str, _) -> print_endline str)
				list;
			assert false)

(** [literal_of attributes] returns a [Calast.Literal name] where [literal] is
the literal whose kind is defined by "literal-kind" JSON attribute, and value
is read from "value" JSON attribute. *)
let literal_of element =
	let (_, attributes, _) = expect_element element "Literal" in
	let value () = field attributes "value" in
	let literal = field attributes "literal-kind" in
	let literal =
		match literal with
		| "Boolean" -> Calast.Boolean ((int_of_string (value ())) <> 0)
		| "Integer" -> Calast.Integer (int_of_string (value ()))
		| "Null" -> Calast.Null
		| "Real" -> Calast.Real (float_of_string (value ()))
		| "String" -> Calast.String (value ())
		| kind -> failwithpos element ("Unknown literal kind: " ^ kind)
	in Calast.Literal literal

(** [var_of attributes] returns a [Calast.Var name] where [name] is the
variable name read from "name" JSON attribute. *)
let var_of element =
	let (_, attributes, _) = expect_element element "Var"
	in Calast.Var (validate_id (field attributes "name"))

(** [decl_of (_, attributes, children)].

[expr_of (_, attributes, children)].

[list_type_of (name, attributes, children)].

[type_of (name, attributes, children)] returns a [Calast.Type.definition]
translated from the type encoded in [attributes] and [children].

*)
let rec application_of element =
	let (_, _, children) = expect_element element "Application"
	in
	match children with
	| [ expr; args ] -> Calast.Application (expr_of expr, args_of args)
	| _ -> failwith "Application: syntax error"

and args_of element =
	let (_, _, children) = expect_element element "Args"
	in List.map expr_of children

and assign_of element =
	let (_, attributes, children) = expect_element element "Assign" in
	let target = Calast.Var (validate_id (field attributes "name")) in
	let (args, children) = partition children "Args" in
	let (exprs, children) = partition children "Expr"
	in
	(assert_empty children;
		match (args, exprs) with
		| ([], [ e ]) -> Calast.Assign (target, expr_of e)
		| ([ e1 ], [ e2 ]) ->
				let args = args_of e1 in
				let indexer =
					List.fold_left (fun expr arg -> Calast.Indexer (expr, arg)) target
						args
				in Calast.Assign (indexer, expr_of e2)
		| _ -> failwith "Assign: syntax error")

and binary_of element =
	let (_, _, _) = expect_element element "BinOp"
	in
	Calast.BinaryOp (Calast.Literal (Calast.Integer 0), Calast.Plus,
		Calast.Literal (Calast.Integer 0))

and block_of element =
	let (_, _, children) = expect_element element "Block" in
	let stmts = List.map expr_of children
	in
	match stmts with
	| [] -> Calast.Unit
	| expr :: stmts ->
			List.fold_right (fun stmt expr -> Calast.Statements (stmt, expr))
				stmts expr

and decl_of element =
	let (_, attributes, children) = expect_element element "Decl" in
	let name = validate_id (field attributes "name") in
	let (types, children) = partition children "Type" in
	let t =
		match types with
		| [ t ] -> type_of t
		| _ -> ((IS.singleton 0), (Calast.Type.TV 0)) in
	let (exprs, children) = partition children "Expr" in
	let v = match exprs with | [ e ] -> Some (expr_of e) | _ -> None
	in
	(assert_empty children;
		{ Calast.d_name = name; d_type = t; d_value = v; })

and entry_of element =
	let (_, attributes, children) = expect_element element "Entry" in
	let name = validate_id (field attributes "name")
	in
	(ignore children;
		printf "Ignoring Entry element %s\n" name;
		Calast.Var name)

and expr_of element =
	let (_, attributes, _) = expect_element element "Expr" in
	let kind = field attributes "kind" in
	let expr =
		match kind with
		| "Application" -> application_of element
		| "Assign" -> assign_of element
		| "BinOpSeq" -> binary_of element
		| "Block" -> block_of element
		| "Call" -> application_of element
		| "Entry" -> entry_of element
		| "If" -> if_of element
		| "Indexer" -> indexer_of element
		| "Lambda" -> lambda_of element
		| "Let" -> let_of element
		| "List" -> Calast.List (list_of element)
		| "Literal" -> literal_of element
		| "Proc" -> proc_of element
		| "UnaryOp" -> unary_of element
		| "Var" -> var_of element
		| "While" -> while_of element
		| _ -> failwith ("Unknown expression type: " ^ kind)
	in expr

and generator_of element =
	let (_, _, children) = expect_element element "Generator"
	in
	match children with
	| [ decl; expr ] ->
			let decl = decl_of decl
			in { (decl) with Calast.d_value = Some (expr_of expr); }
	| _ -> failwith "Generator: incorrect number of nodes"

and if_of element =
	let (_, _, children) = expect_element element "If" in
	let children = List.map expr_of children
	in
	match children with
	| [ e1; e2 ] -> Calast.If (e1, e2, Calast.Unit)
	| [ e1; e2; e3 ] -> Calast.If (e1, e2, e3)
	| _ -> failwith "If: syntax error"

and indexer_of element =
	let (_, _, children) = expect_element element "Indexer"
	in
	match children with
	| [ expr; args ] ->
			let args = args_of args
			in
			List.fold_left (fun expr arg -> Calast.Indexer (expr, arg))
				(expr_of expr) args
	| _ -> failwith "Indexer: syntax error"

and lambda_of element =
	let (_, _, children) = expect_element element "Lambda" in
	let (decls, children) = partition children "Decl" in
	let (exprs, children) = partition children "Expr" in
	let (_types, children) = partition children "Type" in
	let decls = List.map decl_of decls in
	let exprs = List.map expr_of exprs
	in
	(assert ((List.length exprs) = 1);
		assert_empty children;
		Calast.Function (decls, [], List.hd exprs))

and let_of element =
	let (_, _, children) = expect_element element "Let"
	in
	match children with
	| [ expr ] -> expr_of expr
	| [ decl; expr ] ->
			let decl = decl_of decl in
			let expr = expr_of expr
			in
			(match (expr, (decl.Calast.d_value)) with
				| (Calast.Var varname, Some v) when decl.Calast.d_name = varname
				-> v
				| _ -> failwith "Let: syntax error")
	| _ -> failwith "Let: syntax error"

and list_of element =
	let (_, _, children) = expect_element element "List" in
	let (exprs, children) = partition children "Expr"
	in
	if children = []
	then Calast.Comprehension (List.map expr_of exprs)
	else
		(let list = List.map expr_of exprs
			in Calast.Generator (list, List.map generator_of children))

and list_type_of children =
	let (entries, children) = partition children "Entry"
	in
	(assert_empty children;
		let (types, sizes) =
			List.partition
				(fun element ->
					let (_, attributes, _) = expect_element element "Entry" in
								field attributes "kind" = "Type")
				entries
		in
		match types with
		| [ Node (_, _, [ t ], _) ] ->
				let (is, t) = type_of t
				in
				(match sizes with
					| [] -> (is, (Calast.Type.TL (None, t)))
					| [ Node (_, _, [ expr ], _) ] ->
							let expr = expr_of expr
							in (ignore expr; (is, (Calast.Type.TL (None, t))))
					| _ -> assert false)
		| _ -> assert false)

and proc_of element =
	let (_, _, children) = expect_element element "Proc" in
	let (decls, children) = partition children "Decl" in
	let (stmts, children) = partition children "Stmt" in
	let (_types, children) = partition children "Type" in
	let decls = List.map decl_of decls
	in (assert_empty children; Calast.Function (decls, [], stmt_of stmts))

and stmt_of stmts =
	expr_of (Node ("Block", [ (("", "kind"), "Block") ], stmts, (-1, -1)))

and type_of element =
	let (_, attributes, children) = expect_element element "Type" in
	let name = optfield attributes "name"
	in
	match name with
	| None -> ((IS.singleton 0), (Calast.Type.TV 0))
	| Some name ->
			(match name with
				| "bool" ->
						(assert_empty children;
							(IS.empty, (Calast.Type.TP Calast.Type.Bool)))
				| "int" ->
						let (_entries, children) = partition children "Entry"
						in
						(assert_empty children;
							(IS.empty, (Calast.Type.TP Calast.Type.Int)))
				| "list" | "List" -> list_type_of children
				| "string" ->
						(assert_empty children;
							(IS.empty, (Calast.Type.TP Calast.Type.String)))
				| n -> failwith (n ^ " type is not known"))

and unary_of element =
	let (_, _, children) = expect_element element "Unop"
	in
	match children with
	| [ Node (_, attributes, [], _); expr ] ->
			let op = field attributes "name" in
			let op =
				(match op with
					| "-" -> Calast.UMinus
					| "not" -> Calast.Not
					| _ -> failwith "UnaryOp: syntax error") in
			let expr = expr_of expr in Calast.UnaryOp (op, expr)
	| _ -> failwithpos element "UnaryOp: syntax error"

and while_of element =
	let (_, _, children) = expect_element element "While"
	in
	match children with
	| [ expr; block ] -> Calast.While (expr_of expr, expr_of block)
	| _ -> failwith "While: syntax error"

(** [inputs_of ] *)
let inputs_of inputs =
	List.map
		(fun element ->
					let (_, attributes, children) = expect_element element "Port" in
					let port = validate_id (field attributes "port") in
					let (decls, children) = partition children "Decl" in
					let (repeats, children) = partition children "Repeat" in
					let decl =
						match decls with | [ decl ] -> decl_of decl | _ -> assert false in
					let repeat =
						match repeats with
						| [] -> Calast.Literal (Calast.Integer 1)
						| [ Node (_, [], [ repeat ], _) ] -> expr_of repeat
						| _ -> assert false
					in (assert_empty children; (port, decl, repeat)))
		inputs

(** [outputs_of ] *)
let outputs_of outputs =
	List.map
		(fun element ->
					let (_, attributes, children) = expect_element element "Port" in
					let port = validate_id (field attributes "port") in
					let (exprs, children) = partition children "Expr" in
					let (repeats, children) = partition children "Repeat" in
					let expr =
						match exprs with
						| [ expr ] -> expr_of expr
						| exprs ->
								let list = List.map expr_of exprs
								in Calast.List (Calast.Comprehension list) in
					let repeat =
						match repeats with
						| [] -> Calast.Literal (Calast.Integer 1)
						| [ Node (_, [], [ repeat ], _) ] -> expr_of repeat
						| _ -> assert false in
					let decl =
						{
							Calast.d_name = port;
							d_type = ((IS.singleton 0), (Calast.Type.TV 0));
							d_value = Some expr;
						}
					in (assert_empty children; (decl, repeat)))
		outputs

(** [action_of ] *)
let action_of element =
	let (_, _, children) = expect_element element "Action" in
	let (locals, children) = partition children "Decl" in
	let (delays, children) = partition children "Delay" in
	let (guards, children) = partition children "Guards" in
	let (inputs, children) = partition children "Input" in
	let (outputs, children) = partition children "Output" in
	let (qids, children) = partition children "QID" in
	let (stmts, children) = partition children "Stmt" in
	let delay =
		match delays with
		| [] -> Calast.Literal (Calast.Integer 1)
		| [ Node (_, _, [ child ], _) ] -> expr_of child
		| _ -> failwith "Delay: syntax error" in
	let guards =
		match guards with
		| [] -> []
		| [ Node (_, _, children, _) ] -> List.map expr_of children
		| _ -> failwith "Guards: syntax error" in
	let inputs = inputs_of inputs in
	let locals = List.map decl_of locals in
	let name =
		match qids with
		| [] -> "unnamed"
		| [ Node (_, attributes, _, _) ] -> validate_id (field attributes "name")
		| _ -> assert false in
	let outputs = outputs_of outputs in
	let stmts = stmt_of stmts
	in
	(assert_empty children;
		{
			Calast.a_delay = delay;
			a_guards = guards;
			a_inputs = inputs;
			a_locals = locals;
			a_name = name;
			a_outputs = outputs;
			a_stmts = stmts;
		})

(** [priorities_of priorities] returns the priorities contained in the JSON
list [priorities]. *)
let priorities_of priorities =
	List.map
		(fun element ->
					let (_, _, children) = expect_element element "Priority" in
					let (qids, children) = partition children "QID"
					in
					(assert_empty children;
						List.map
							(fun element ->
										let (_, attributes, _) = expect_element element "QID" in
										validate_id (field attributes "name"))
							qids))
		priorities

(** [structure_of connections] returns the network structure from
[connections]. *)
let structure_of connections =
	List.map
		(fun element ->
					let (_, attributes, _) = expect_element element "Connection" in
					let src = validate_id (field attributes "src") in
					let src_port = validate_id (field attributes "src-port") in
					let dst = validate_id (field attributes "dst") in
					let dst_port = validate_id (field attributes "dst-port") in
					let sl = [ src_port ] in
					let sl = if src = "" then sl else src :: sl in
					let dl = [ dst_port ] in
					let dl = if src = "" then dl else dst :: dl in (sl, dl))
		connections

(** [actor_of (_, attributes, children)] returns a [Calast.actor] record from
JSON content.

[entity_of (_, attributes, children)] returns an [Calast.entity] from JSON
content. This entity is an instantiation of a [Calast.Actor actor] or
a [Calast.Network network]. The function calls [actor_of] or [network_of]
if [children] contains an "Actor" or a "Network" respectively.

[network_of (name, attributes, children)] returns a [Calast.network] from a
JSON element. It reads the name, ports, structure and entities.

*)
let rec actor_of element =
	let (_, attributes, children) = expect_element element "Actor" in
	let (actions, children) = partition children "Action" in
	let (decls, children) = partition children "Decl" in
	let (_imports, children) = partition children "Import" in
	let (_initializers, children) = partition children "Initializer" in
	let (_notes, children) = partition children "Note" in
	let (ports, children) = partition children "Port" in
	let (priorities, children) = partition children "Priority" in
	let (_schedule, children) = partition children "Schedule" in
	let (_type_parameters, children) =
		partition children "TypeParameter" in
	let (locals, parameters) =
		List.partition
			(fun element ->
						let (_, attributes, _) = expect_element element "Decl" in
						(field attributes "kind") = "Variable")
			decls in
	let (inputs, outputs) =
		List.partition
			(fun element ->
						let (_, attributes, _) = expect_element element "Decl" in
						(field attributes "kind") = "Input")
			ports in
	let actions = List.map action_of actions in
	let fsm = None in
	let inputs = List.map decl_of inputs in
	let locals = List.map decl_of locals in
	let name = validate_id (field attributes "name") in
	let outputs = List.map decl_of outputs in
	let parameters = List.map decl_of parameters in
	let priorities = priorities_of priorities
	in
	(assert_empty children;
		{
			Calast.ac_actions = actions;
			Calast.ac_fsm = fsm;
			Calast.ac_inputs = inputs;
			Calast.ac_locals = locals;
			Calast.ac_name = name;
			Calast.ac_outputs = outputs;
			Calast.ac_parameters = parameters;
			Calast.ac_priorities = priorities;
		})

and entity_of element =
	let (_, attributes, children) = expect_element element "Entity" in
	let id = validate_id (field attributes "id") in
	let (actors, children) = partition children "Actor" in
	let (_attributes, children) = partition children "Attribute" in
	let (classes, children) = partition children "Class" in
	let (networks, children) = partition children "Network" in
	let (_notes, children) = partition children "Note" in
	let (parameters, children) = partition children "Parameter" in
	let child =
		(match (actors, networks) with
			| ([ actor ], []) -> Calast.Actor (actor_of actor)
			| ([], [ network ]) -> Calast.Network (network_of network)
			| _ -> assert false) in
	let clasz =
		(match classes with
			| [ Node (_, attributes, _, _) ] ->
					validate_id (field attributes "name")
			| _ -> assert false) in
	let parameters = List.map decl_of parameters in
	let entity =
		{
			Calast.e_name = id;
			e_expr = Calast.DirectInst (clasz, parameters);
			e_child = child;
		}
	in (assert_empty children; entity)

and network_of element =
	match element with
	| Text (str, _) -> failwithpos element ("Unexpected text node: " ^ str)
	| Node (name, attributes, children, _) ->
			if name = "XDF" then
				(let (connections, children) = partition children "Connection" in
					let (instances, children) = partition children "Instance" in
					let (_notes, children) = partition children "Note" in
					let (parameters, children) = partition children "Parameter" in
					let (ports, children) = partition children "Port" in
					let (inputs, outputs) =
						List.partition
							(fun element ->
										let (_, attributes, _) = expect_element element "Port" in
										(field attributes "kind") = "Input")
							ports in
					let entities = List.map entity_of instances in
					let inputs = List.map decl_of inputs in
					let name = field attributes "name" in
					let outputs = List.map decl_of outputs in
					let parameters = List.map decl_of parameters in
					let structure = structure_of connections
					in
					(assert_empty children;
						{
							Calast.n_entities = entities;
							n_inputs = inputs;
							n_locals = [];
							n_name = name;
							n_outputs = outputs;
							n_parameters = parameters;
							n_structure = structure;
						}))
			else
				failwith (sprintf "Expected \"XDF\" node, got \"%s\"" name)

let calast_of_calml source =
	let inch = open_in_bin source in
	let input = Xmlm.make_input ~strip: true (`Channel inch) in
	let el ((_ns, name), attributes) children =
		let pos = Xmlm.pos input in
		Node (name, attributes, children, pos)
	in
	let data str =
		let pos = Xmlm.pos input in
		Text (str, pos)
	in
	let (_dtd, xml) = Xmlm.input_doc_tree ~el ~data input in
	network_of xml