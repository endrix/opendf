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

module E = Exception.Make(struct let module_name = "Cal2c"
	end)

open E

(** Transforms CAL and NL [files] in the output directory [od], starting from
the top network [main]. *)
let transform debug od libcal json_source =
	let network = Calml_parser.calast_of_calml json_source
	in Transl_main.translate debug od libcal network

let file_exists includes file =
	try (ignore (find_file includes file); true) with | Not_found -> false

(** [init_options ()] sets, checks and returns the options passed to Cal2C. *)
let init_options () =
	let d_general = ref false in
	let d_type_inference = ref false in
	let source = ref "" in
	let o = ref "" in
	let speclist =
		[ ("-debug", (Arg.Set d_general), "Prints debugging information");
		("-debug-ti", (Arg.Set d_type_inference),
			"Prints debugging information about type inference");
		("-o", (Arg.Set_string o),
			" < directory > Defines the directory where output files will be placed. \
			Defaults to .") ] in
	let usage = "cal2c [options] <json source file>" in
	let anon_fun = ( := ) source in
	let () = Arg.parse speclist anon_fun usage in
	let debug =
		{ d_general = !d_general; d_type_inference = !d_type_inference; } in
	let o = if !o = "" then "." else !o in (debug, o, [], (!source))

(** main function *)
let _ =
	let (debug, o, includes, source) = init_options ()
	in (Cil.initCIL (); transform debug o includes source)
