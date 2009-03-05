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
  
module IH =
  Hashtbl.Make
    (struct
       type t = int
       
       let equal : int -> int -> bool = ( = )
         
       let hash key : int = key
         
     end)
  
module IM = Map.Make(struct type t = int

                             let compare = compare
                                end)
  
module IS = Set.Make(struct type t = int

                             let compare = compare
                                end)
  
module SH =
  Hashtbl.Make
    (struct type t = string

             let equal = ( = )

                let hash = Hashtbl.hash
                   end)
  
module SM = Map.Make(struct type t = string

                             let compare = String.compare
                                end)
  
module SS = Set.Make(struct type t = string

                             let compare = String.compare
                                end)
  
let rec chop_extensions filename =
  try chop_extensions (Filename.chop_extension filename)
  with | Invalid_argument _ -> filename
  
type debug_options = { d_general : bool; d_type_inference : bool }

module Exception =
  struct
    module Make (M : sig val module_name : string
                            end) =
      struct let failwith s = failwith (M.module_name ^ (": " ^ s))
                end
      
  end
  
(** [read_whole_file inch] reads as much as it can from the [Unix.file_descr]
 [fdin]. To do this, we have a buffer (named [buf]) of length [maxlen] (here
 4096); we first try to read 4096 bytes. Then, while we can still read
 [!nb_read > 0] and the buffer is not full [!total_len < maxlen], we try
 to read again. When the loop ends, either there is no more to read, or the
 buffer is full. If the latter is true, we read again! *)
let read_whole_file inch =
  let maxlen = 4096 in
  let rec aux file =
    let buf = String.create maxlen in
    let nb_read = ref (input inch buf 0 maxlen) in
    let total_len = ref !nb_read
    in
      (while (!nb_read > 0) && (!total_len < maxlen) do
         nb_read := input inch buf !total_len (maxlen - !total_len);
         total_len := !total_len + !nb_read done;
       let contents = String.sub buf 0 !total_len in
       let contents = file ^ contents
       in if !total_len = maxlen then aux contents else contents)
  in aux ""
  
(** [write_tmp_output f] writes the output of [f] to a temporary file, and
 returns this filename. *)
let write_tmp_output f =
  let fn = Filename.temp_file "cal2c_" ".c" in
  let out = open_out fn in (f out; close_out out; fn)
  
let get_output_of f =
  let fn = write_tmp_output f in
  let inch = open_in fn in
  let contents = read_whole_file inch
  in (close_in inch; Sys.remove fn; contents)
  
let find_file includes file =
  List.find
    (fun inc_dir ->
       let filename = Filename.concat inc_dir file
       in Sys.file_exists filename) includes
  
let m4 includes filename f =
  let includes = List.map (sprintf "-I\"%s\"") includes in
  let includes = String.concat " " includes in
  let command = sprintf "m4 %s > %s" includes filename in
  let outch = Unix.open_process_out command
  in
    (f outch;
     match Unix.close_process_out outch with
     | Unix.WEXITED 0 -> ()
     | _ ->
         fprintf stderr "The command \"%s\" did not end correctly.\n" command)
  
let re_dot = Str.regexp "\\."
  
