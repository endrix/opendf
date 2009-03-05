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

(** Util. *)

(** A int hash table. *)
module IH : Hashtbl.S with type key = int

module IM : Map.S with type key = int

module IS : Set.S with type elt = int

(** A string hash table. *)
module SH : Hashtbl.S with type key = string

module SM : Map.S with type key = string

module SS : Set.S with type elt = string

val chop_extensions : string -> string

type debug_options = {
	d_general : bool;
	d_type_inference : bool;
}

module Exception : sig
	module Make : functor (M : sig val module_name : string end) -> sig
		val failwith : string -> 'a
	end
end

(** [get_output_of f] calls [write_tmp_output f], opens an input channel
 inch on the file, and calls [read_whole_file inch]. *)
val get_output_of : (out_channel -> unit) -> string

(** [m4 includes filename f] executes
 ["m4 -I include0 ... -I includen > " ^ filename]. The output of the [f]
 function is fed into m4. *)
val m4 : string list -> string -> (out_channel -> unit) -> unit

(** [find_file includes file] returns the include directory that contains
 [file], if such file exists, or raises Not_found. *)
val find_file : string list -> string -> string

val re_dot : Str.regexp
