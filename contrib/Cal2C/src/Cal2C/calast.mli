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

module rec Type : sig

	type variable = int
	(** Type variable definition. *)

	type primitive =
		| Bool (** Boolean primitive type. *)
		| Float (** Float primitive type. *)
		| Int (** Integer primitive type. *)
		| String (** String primitive type. *)
		| Unit (** Unit primitive type. *)
  (** Primitive type definition. *)

	type definition =
		| TV of variable (** Variable type definition. *)
		| TP of primitive (** Primitive type definition. *)
		| TL of int option * definition (** List type definition. *)
		| TA of definition * TypeSchemeSet.t * definition
		(** Arrow type definition. *)
		| TR of definition (** Reference type definition. *)
		| Tpair of definition * definition (** Pair type. *)
	(** Type definition syntax. *)

	type substitution = definition Cal2c_util.IM.t
	(** Substitution *)

	type scheme = Cal2c_util.IS.t * definition
	(** Type scheme definition. *)
	end and TypeSchemeSet : Set.S with type elt = Type.scheme

(** AST types. *)

type var = string
(** Variable type: alias for [string]. *)

type literal =
	| Boolean of bool (** CAL boolean literal. *)
	| Integer of int (** CAL integer literal. *)
	| Null (** CAL NULL literal. *)
	| Real of float (** CAL real literal. *)
	| String of string (** CAL string literal. *)
(** CAL literal. *)

type uop =
	| Contents
	| Not (** CAL unary not operator. *)
	| Reference
	| UMinus (** CAL unary minus operator. *)
(** CAL unary operator. *)

type bop =
	| Equal (** CAL equal operator. *)
	| NotEqual (** CAL different operator. *)
	| Plus (** CAL plus operator. *)
	| Minus (** CAL binary minus operator. *)
	| Times (** CAL times operator. *)
	| Div (** CAL divide operator. *)
	| Mod (** CAL modulo operator. *)
	| LessThan (** CAL less than operator. *)
	| LessThanOrEqual (** CAL less than or equal operator. *)
	| GreaterThan (** CAL greater than operator. *)
	| GreaterThanOrEqual (** CAL greater than or equal operator. *)
	| Or (** CAL boolean or operator. *)
	| And (** CAL boolean and operator. *)
(** CAL binary operator. *)

type cal_type = string

type collection =
	| Generator of expr list * decl list (** CAL list generator. *)
	| Comprehension of expr list (** CAL list comprehension. *)
(** CAL collection. *)

and expr =
	| Application of expr * expr list (** CAL function call expression. *)
	| Assign of expr * expr (** CAL assignment. *)
	| BinaryOp of expr * bop * expr (** CAL binary expression. *)
	| Function of decl list * decl list * expr
			(** CAL function declaration expression: params, locals, body. *)
	| If of expr * expr * expr (** CAL if expression. *)
	| Indexer of expr * expr (** CAL array access expression: array, index. *)
	| List of collection (** CAL list expression. *)
	| Literal of literal (** CAL literal expression. *)
	| Statements of expr * expr (** CAL statements. *)
	| Switch of (expr * (expr * expr) list)
	| UnaryOp of uop * expr (** CAL unary expression. *)
	| Unit (** CAL unit. *)
	| Var of var (** CAL variable expression. *)
	| While of expr * expr (** CAL while. *)
 
(** CAL expression. *)

and decl = {
	d_name : string; (** CAL declaration name. *)
	mutable d_type : Type.scheme; (** CAL declaration type. *)
	mutable d_value : expr option; (** CAL (optional) declaration value. *)
		}
(** CAL declaration. *)

module DS : Set.S with type elt = decl

type action = {
	a_name : string; (** CAL action name. *)
	a_inputs : (string * decl * expr) list; (** CAL action input tokens. *)
	a_outputs : (decl * expr) list; (** CAL action output tokens. *)
	a_guards : expr list; (** CAL action guards. *)
	a_decls : decl list; (** CAL action local declarations. *)
	a_delay : expr; (** CAL action delay (what is it used for?). *)
	a_stmts : expr; (** CAL action body statements. *)
}
(** CAL action. *)

type actor = {
	ac_actions : action list; (** CAL actor actions. *)
	ac_fsm : (string * (string * string * string) list) option;
	ac_inputs : decl list; (** CAL actor input ports declarations. *)
	ac_locals : decl list; (** CAL actor local declarations. *)
	ac_name : string; (** CAL actor name. *)
	ac_outputs : decl list; (** CAL actor output ports declarations. *)
	ac_parameters : decl list; (** CAL actor parameter. *)
	ac_priorities : string list list; (** CAL priorities. *)
}
(** CAL actor.*)

type entity_expr =
	| DirectInst of string * (string * expr) list
	| CondInst of expr * entity_expr * entity_expr

type child =
	| Actor of actor (** NL actor child. *)
	| Network of network (** NL network child. *)
(** NL child type. *)

		and entity = {
			e_name : string; (** NL entity declaration name. *)
			e_expr : entity_expr; (** NL entity expression name. *)
			e_filename : string; (** NL entity filename. *)
			e_child : child;
		}
(** NL entity*)

		and network = {
			n_entities : entity list; (** NL network entities. *)
			n_inputs : decl list; (** NL network ports. *)
			n_locals : decl list; (** NL network local declarations. *)
			n_name : string; (** NL network name. *)
			n_outputs : decl list; (** NL network ports. *)
			n_parameters : decl list; (** NL network parameters. *)
			n_structure : (string * string) list;
		}
(** NL network. *)

val empty_actor : actor

val string_of_literal : literal -> string
(** [string_of_literal l] returns a string representation of the CAL literal
 [l]. *)

val string_of_uop : uop -> string
(** [string_of_uop uop] returns a string representation of the CAL unary
 operator [uop]. *)

val string_of_bop : bop -> string
(** [string_of_bop bop] returns a string representation of the CAL binary
 operator [bop]. *)

val decl_hashmap_of_list : decl list -> decl Cal2c_util.SH.t
