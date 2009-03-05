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

(** CAL -> CIL functions. *)

val varinfo_of_var_name : string -> Cil.varinfo
(** [varinfo_of_var_name varname] returns the associated [Cil.varinfo]. It proceeds
 by first looking in the locals, then parameters, and finally globals. If it
 is still not found, a global variable is created. *)

val lval_of_var_name : string -> Cil.lval
(** [lval_of_var_name varname] returns the associated [Cil.lval]. It proceeds
 by first looking in the locals, then parameters, and finally globals. If it
 is still not found, a global variable is created. *)

val add_global : string -> Cil.varinfo -> unit
(** [add_global varname var] adds a variable to the globals. *)

val get_local : Cil.fundec -> string -> Cil.typ -> Cil.varinfo
(** [get_local f varname typ] tries to get the local [varname] from the
 locals table. If it is not present, adds the variable to the locals. *)

val add_parameter : Cil.fundec -> string -> Cil.typ -> Cil.varinfo
(** [add_local f varname typ] adds a variable to the parameters. *)

val add_temp : Cil.fundec -> string -> Cil.typ -> Cil.varinfo
(** [add_temp f varname typ] adds a variable to the temps. *)

val fresh_file : unit -> Cil.file
(** [fresh_file ()] returns the default Cil.file with default values. *)

val clear_locals : unit -> unit
val clear_parameters : unit -> unit
val cil_of_type : Calast.Type.definition -> Cil.typ
