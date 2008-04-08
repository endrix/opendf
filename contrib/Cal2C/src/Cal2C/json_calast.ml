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
  
let network_of_json json =
  let array = Browse.array json
  in
    match array with
    | [ json_name; attributes; children ] ->
        let name = Browse.string json_name in
        let attributes = Browse.array attributes in
        let children = Browse.objekt children
        in
          if name <> "XDF"
          then Browse.type_mismatch "XDF" json_name
          else
            {
              Calast.n_entities = [];
              n_inputs = [];
              n_locals = [];
              n_name = "toto";
              n_outputs = [];
              n_parameters = [];
              n_structure = [];
            }
    | _ -> Browse.type_mismatch "XDF" (Array array)
  
let calast_of_json json_source =
  try let json = Json_io.load_json json_source in network_of_json json
  with
  | Json_type.Json_error err ->
      let msg = sprintf "\"%s\": %s" json_source err in failwith msg
  
