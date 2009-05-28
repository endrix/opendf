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

(** This module defines type inference. *)

(**1 This module holds type inference definitions. *)

type environment = Calast.decl Cal2c_util.SM.t

val string_of_type : Calast.Type.definition -> string
(** [string_of_type t] returns a string representation of [t]. *)

val string_of_type_scheme : Calast.Type.scheme -> string
(** [string_of_type_scheme ts] returns a string representation of [ts]. *)

val string_of_env : environment -> string
(** [string_of_env env] returns a string representation of [env]. *)

val initial_environment : environment
(** [initial_environment] is an environment containing predefined CAL
 functions types. *)

val mgu : environment -> bool -> Calast.Type.definition -> Calast.Type.definition ->
	Calast.Type.substitution
(** [unify t1 t2] tries to unify [t1] and [t2], and returns the substitution
 necessary to a successful unification. *)

val type_of_expr : bool -> environment -> Calast.expr -> Calast.Type.scheme
(** [type_of_expr debug env e] returns the type [t] of expression [e].
 [e] is first converted to lambda calculus, and then typed. *)
