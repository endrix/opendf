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

(** This module contains the definition of a visitor class, called calVisitor,
a base visitor ([nopVisitor]) and various visitors. *)

(** The cal visitor class type that visits an [Typedast] AST. *)
class type calVisitor =
  object
    method visitAction : Calast.action -> Calast.action
      
    method visitActor : Calast.actor -> Calast.actor
      
    method visitAssign : Calast.expr -> Calast.expr -> Calast.expr
      
    method visitChild : Calast.child -> Calast.child
      
    method visitCollection : Calast.collection -> Calast.collection
      
    method visitDecl : Calast.decl -> Calast.decl
      
    method visitEntity : Calast.entity -> Calast.entity
      
    method visitEntityExpr : Calast.entity_expr -> Calast.entity_expr
      
    method visitExpr : Calast.expr -> Calast.expr
		
		method visitFsm : (string * (string * string * string) list) option ->
			(string * (string * string * string) list) option
      
    method visitInput :
      (string * Calast.decl * Calast.expr) ->
        (string * Calast.decl * Calast.expr)
      
    method visitNetwork : Calast.network -> Calast.network
      
    method visitOutput :
      (Calast.decl * Calast.expr) -> (Calast.decl * Calast.expr)
      
    method visitVar : Calast.var -> Calast.expr
      
  end
  
class nopVisitor : calVisitor
