(*****************************************************************************)
(* CAL2C                                                                     *)
(* Copyright (c) 2007-2008, IETR/INSA of Rennes.                             *)
(* All rights reserved.                                                      *)
(*                                                                           *)
(* This software is governed by the CeCILL-B license under French law and    *)
(* abiding by the rules of distribution of free software. You can use,       *)
(* modify and/or redistribute the software under the terms of the CeCILL-B   *)
(* license as circulated by CEA, CNRS and INRIA at the following URL         *)
(* "http://www.cecill.info".                                                 *)
(*                                                                           *)
(* Matthieu WIPLIEZ <Matthieu.Wipliez@insa-rennes.fr                         *)
(*****************************************************************************)

(** This module provides a parser for CALML files. *)

(** [calast_of_calml source] loads CAL XML (CALML) content from the file whose
name is [source], and calls [network_of] to extract a CAL AST from
the CALML - encoded top network. *)
val calast_of_calml : string -> Calast.network
