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

(** Translation of CAL expressions to CIL. *)

val cst_unit : Cil.exp
  
(** [exp_of_expr f stmts expr] returns a [(exp, stmts)] couple. [exp] is the
 resulting CIL expression. [stmts] is a concatenation of the given statement
 list and [Cil.stmt] statement list created from [expr]. *)
val exp_of_expr :
  Cil.fundec ->
    Cil.stmt Clist.clist -> Calast.expr -> (Cil.exp * (Cil.stmt Clist.clist))
  
