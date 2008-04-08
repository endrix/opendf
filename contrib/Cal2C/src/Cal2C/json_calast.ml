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
  
module E = Exception.Make(struct let module_name = "Json_calast"
                                    end)
  
open E
  
open Printf
  
open Json_type
  
open Browse
  
(** [contents_of_json json] returns a tuple (name, attributes, children) that
 hold the information of the JSON element [json]. *)
let contents_of_json json =
  let json_array = array json
  in
    match json_array with
    | [ name; attributes; children ] ->
        let name = string name in
        let attributes = objekt attributes in
        (* let attributes = make_table attributes in *)
        let children = array children in (name, attributes, children)
    | _ ->
        type_mismatch "[ name, {attributes...}, [ children ] ] "
          (Array json_array)
  
(** Should it be removed when conversion is fully implemented? *)
let field tbl key = List.assoc key tbl
  
(** Matches either \ or $. Why so many backslashes? Because \ has to be
 escaped in strings, so we get \\. \, | and $ also have to be escaped in regexps,
 so we have \\\\ \\| \\$. *)
let re_id = Str.regexp "\\\\\\|\\$"
  
(** [ident json] reads [json] as a string, and makes sure it is a valid
 identifier in C and C++. *)
let ident json =
  let ident = string json in
  let ident = Str.global_replace re_id "_" ident
  in
    match ident with
    | "signed" -> "_cal_signed"
    | "OUT" -> "out"
    | "BTYPE" -> "btype"
    | "IN" -> "in"
    | _ -> ident
  
(** [partition elements key] returns a pair of lists [(l1, l2)], where l1 is
 the list of all [elements] that have the name [key], and l2 is the list
 of all [elements] that do not. The order of the elements in the input
 list is preserved. *)
let partition elements key =
  let (yes, no) =
    List.fold_left
      (fun (yes, no) element ->
         let (name, attributes, children) = contents_of_json element
         in
           if name = key
           then (((name, attributes, children) :: yes), no)
           else (yes, (element :: no)))
      ([], []) elements in
  let yes = List.rev yes in let no = List.rev no in (yes, no)
  
let decl :
  (string * ((string * Json_type.t) list) * (Json_type.t list)) -> Calast.
    decl =
  fun (name, attributes, children) ->
    (ignore (attributes, children);
     {
       Calast.d_name = name;
       d_type = ((IS.singleton 0), (Calast.Type.TV 0));
       d_value = None;
     })
  
(** [convert_ports ports] returns (inputs, outputs) where [inputs] are
 [Calast.decl] of input ports, [outputs] [Calast.decl] of output ports of the
 actor/network. *)
let convert_ports ports =
  let (inputs, outputs) =
    List.partition
      (fun (_, attributes, _) -> (field attributes "kind") = (String "Input"))
      ports
  in ((List.map decl inputs), (List.map decl outputs))
  
(** Converts the network structure. *)
let convert_structure connections =
  List.map
    (fun (_, attributes, _) ->
       let src = ident (field attributes "src") in
       let src_port = ident (field attributes "src-port") in
       let dst = ident (field attributes "dst") in
       let dst_port = ident (field attributes "dst-port") in
			 (* source *)
       let sl = [ src_port ] in
       let sl = if src = "" then sl else src :: sl in
			 (* destination *)
       let dl = [ dst_port ] in
       let dl = if src = "" then dl else dst :: dl in (sl, dl))
    connections
  
let network_of_json json =
  let (name, attributes, children) = contents_of_json json
  in
    if name = "XDF"
    then
      (let name = string (field attributes "name") in
       let (ports, children) = partition children "Port" in
       let (instances, children) = partition children "Instance" in
       let (connections, children) = partition children "Connection"
       in
         (ignore (instances, connections, children);
				  List.iter (fun element -> ignore (element)) instances;
          let (inputs, outputs) = convert_ports ports in
          let structure = convert_structure connections
          in
            {
              Calast.n_entities = [];
              n_inputs = inputs;
              n_locals = [];
              n_name = name;
              n_outputs = outputs;
              n_parameters = [];
              n_structure = structure;
            }))
    else type_mismatch "XDF" (String name)
  
let calast_of_json json_source =
  try let json = Json_io.load_json json_source in network_of_json json
  with
  | Json_type.Json_error err ->
      let msg = sprintf "\"%s\": %s" json_source err in failwith msg
  
