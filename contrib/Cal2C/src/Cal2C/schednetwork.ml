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
  
open Cal2c_util
  
module E =
  Cal2c_util.Exception.Make(struct let module_name = "Schednetwork"
                                      end)
  
open E
  
let includes_of_network network =
  let include_set =
    List.fold_left
      (fun include_set entity ->
         match entity.Calast.e_expr with
         | Calast.DirectInst (module_type, _) ->
             SS.add (sprintf "#include \"sched_%s.h\"" module_type)
               include_set
         | Calast.CondInst _ ->
             failwith
               "print_makefile: conditional instantiation not supported")
      SS.empty network.Calast.n_entities
  in String.concat "\n" (SS.elements include_set)
  
(** [ports_of_network network connections] returns a string containing all
 ports of [network] that appear in [connections]. *)
let ports_of_network network connections =
  let ports =
    List.fold_left
      (fun ports decl ->
         if
           List.exists
             (fun (_, p, target) ->
                (target = decl.Calast.d_name) || (p = decl.Calast.d_name))
             connections
         then
           (sprintf "  sc_port<tlm::tlm_fifo_get_if<int> > %s;"
              decl.Calast.d_name) ::
             ports
         else ports)
      [] network.Calast.n_inputs in
  let ports =
    List.fold_left
      (fun ports decl ->
         if
           List.exists (fun (_, _, target) -> target = decl.Calast.d_name)
             connections
         then
           (sprintf "  sc_port<tlm::tlm_fifo_put_if<int> > %s;"
              decl.Calast.d_name) ::
             ports
         else ports)
      ports network.Calast.n_outputs
  in String.concat "\n" (List.rev ports)
  
let string_of_expr expr =
  match expr with
  | Calast.Var var -> var
  | Calast.Literal lit -> Calast.string_of_literal lit
  | _ -> failwith "string_of_expr: expr not supported"
  
let params_of_module instances module_type params =
  let child = SM.find module_type instances in
  let parameters =
    match child with
    | (Calast.Actor actor, _) -> actor.Calast.ac_parameters
    | (Calast.Network network, _) -> network.Calast.n_parameters in
  let parameters =
    List.filter (fun { Calast.d_value = value } -> value = None) parameters in
  let params =
    List.fold_left
      (fun params (name, value) ->
         if
           List.exists (fun { Calast.d_name = dname } -> dname = name)
             parameters
         then (", " ^ (string_of_expr value)) :: params
         else params)
      [] params in
  let params = List.rev params in String.concat "" params
  
(** [modules_of_network network connections] returns the modules declared and
 instantiated in [network], possibly with an additional broadcaster module if
 needed (to have correct [connections]). *)
let modules_of_network instances network modules modules_inst =
  let (modules, modules_inst) =
    List.fold_left
      (fun (modules, modules_inst) entity ->
         let module_name = entity.Calast.e_name
         in
           match entity.Calast.e_expr with
           | Calast.DirectInst (module_type, params) ->
               let module_params =
                 params_of_module instances module_type params in
               let modul =
                 sprintf "  sched_%s %s;" module_type module_name in
               let module_inst =
                 sprintf "    %s(\"%s\"%s)," module_name module_name
                   module_params
               in ((modul :: modules), (module_inst :: modules_inst))
           | Calast.CondInst _ ->
               failwith
                 "print_makefile: conditional instantiation not supported")
      (modules, modules_inst) network.Calast.n_entities
  in
    ((String.concat "\n" (List.rev modules)),
     (String.concat "\n" (List.rev modules_inst)))
  
let is_input_actor network actor instances =
  let entity =
    List.find (fun entity -> entity.Calast.e_name = actor)
      network.Calast.n_entities
  in
    match entity.Calast.e_expr with
    | Calast.DirectInst (actor, _) ->
        (try
           let child = SM.find actor instances
           in
             match child with
             | (Calast.Actor actor, _) when actor.Calast.ac_inputs = [] ->
                 true
             | _ -> false
         with | Not_found -> (printf "not found %s\n" actor; raise Not_found))
    | Calast.CondInst _ ->
        failwith "print_makefile: conditional instantiation not supported"
  
let fifos_of_network network instances =
  List.fold_left
    (fun (fifos, fifos_size) (source, dest) ->
       let sl = Str.split re_dot source in
       let dl = Str.split re_dot dest
       in
         match (sl, dl) with
         | ([ e1; p1 ], [ e2; p2 ]) ->
             let size =
               if is_input_actor network e1 instances then 100 else (-16) in
             let fifo_name = sprintf "%s_%s_%s_%s" e1 p1 e2 p2 in
             let fifo = sprintf "  tlm::tlm_fifo<int> %s;" fifo_name in
             let fifo_size = sprintf "    %s(%i)" fifo_name size
             in ((fifo :: fifos), (fifo_size :: fifos_size))
         | _ -> (fifos, fifos_size))
    ([], []) network.Calast.n_structure
  
(** [group_connections_by_target connections] does what its name suggests.
 It sorts connections and group them by target to allow detection of input
 broadcast. *)
let group_connections_by_target connections ports =
  let connections =
    List.sort
      (fun (e1, p1, target1) (e2, p2, target2) ->
         let res = String.compare target1 target2
         in
           if res = 0
           then
             (let res = String.compare e1 e2
              in if res = 0 then String.compare p1 p2 else res)
           else res)
      connections in
  let (_, connections) =
    List.fold_left
      (fun (last_target, connections) (e, p, target) ->
         let connections =
           if
             (last_target = target) &&
               (List.exists (fun { Calast.d_name = name } -> name = target)
                  ports)
           then
             (match connections with
              | [] -> failwith "no connections"
              | h :: t -> ((e, p, target) :: h) :: t)
           else [ (e, p, target) ] :: connections
         in (target, connections))
      ("", []) connections
  in connections
  
(** [broadcast_input connections] analyses [connections] to see if a
 broadcaster module is necessary, between a network input port and multiple
 targets. If that is the case, it returns new connections using this module.
 Otherwise, connections are returned unmodified. *)
let broadcast_input connections ports fifos fifos_size =
  let connections = group_connections_by_target connections ports in
  let (_, connections, fifos, fifos_size, modules, modules_inst) =
    List.fold_right
      (fun connections_list
         (cnt_broadcast, connections, fifos, fifos_size, modules,
          modules_inst)
         ->
         match connections_list with
         | [] -> failwith "no connections"
         | [ connection ] ->
             (cnt_broadcast, (connection :: connections), fifos, fifos_size,
              modules, modules_inst)
         | (_, _, target) :: _ ->
             let cnt_broadcast = cnt_broadcast + 1 in
             let input =
               ((sprintf "broadcaster_input%i" cnt_broadcast), "input",
                target) in
             let (connections, fifos, fifos_size) =
               List.fold_right
                 (fun (e, p, _) (connections, fifos, fifos_size) ->
                    let fifo_name =
                      sprintf "broadcaster_input%i_output_%s_%s"
                        cnt_broadcast e p in
                    let fifo =
                      sprintf "  tlm::tlm_fifo<int> %s;" fifo_name in
                    let fifo_size = sprintf "    %s(-16)" fifo_name in
                    let broadcast_connection =
                      ((sprintf "broadcaster_input%i" cnt_broadcast),
                       "output", fifo_name) in
                    let fifo_connection = (e, p, fifo_name) in
                    let connections =
                      broadcast_connection :: fifo_connection :: connections
                    in
                      (connections, (fifo :: fifos),
                       (fifo_size :: fifos_size)))
                 connections_list ((input :: connections), fifos, fifos_size) in
             let modul =
               sprintf "  broadcaster_input broadcaster_input%i;"
                 cnt_broadcast in
             let module_inst =
               sprintf "    broadcaster_input%i(\"broadcaster_input%i\"),"
                 cnt_broadcast cnt_broadcast
             in
               (cnt_broadcast, connections, fifos, fifos_size,
                (modul :: modules), (module_inst :: modules_inst)))
      connections (0, [], fifos, fifos_size, [], [])
  in ((List.rev connections), fifos, fifos_size, modules, modules_inst)
  
(** [group_connections_by_entity_and_port connections] does what its name
 suggests. It sorts connections and group them by (entity, port) to allow
 detection of output broadcast. *)
let group_connections_by_entity_and_port connections =
  let is_broadcaster e =
    if (String.length e) > 11
    then (String.sub e 0 11) = "broadcaster"
    else false in
  let connections =
    List.sort
      (fun (e1, p1, target1) (e2, p2, target2) ->
         let res = String.compare e1 e2
         in
           if res = 0
           then
             (let res = String.compare p1 p2
              in if res = 0 then String.compare target1 target2 else res)
           else res)
      connections in
  let (_, connections) =
    List.fold_left
      (fun ((last_e, last_p), connections) (e, p, target) ->
         let connections =
           if (last_e = e) && ((last_p = p) && (not (is_broadcaster e)))
           then
             (match connections with
              | [] -> failwith "no connections"
              | h :: t -> ((e, p, target) :: h) :: t)
           else [ (e, p, target) ] :: connections
         in ((e, p), connections))
      (("", ""), []) connections
  in connections
  
(** [broadcast_output connections] analyses [connections] to see if a
 broadcaster module is necessary, between an output port of a local entity
 to multiple targets. If that is the case, it returns new
 connections using this module. Otherwise, connections are returned
 unmodified. *)
let broadcast_output connections modules modules_inst =
  let connections = group_connections_by_entity_and_port connections in
  let (_, connections, modules, modules_inst) =
    List.fold_right
      (fun connections_list
         (cnt_broadcast, connections, modules, modules_inst) ->
         match connections_list with
         | [] -> failwith "no connections"
         | [ connection ] ->
             (cnt_broadcast, (connection :: connections), modules,
              modules_inst)
         | (e, p, _) :: _ ->
             let cnt_broadcast = cnt_broadcast + 1 in
             let input =
               (e, p, (sprintf "broadcaster_output%i.input" cnt_broadcast)) in
             let connections =
               List.fold_right
                 (fun (_, _, target) connections ->
                    ((sprintf "broadcaster_output%i" cnt_broadcast),
                     "output", target) :: connections)
                 connections_list (input :: connections) in
             let modul =
               sprintf "  broadcaster_output broadcaster_output%i;"
                 cnt_broadcast in
             let module_inst =
               sprintf "    broadcaster_output%i(\"broadcaster_output%i\"),"
                 cnt_broadcast cnt_broadcast
             in
               (cnt_broadcast, connections, (modul :: modules),
                (module_inst :: modules_inst)))
      connections (0, [], modules, modules_inst)
  in ((List.rev connections), modules, modules_inst)
  
(** [connections_of_network network] creates the connections between ports
 of [network]. *)
let connections_of_network network =
  List.fold_left
    (fun connections (source, dest) ->
       let sl = Str.split re_dot source in
       let dl = Str.split re_dot dest
       in
         match (sl, dl) with
         | ([ e1; p1 ], [ e2; p2 ]) ->
             let c1 = (e1, p1, (sprintf "%s_%s_%s_%s" e1 p1 e2 p2)) in
             let c2 = (e2, p2, (sprintf "%s_%s_%s_%s" e1 p1 e2 p2))
             in c1 :: c2 :: connections
         | ([ p1 ], [ e2; p2 ]) -> (e2, p2, p1) :: connections
         | ([ e1; p1 ], [ p2 ]) -> (e1, p1, p2) :: connections
         | _ -> failwith "Invalid connection")
    [] network.Calast.n_structure
  
(** [string_of_connections connections] returns a string representation of
 [connections]. *)
let string_of_connections connections =
  let connections =
    List.map
      (fun (e, p, target) ->
         if e = ""
         then sprintf "    %s(%s);" p target
         else sprintf "    %s.%s(%s);" e p target)
      connections
  in String.concat "\n" connections
  
let parameters_of_network network =
  let parameters =
    List.fold_left
      (fun parameters decl ->
         match decl.Calast.d_value with
         | None -> decl.Calast.d_name :: parameters
         | _ -> parameters)
      [] network.Calast.n_parameters in
  let parameters =
    List.rev_map (fun param -> sprintf ", int %s" param)
      parameters
  in String.concat "" parameters
  
let print_header instances network out =
  let n = network.Calast.n_name in
  let ns = String.uppercase n in
  let includes = includes_of_network network in
  let (fifos, fifos_size) = fifos_of_network network instances in
  let connections = connections_of_network network in
  let (connections, fifos, fifos_size, modules, modules_instantiation) 
    = broadcast_input connections network.Calast.n_inputs fifos fifos_size in
  let (connections, modules, modules_instantiation) =
    broadcast_output connections modules modules_instantiation in
  let (fifos, fifos_size) =
    ((String.concat "\n" fifos), (String.concat ",\n" fifos_size)) in
  let ports = ports_of_network network connections in
  let (modules, modules_instantiation) =
    modules_of_network instances network modules modules_instantiation in
  let connections = string_of_connections connections in
  let parameters = parameters_of_network network
  in
    fprintf out
      "include(%s)dnl
#ifndef SCHED_%s_H
#define SCHED_%s_H

#include \"broadcaster.h\"
%s

SC_MODULE(sched_%s) {
  // Input and output ports
%s

  // Sub-module instantiation
%s

  // Local FIFO channels
%s

  SC_HAS_PROCESS(sched_%s);

  sched_%s(sc_module_name N%s) : sc_module(N),

    // Creates the modules
%s

    // Initializes FIFOs size
%s
  {
    // Connects FIFOs and ports
%s
  }

};

#endif
"
      "tlm.m4" ns ns includes n ports modules fifos n n parameters
      modules_instantiation fifos_size connections
  
(** *)
let translate od includes network instances =
  let filename =
    Filename.concat od ("sched_" ^ (network.Calast.n_name ^ ".h"))
  in m4 includes filename (print_header instances network)
  
