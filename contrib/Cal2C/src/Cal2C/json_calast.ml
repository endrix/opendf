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
  
open Json_calast_expr
  
(** [inputs_of ] *)
let inputs_of inputs =
  List.map
    (fun (_, attributes, children) ->
       let port = ident (field attributes "port") in
       let (decls, children) = partition children "Decl" in
       let (repeats, children) = partition children "Repeat" in
       let decl =
         match decls with | [ decl ] -> decl_of decl | _ -> assert false in
       let repeat =
         match repeats with
         | [] -> Calast.Literal (Calast.Integer 1)
         | [ (_, [], [ repeat ]) ] -> expr_of (browse repeat)
         | _ -> assert false
       in (assert (children = []); (port, decl, repeat)))
    inputs
  
(** [outputs_of ] *)
let outputs_of outputs =
  List.map
    (fun (_, attributes, children) ->
       let port = ident (field attributes "port") in
       let (exprs, children) = partition children "Expr" in
       let (repeats, children) = partition children "Repeat" in
       let expr =
         match exprs with
         | [ expr ] -> expr_of expr
         | exprs ->
             let list = List.map expr_of exprs
             in Calast.List (Calast.Comprehension list) in
       let repeat =
         match repeats with
         | [] -> Calast.Literal (Calast.Integer 1)
         | [ (_, [], [ repeat ]) ] -> expr_of (browse repeat)
         | _ -> assert false in
       let decl =
         {
           Calast.d_name = port;
           d_type = ((IS.singleton 0), (Calast.Type.TV 0));
           d_value = Some expr;
         }
       in (assert (children = []); (decl, repeat)))
    outputs
  
(** [action_of ] *)
let action_of (_, _, children) =
  let (locals, children) = partition children "Decl" in
  let (delays, children) = partition children "Delay" in
  let (guards, children) = partition children "Guards" in
  let (inputs, children) = partition children "Input" in
  let (outputs, children) = partition children "Output" in
  let (qids, children) = partition children "QID" in
  let (stmts, children) = partition children "Stmt" in
  let delay =
    match delays with
    | [] -> Calast.Literal (Calast.Integer 1)
    | [ (_, _, [ child ]) ] -> expr_of (browse child)
    | _ -> failwith "Delay: syntax error" in
  let guards =
    match guards with
    | [] -> []
    | [ (_, _, children) ] -> List.map expr_of (List.map browse children)
    | _ -> failwith "Guards: syntax error" in
  let inputs = inputs_of inputs in
  let locals = List.map decl_of locals in
  let name =
    match qids with
    | [] -> "unnamed"
    | [ (_, attributes, _) ] -> ident (field attributes "name")
    | _ -> assert false in
  let outputs = outputs_of outputs in
  let stmts = stmt_of stmts
  in
    (assert (children = []);
     {
       Calast.a_delay = delay;
       a_guards = guards;
       a_inputs = inputs;
       a_locals = locals;
       a_name = name;
       a_outputs = outputs;
       a_stmts = stmts;
     })
  
(** [priorities_of priorities] returns the priorities contained in the JSON
 list [priorities]. *)
let priorities_of priorities =
  List.map
    (fun (_, _, children) ->
       let (qids, children) = partition children "QID"
       in
         (assert (children = []);
          List.map
            (fun (_, attributes, _) -> ident (field attributes "name")) qids))
    priorities
  
(** [structure_of connections] returns the network structure from
 [connections]. *)
let structure_of connections =
  List.map
    (fun (_, attributes, _) ->
       let src = ident (field attributes "src") in
       let src_port = ident (field attributes "src-port") in
       let dst = ident (field attributes "dst") in
       let dst_port = ident (field attributes "dst-port") in
       let sl = [ src_port ] in
       let sl = if src = "" then sl else src :: sl in
       let dl = [ dst_port ] in
       let dl = if src = "" then dl else dst :: dl in (sl, dl))
    connections
  
(** [actor_of (_, attributes, children)] returns a [Calast.actor] record from
 JSON content.

[entity_of (_, attributes, children)] returns an [Calast.entity] from JSON
 content. This entity is an instantiation of a [Calast.Actor actor] or
 a [Calast.Network network]. The function calls [actor_of] or [network_of]
 if [children] contains an "Actor" or a "Network" respectively.

[network_of (name, attributes, children)] returns a [Calast.network] from a
 JSON element. It reads the name, ports, structure and entities.

*)
let rec actor_of (_, attributes, children) =
  let (actions, children) = partition children "Action" in
  let (decls, children) = partition children "Decl" in
  let (_imports, children) = partition children "Import" in
	let (_initializers, children) = partition children "Initializer" in
  let (_notes, children) = partition children "Note" in
  let (ports, children) = partition children "Port" in
  let (priorities, children) = partition children "Priority" in
  let (_schedule, children) = partition children "Schedule" in
	let (_type_parameters, children) = partition children "TypeParameter" in
  let (locals, parameters) =
    List.partition
      (fun (_, attributes, _) ->
         (string (field attributes "kind")) = "Variable")
      decls in
  let (inputs, outputs) =
    List.partition
      (fun (_, attributes, _) -> (field attributes "kind") = (String "Input"))
      ports in
  let actions = List.map action_of actions in
  let fsm = None in
  let inputs = List.map decl_of inputs in
  let locals = List.map decl_of locals in
  let name = ident (field attributes "name") in
  let outputs = List.map decl_of outputs in
  let parameters = List.map decl_of parameters in
  let priorities = priorities_of priorities
  in
    (assert_empty children;
     {
       Calast.ac_actions = actions;
       Calast.ac_fsm = fsm;
       Calast.ac_inputs = inputs;
       Calast.ac_locals = locals;
       Calast.ac_name = name;
       Calast.ac_outputs = outputs;
       Calast.ac_parameters = parameters;
       Calast.ac_priorities = priorities;
     })

and entity_of (_, attributes, children) =
  let id = ident (field attributes "id") in
  let (actors, children) = partition children "Actor" in
  let (_attributes, children) = partition children "Attribute" in
  let (classes, children) = partition children "Class" in
  let (networks, children) = partition children "Network" in
  let (_notes, children) = partition children "Note" in
  let (parameters, children) = partition children "Parameter" in
  let child =
    match (actors, networks) with
    | ([ actor ], []) -> Calast.Actor (actor_of actor)
    | ([], [ network ]) -> Calast.Network (network_of network)
    | _ -> assert false in
  let clasz =
    match classes with
    | [ (_, attributes, _) ] -> ident (field attributes "name")
    | _ -> assert false in
  let parameters = List.map decl_of parameters in
  let entity =
    {
      Calast.e_name = id;
      e_expr = Calast.DirectInst (clasz, parameters);
      e_child = child;
    }
  in (assert (children = []); entity)

and network_of (name, attributes, children) =
  if name = "XDF"
  then
    (let (connections, children) = partition children "Connection" in
     let (instances, children) = partition children "Instance" in
     let (_notes, children) = partition children "Note" in
     let (parameters, children) = partition children "Parameter" in
     let (ports, children) = partition children "Port" in
     let (inputs, outputs) =
       List.partition
         (fun (_, attributes, _) ->
            (field attributes "kind") = (String "Input"))
         ports in
     let entities = List.map entity_of instances in
     let inputs = List.map decl_of inputs in
     let name = string (field attributes "name") in
     let outputs = List.map decl_of outputs in
     let parameters = List.map decl_of parameters in
     let structure = structure_of connections
     in
       (assert (children = []);
        {
          Calast.n_entities = entities;
          n_inputs = inputs;
          n_locals = [];
          n_name = name;
          n_outputs = outputs;
          n_parameters = parameters;
          n_structure = structure;
        }))
  else type_mismatch "XDF" (String name)
  
(** [calast_of_json json_source] loads JSON content from the file whose name
 is [json_source], and calls [network_of_json] to extract a CAL AST from
 the JSON-encoded top network. *)
let calast_of_json json_source =
  try let json = Json_io.load_json json_source in network_of (browse json)
  with
  | Json_type.Json_error err ->
      let msg = sprintf "\"%s\": %s" json_source err
      in (print_endline msg; failwith msg)
  
