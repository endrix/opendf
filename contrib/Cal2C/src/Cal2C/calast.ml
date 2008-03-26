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

open Printf
  
open Cal2c_util
  
module E = Exception.Make(struct let module_name = "Calast"
                                    end)
  
open E
  
module rec Type :
             sig
               type variable = int
               
               type primitive = | Bool | Float | Int | String | Unit
               
               type definition =
                 | TV of variable
                 | TP of primitive
                 | TL of int option * definition
                 | TA of definition * TypeSchemeSet.t * definition
                 | TR of definition
                 | Tpair of definition * definition
               
               type substitution = definition Cal2c_util.IM.t
               
               type scheme = (Cal2c_util.IS.t * definition)
               
             end =
             struct
               type variable = int
               
               type primitive = | Bool | Float | Int | String | Unit
               
               type definition =
                 | TV of variable
                 | TP of primitive
                 | TL of int option * definition
                 | TA of definition * TypeSchemeSet.t * definition
                 | TR of definition
                 | Tpair of definition * definition
               
               type substitution = definition Cal2c_util.IM.t
               
               type scheme = (Cal2c_util.IS.t * definition)
               
             end
and
  TypeSchemeSet : Set.S with type elt = Type.scheme =
    Set.Make
      (struct
         type t = Type.scheme
         
         let compare (vars1, t1) (vars2, t2) =
           let res = Cal2c_util.IS.compare vars1 vars2
           in if res = 0 then compare t1 t2 else res
           
       end)
  
type var = string

type literal =
  | Boolean of bool
  | Integer of int
  | Null
  | Real of float
  | String of string

type uop = | Contents | Not | Reference | UMinus

type bop =
  | Equal
  | NotEqual
  | Plus
  | Minus
  | Times
  | Div
  | Mod
  | LessThan
  | LessThanOrEqual
  | GreaterThan
  | GreaterThanOrEqual
  | Or
  | And

type cal_type = string

type collection =
  | Generator of expr list * decl list | Comprehension of expr list

  and expr =
  | Application of expr * expr list
  | Assign of expr * expr
  | BinaryOp of expr * bop * expr
  | Function of decl list * decl list * expr
  | If of expr * expr * expr
  | Indexer of expr * expr
  | List of collection
  | Literal of literal
  | Statements of expr * expr
  | Switch of (expr * ((expr * expr) list))
  | UnaryOp of uop * expr
  | Unit
  | Var of var
  | While of expr * expr

  and decl =
  { d_name : string; mutable d_type : Type.scheme;
    mutable d_value : expr option
  }

module DS =
  Set.Make
    (struct
       type t = decl
       
       let compare d1 d2 = String.compare d1.d_name d2.d_name
         
     end)
  
type action =
  { a_name : string; a_inputs : (string * decl * expr) list;
    a_outputs : (decl * expr) list; a_guards : expr list;
    a_decls : decl list; a_delay : expr; a_stmts : expr
  }

type actor =
  { ac_actions : action list;
    ac_fsm : (string * ((string * string * string) list)) option;
    ac_inputs : decl list; ac_locals : decl list; ac_name : string;
    ac_outputs : decl list; ac_parameters : decl list;
    ac_priorities : (string list) list
  }

type entity_expr =
  | DirectInst of string * (string * expr) list
  | CondInst of expr * entity_expr * entity_expr

type entity =
  { e_name : string; e_expr : entity_expr; e_filename : string;
    e_child : child
  }

  and child =
  | Actor of actor | Network of network

  and network =
  { n_entities : entity list; n_inputs : decl list; n_locals : decl list;
    n_name : string; n_outputs : decl list; n_parameters : decl list;
    n_structure : (string * string) list
  }

let empty_actor =
  {
    ac_actions = [];
    ac_fsm = None;
    ac_inputs = [];
    ac_locals = [];
    ac_name = "";
    ac_outputs = [];
    ac_parameters = [];
    ac_priorities = [];
  }
  
let string_of_literal =
  function
  | Boolean b -> string_of_bool b
  | Integer i -> string_of_int i
  | Null -> "NULL"
  | Real f -> string_of_float f
  | String s -> s
  
let string_of_uop =
  function
  | Contents -> "!"
  | Not -> "not"
  | Reference -> "ref"
  | UMinus -> "-"
  
let string_of_bop =
  function
  | And -> "and"
  | Div -> "/"
  | Equal -> "="
  | GreaterThan -> ">"
  | GreaterThanOrEqual -> ">="
  | LessThan -> "<"
  | LessThanOrEqual -> "<="
  | Minus -> "-"
  | Mod -> "mod"
  | NotEqual -> "!="
  | Or -> "or"
  | Plus -> "+"
  | Times -> "*"
  
let decl_hashmap_of_list list =
  let sh = SH.create (List.length list)
  in (List.iter (fun decl -> SH.replace sh decl.d_name decl) list; sh)
  
