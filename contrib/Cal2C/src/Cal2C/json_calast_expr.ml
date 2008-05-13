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
  
module E = Exception.Make(struct let module_name = "Json_calast_expr"
                                    end)
  
open E
  
open Printf
  
open Json_type
  
open Browse

(** [browse json] returns a tuple (name, attributes, children) that holds
 the information of the JSON element [json]. *)
let browse json =
  let json_array = array json in
  let (name, attributes, children) =
    match json_array with
    | [ name; children ] ->
        let name = string name in
        let children = array children in (name, [], children)
    | [ name; attributes; children ] ->
        let name = string name in
        let attributes = objekt attributes in
        let children = array children in (name, attributes, children)
    | _ ->
        type_mismatch "[ name, {attributes...}, [ children ] ] "
          (Array json_array)
  in
    (* let attributes = make_table attributes in *)
    (name, attributes, children)
  
(** [build json] returns a JSON array made from the tuple
 (name, attributes, children). *)
let build (name, attributes, children) =
  Build.array
    [ Build.string name; Build.objekt attributes; Build.array children ]
  
(** Should it be removed when conversion is fully implemented? *)
let field (tbl : (string * Json_type.t) list) key = List.assoc key tbl
  
(** Should it be removed when conversion is fully implemented? *)
let optfield (tbl : (string * Json_type.t) list) key =
  try Some (field tbl key) with | Not_found -> None
  
(** Matches either \ or $. Why so many backslashes? Because \ has to be
 escaped in strings, so we get \\. \, | and $ also have to be escaped in regexps,
 so we have \\\\ \\| \\$. *)
let re_id = Str.regexp "\\\\\\|\\$"
  
(** [ident json] reads [json] as a string, and makes sure it is a valid
 identifier in C and C++. *)
let ident json =
  let ident = string json in
  let ident = Str.global_replace re_id "_" ident
  in
    match ident with
    | "signed" -> "_cal_signed"
    | "OUT" -> "out"
    | "BTYPE" -> "btype"
    | "IN" -> "in"
    | _ -> ident
  
(** [partition elements key] returns a pair of lists [(l1, l2)], where l1 is
 the list of all [elements] that have the name [key], and l2 is the list
 of all [elements] that do not. The order of the elements in the input
 list is preserved. *)
let partition elements key =
  let (yes, no) =
    List.fold_left
      (fun (yes, no) element ->
         let (name, attributes, children) = browse element
         in
           if name = key
           then (((name, attributes, children) :: yes), no)
           else (yes, (element :: no)))
      ([], []) elements in
  let yes = List.rev yes in let no = List.rev no in (yes, no)
  
(** [literal_of attributes] returns a [Calast.Literal name] where [literal] is
 the literal whose kind is defined by "literal-kind" JSON attribute, and value
 is read from "value" JSON attribute. *)
let literal_of (_, attributes, _) =
  let value () = string (field attributes "value") in
  let literal = string (field attributes "literal-kind") in
  let literal =
    match literal with
    | "Boolean" -> Calast.Boolean ((int_of_string (value ())) <> 0)
    | "Integer" -> Calast.Integer (int_of_string (value ()))
    | "Null" -> Calast.Null
    | "Real" -> Calast.Real (float_of_string (value ()))
    | "String" -> Calast.String (value ())
    | kind -> failwith ("Unknown literal kind: " ^ kind)
  in Calast.Literal literal

(** [assert_empty list] makes sure that there is no more elements in [list].
Otherwise it prints its contents, and asserts false. *)
let assert_empty list =
	if list = [] then
		()
	else (
		List.iter (fun json ->
			let (name, attributes, _) = browse json in
			print_endline name;
			List.iter (fun (name, value) ->
				printf "attribute %s = \"%s\"\n" name (string value))
				attributes) list;
		assert false)
		  
(** [var_of attributes] returns a [Calast.Var name] where [name] is the
 variable name read from "name" JSON attribute. *)
let var_of (_, attributes, _) = Calast.Var (ident (field attributes "name"))
  
let rec parse_fun subfun op_list children =
  let (e, children) = subfun children
  in
    match children with
    | ("Op", attributes, []) :: t ->
        let op = string (field attributes "name")
        in
          (try
             let op = List.assoc op op_list in
             let (e2, children) = parse_fun subfun op_list t
             in ((Calast.BinaryOp (e, op, e2)), children)
           with | Not_found -> (e, children))
    | _ -> (e, children)
  
(** [decl_of (_, attributes, children)]. 

[expr_of (_, attributes, children)]. 

[list_type_of (name, attributes, children)].

[type_of (name, attributes, children)] returns a [Calast.Type.definition]
 translated from the type encoded in [attributes] and [children]. 

*)
let rec application_of (_, _, children) =
  let children = List.map browse children
  in
    match children with
    | [ expr; args ] -> Calast.Application (expr_of expr, args_of args)
    | _ -> failwith "Application: syntax error"

and args_of (_, _, children) =
  List.map (fun json -> expr_of (browse json)) children

and assign_of (_, attributes, children) =
  let target = Calast.Var (ident (field attributes "name")) in
  let (args, children) = partition children "Args" in
  let (exprs, children) = partition children "Expr"
  in
    (assert (children = []);
     match (args, exprs) with
     | ([], [ e ]) -> Calast.Assign (target, expr_of e)
     | ([ e1 ], [ e2 ]) ->
         let args = args_of e1 in
         let indexer =
           List.fold_left (fun expr arg -> Calast.Indexer (expr, arg)) target
             args
         in Calast.Assign (indexer, expr_of e2)
     | _ -> failwith "Assign: syntax error")

and binary_of (_, _, children) =
  let parse_literal =
    function
    | [] -> failwith "parse_literal: no literal!"
    | h :: t -> ((expr_of h), t) in
  let parse_e5 =
    parse_fun parse_literal [ ("*", Calast.Times); ("/", Calast.Div) ] in
  let parse_e4 =
    parse_fun parse_e5 [ ("+", Calast.Plus); ("-", Calast.Minus) ] in
  let parse_e2 =
    parse_fun parse_e4
      [ ("=", Calast.Equal); ("!=", Calast.NotEqual); ("<", Calast.LessThan);
        ("<=", Calast.LessThanOrEqual); (">", Calast.GreaterThan);
        (">=", Calast.GreaterThanOrEqual) ] in
  let rec parse_e children =
    let (e, children) = parse_e2 children
    in
      match children with
      | [] -> e
      | ("Op", attributes, []) :: t ->
          let op = string (field attributes "name")
          in
            (match op with
             | "and" ->
                 let e2 = parse_e t in Calast.BinaryOp (e, Calast.And, e2)
             | "or" ->
                 let e2 = parse_e t in Calast.BinaryOp (e, Calast.Or, e2)
             | _ -> failwith "Incorrect syntax")
      | _ -> failwith "Incorrect syntax"
  in parse_e (List.map browse children)

and block_of (_, _, children) =
  let children = List.map browse children in
  let stmts = List.map expr_of children
  in
    match stmts with
    | [] -> Calast.Unit
    | expr :: stmts ->
        List.fold_right (fun stmt expr -> Calast.Statements (stmt, expr))
          stmts expr

and decl_of (_, attributes, children) =
  let name = ident (field attributes "name") in
  let (types, children) = partition children "Type" in
  let t =
    match types with
    | [ t ] -> type_of t
    | _ -> ((IS.singleton 0), (Calast.Type.TV 0)) in
  let (exprs, children) = partition children "Expr" in
  let v = match exprs with | [ e ] -> Some (expr_of e) | _ -> None
  in
    (assert (children = []);
     { Calast.d_name = name; d_type = t; d_value = v; })

and entry_of (_, attributes, children) =
  let name = ident (field attributes "name") in
	ignore children;
	printf "Ignoring Entry element %s\n" name;
  Calast.Var name

and expr_of (((_, attributes, _) as expr)) =
  let kind = string (field attributes "kind") in
  let expr =
    match kind with
    | "Application" -> application_of expr
    | "Assign" -> assign_of expr
    | "BinOpSeq" -> binary_of expr
    | "Block" -> block_of expr
    | "Call" -> application_of expr
		| "Entry" -> entry_of expr
    | "If" -> if_of expr
    | "Indexer" -> indexer_of expr
    | "Lambda" -> lambda_of expr
    | "Let" -> let_of expr
    | "List" -> Calast.List (list_of expr)
    | "Literal" -> literal_of expr
    | "Proc" -> proc_of expr
    | "UnaryOp" -> unary_of expr
    | "Var" -> var_of expr
    | "While" -> while_of expr
    | _ -> failwith ("not supported: " ^ kind)
  in expr

and generator_of (_, _, children) =
  match children with
  | [ decl; expr ] ->
      let decl = decl_of (browse decl)
      in { (decl) with Calast.d_value = Some (expr_of (browse expr)); }
  | _ -> failwith "Generator: incorrect number of nodes"

and if_of (_, _, children) =
  let children = List.map (fun json -> expr_of (browse json)) children
  in
    match children with
    | [ e1; e2 ] -> Calast.If (e1, e2, Calast.Unit)
    | [ e1; e2; e3 ] -> Calast.If (e1, e2, e3)
    | _ -> failwith "If: syntax error"

and indexer_of (_, _, children) =
  match children with
  | [ expr; args ] ->
      let args = args_of (browse args)
      in
        List.fold_left (fun expr arg -> Calast.Indexer (expr, arg))
          (expr_of (browse expr)) args
  | _ -> failwith "Indexer: syntax error"

and lambda_of (_, _, children) =
  let (decls, children) = partition children "Decl" in
  let (exprs, children) = partition children "Expr" in
  let (_types, children) = partition children "Type" in
  let decls = List.map decl_of decls in
  let exprs = List.map expr_of exprs
  in
    (assert ((List.length exprs) = 1);
     assert (children = []);
     Calast.Function (decls, [], List.hd exprs))

and let_of (_, _, children) =
  let children = List.map browse children
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

and list_of (_, _, children) =
  let (exprs, children) = partition children "Expr"
  in
    if children = []
    then Calast.Comprehension (List.map expr_of exprs)
    else
      (let list = List.map expr_of exprs in
       let children = List.map browse children
       in Calast.Generator (list, List.map generator_of children))

and list_type_of children =
  let (entries, children) = partition children "Entry"
  in
    (assert (children = []);
     let (types, sizes) =
       List.partition
         (fun (_, attributes, _) ->
            (string (field attributes "kind")) = "Type")
         entries
     in
       match types with
       | [ (_, _, [ t ]) ] ->
           let (is, t) = type_of (browse t)
           in
             (match sizes with
              | [] -> (is, (Calast.Type.TL (None, t)))
              | [ (_, _, [ expr ]) ] ->
                  let expr = expr_of (browse expr)
                  in (ignore expr; (is, (Calast.Type.TL (None, t))))
              | _ -> assert false)
       | _ -> assert false)

and proc_of (_, _, children) =
  let (decls, children) = partition children "Decl" in
  let (stmts, children) = partition children "Stmt" in
  let (_types, children) = partition children "Type" in
  let decls = List.map decl_of decls
  in (assert (children = []); Calast.Function (decls, [], stmt_of stmts))

and stmt_of stmts =
  let children = List.map build stmts
  in expr_of ("Block", [ ("kind", (Build.string "Block")) ], children)

and type_of (_, attributes, children) =
  let name = optfield attributes "name"
  in
    match name with
    | None -> ((IS.singleton 0), (Calast.Type.TV 0))
    | Some name ->
        let name = string name
        in
          (match name with
           | "bool" ->
               (assert (children = []);
                (IS.empty, (Calast.Type.TP Calast.Type.Bool)))
           | "int" ->
               let (_entries, children) = partition children "Entry"
               in
                 (assert (children = []);
                  (IS.empty, (Calast.Type.TP Calast.Type.Int)))
           | "list" | "List" -> list_type_of children
           | "string" ->
               (assert (children = []);
                (IS.empty, (Calast.Type.TP Calast.Type.String)))
           | n -> failwith (n ^ " type is not known"))

and unary_of (_, _, children) =
  let children = List.map browse children
  in
    match children with
    | [ (_, attributes, []); expr ] ->
        let op = string (field attributes "name") in
        let op =
          (match op with
           | "-" -> Calast.UMinus
           | "not" -> Calast.Not
           | _ -> failwith "UnaryOp: syntax error") in
        let expr = expr_of expr in Calast.UnaryOp (op, expr)
    | _ -> failwith "UnaryOp: syntax error"

and while_of (_, _, children) =
  let children = List.map browse children
  in
    match children with
    | [ expr; block ] -> Calast.While (expr_of expr, expr_of block)
    | _ -> failwith "While: syntax error"
  
