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
  
let error = ref false
  
(** Transforms CAL and NL [files] in the output directory [od], starting from
 the top network [main]. *)
let transform debug od libcal json_source =
  let network = Json_calast.calast_of_json json_source
  in
    (if !error
     then failwith "Please correct the above errors and try again"
     else ();
     Transl_main.translate debug od libcal network)
  
let check_include_m4 includes =
  if
    List.exists
      (fun inc_dir ->
         let files = Sys.readdir inc_dir in
         let m4_exists = ref false in
         let i = ref 0
         in
           (while (!i < (Array.length files)) && (not !m4_exists) do
              (let filename = Filename.concat inc_dir files.(!i)
               in m4_exists := Filename.check_suffix filename "m4");
              incr i done;
            !m4_exists))
      includes
  then true
  else
    (print_endline "No *.m4 file could be found! Please check the -I option.";
     false)
  
let file_exists includes file =
  try (ignore (find_file includes file); true) with | Not_found -> false
  
(** [check_includes includes] checks the "-I" option: libcal.c and a m4 file
 should be found. *)
let check_includes includes =
  if file_exists includes "libcal.c"
  then
    if
      (file_exists includes "SDL.h") ||
        (file_exists includes "include\\SDL.h")
    then
      if
        (file_exists includes "include/systemc.h") ||
          (file_exists includes "src\\systemc.h")
      then
        if file_exists includes "include/tlm.h"
        then
          if check_include_m4 includes
          then true
          else
            (print_endline
               "No *.m4 file could not be found! Please check the -I option.";
             false)
        else
          (print_endline
             "tlm.h could not be found! Please check the -I option.";
           false)
      else
        (print_endline
           "systemc.h could not be found! Please check the -I option.";
         false)
    else
      (print_endline "SDL.h could not be found! Please check the -I option.";
       false)
  else
    (print_endline "libcal.c could not be found! Please check the -I option.";
     false)
  
(** [init_options ()] sets, checks and returns the options passed to Cal2C. *)
let init_options () =
  let d_general = ref false in
  let d_type_inference = ref false in
  let includes = ref [] in
  let json_source = ref "" in
  let o = ref "" in
  let speclist =
    [ ("-debug", (Arg.Set d_general), "Prints debugging information");
      ("-debug-ti", (Arg.Set d_type_inference),
       "Prints debugging information about type inference");
      ("-I", (Arg.String (fun str -> includes := str :: !includes)),
       "<directory> Adds <directory> to the include folders (actually, \
			 only libcal is needed right now)");
      ("-o", (Arg.Set_string o),
       "<directory> Defines the directory where output files will be placed. \
		Defaults to .") ] in
  let usage = "cal2c [options] <json source file>" in
  let anon_fun = (:=) json_source in
  let () = Arg.parse speclist anon_fun usage in
  let cwd = Sys.getcwd () in
  let includes =
    List.map
      (fun inc_dir ->
         if Filename.is_relative inc_dir
         then Filename.concat cwd inc_dir
         else inc_dir)
      !includes
  in
    if check_includes includes
    then
      (let debug =
         { d_general = !d_general; d_type_inference = !d_type_inference; } in
       let o = if !o = "" then "." else !o
       in (debug, o, includes, !json_source))
    else (Arg.usage speclist usage; exit (-1))
  
(** main function *)
let _ =
  let (debug, o, includes, json_source) = init_options ()
  in (Cil.initCIL (); transform debug o includes json_source)
  
