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
  
(** [parse debug pf file] calls the parsing function [pf] on
 [file]. [pf] is typically [Cal_parser.actor] or [Cal_parser.network]. *)
let parse debug parse_function file =
  (if debug.d_general
   then print_endline ("Parsing \"" ^ (file ^ "\"..."))
   else ();
   let inch =
     try open_in file with | Sys_error s -> (prerr_endline s; exit 1) in
   let lexbuf = Lexing.from_channel inch in
   let (e, res) =
     try parse_function Cal_lexer.token lexbuf
     with
     | Parsing.Parse_error ->
         (Printf.printf "Line %i, column %i: parse error\n%!"
            lexbuf.Lexing.lex_curr_p.Lexing.pos_lnum
            (lexbuf.Lexing.lex_curr_p.Lexing.pos_cnum -
               lexbuf.Lexing.lex_curr_p.Lexing.pos_bol);
          raise Parsing.Parse_error)
   in (error := e; close_in inch; res))
  
(** [parse_children debug files file] parses [file] as a network, and
 recursively parses its children. *)
let rec parse_children debug files file =
  let network = parse debug Cal_parser.network file in
  (* creates a set of referenced actors/networks names *)
  let set =
    List.fold_left
      (fun set entity ->
         match entity.Calast.e_expr with
         | Calast.DirectInst (name, _) -> SS.add name set
         | _ ->
             failwith
               "parse_children: conditional instantiation not supported")
      SS.empty network.Calast.n_entities in
  (* parses them and returns an assoc list *)
  let entities =
    SS.fold
      (fun name entities ->
         let file =
           List.find
             (fun file -> name = (chop_extensions (Filename.basename file)))
             files in
         let node =
           if Filename.check_suffix file ".nl"
           then Calast.Network (parse_children debug files file)
           else Calast.Actor (parse debug Cal_parser.actor file)
         in (name, node) :: entities)
      set [] in
  (* builds the network *)
  let entities =
    List.map
      (fun entity ->
         match entity.Calast.e_expr with
         | Calast.DirectInst (name, _) ->
             let node = List.assoc name entities
             in { (entity) with Calast.e_filename = file; e_child = node; }
         | _ ->
             failwith
               "parse_children: conditional instantiation not supported")
      network.Calast.n_entities
  in { (network) with Calast.n_entities = entities; }
  
(** Transforms CAL and NL [files] in the output directory [od], starting from
 the top network [main]. *)
let transform debug od libcal main files =
  let network = parse_children debug files main
  in
    (if !error
     then failwith "Please correct the above errors and try again"
     else ();
     Transl_main.translate debug od libcal network)
  
(** [check_main main] checks the "-main" option. The main network should
 exist and not be a directory. *)
let check_main main =
  if main <> ""
  then
    if Sys.file_exists main
    then
      if not (Sys.is_directory main)
      then true
      else
        (print_endline "The main network specified is a directory."; false)
    else
      (print_endline
         "The main network does not exist in the specified model path.";
       false)
  else
    (print_endline
       "No main network has been defined. \
		       Please provide this information using the -main option";
     false)
  
(** [check_mp mp] checks the "-mp" option. The model path should exist and
 be a directory. *)
let check_mp mp =
  if mp <> ""
  then
    if Sys.file_exists mp
    then
      if Sys.is_directory mp
      then true
      else
        (print_endline "The model path specified is not a directory."; false)
    else (print_endline "The model path specified does not exist."; false)
  else (print_endline "No model path has been defined."; false)
  
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
  let main = ref "" in
  let mp = ref "" in
  let o = ref "" in
  let speclist =
    [ ("-debug", (Arg.Set d_general), "Prints debugging information");
      ("-debug-ti", (Arg.Set d_type_inference),
       "Prints debugging information about type inference");
      ("-I", (Arg.String (fun str -> includes := str :: !includes)),
       "<directory> Adds <directory> to the include folders (actually, \
			 only libcal is needed right now)");
      ("-mp", (Arg.Set_string mp),
       "<directory> Defines the directory where input files are located");
      ("-o", (Arg.Set_string o),
       " < directory > Defines the directory where output files will be placed. \
		Defaults to .") ] in
  let usage = "cal2c -mp <dir> [options] <top NL>" in
  let anon_fun filename = main := filename ^ ".nl" in
  let () = Arg.parse speclist anon_fun usage in
  let main = Filename.concat !mp !main in
  let cwd = Sys.getcwd () in
  let includes =
    List.map
      (fun inc_dir ->
         if Filename.is_relative inc_dir
         then Filename.concat cwd inc_dir
         else inc_dir)
      !includes
  in
    if (check_mp !mp) && ((check_main main) && (check_includes includes))
    then
      (let debug =
         { d_general = !d_general; d_type_inference = !d_type_inference; } in
       let files = Array.to_list (Sys.readdir !mp) in
       let files =
         List.filter
           (fun filename ->
              (Filename.check_suffix filename ".cal") ||
                (Filename.check_suffix filename ".nl"))
           files in
       let files = List.map (Filename.concat !mp) files in
       let o = if !o = "" then "." else !o
       in (debug, o, includes, main, files))
    else (Arg.usage speclist usage; exit (-1))
  
(** main function *)
let _ =
  let (debug, o, includes, main, files) = init_options ()
  in (Cil.initCIL (); transform debug o includes main files)
  
