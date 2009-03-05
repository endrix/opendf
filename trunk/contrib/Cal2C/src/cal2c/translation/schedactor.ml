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
  Cal2c_util.Exception.Make(struct let module_name = "Schedactor"
                                      end)
  
open E
  
let fifos_of_actor actor =
  let fifos =
    List.fold_left
      (fun fifos decl ->
         (sprintf "  sc_port<tlm::tlm_fifo_get_if<int> > %s;"
            decl.Calast.d_name) ::
           fifos)
      [] actor.Calast.ac_inputs in
  let fifos =
    List.fold_left
      (fun fifos decl ->
         (sprintf "  sc_port<tlm::tlm_fifo_put_if<int> > %s;"
            decl.Calast.d_name) ::
           fifos)
      fifos actor.Calast.ac_outputs in
  let fifos = String.concat "\n" (List.rev fifos) in fifos
  
let parameters_of_actor actor =
  let parameters =
    List.fold_left
      (fun parameters decl ->
         match decl.Calast.d_value with
         | None -> decl.Calast.d_name :: parameters
         | _ -> parameters)
      [] actor.Calast.ac_parameters in
  let parameters = List.rev parameters in
  let members =
    List.map (fun param -> sprintf "  int m_%s;\n" (String.lowercase param))
      parameters in
  let members_init =
    List.map
      (fun param ->
         sprintf ", m_%s(%s)" (String.lowercase param)
           param)
      parameters in
  let parameters =
    List.map (fun param -> sprintf ", int %s" param)
      parameters
  in
    ((String.concat "" members), (String.concat "" parameters),
     (String.concat "" members_init))
  
let print_header out actor =
  let fifos = fifos_of_actor actor in
  let n = actor.Calast.ac_name in
  let ns = String.uppercase n in
  let (members, parameters, members_init) = parameters_of_actor actor
  in
    fprintf out
      "include(%s)dnl
#ifndef SCHED_%s_H
#define SCHED_%s_H

SC_MODULE(sched_%s) {
  // FIFOs
%s

  // Variable parameters
%s

  SC_HAS_PROCESS(sched_%s);

  sched_%s(sc_module_name N%s) : sc_module(N)%s {
    SC_THREAD(process);
  }

  void process();

};

#endif
"
      "tlm.m4" ns ns n fifos members n n parameters members_init
  
(** [associate_params action params] associates the action inputs and outputs
 with the parameters [params]. *)
let associate_params action (params : Calast.decl list) =
  let (in_params, remaining) =
    List.fold_left
      (fun (params, remaining) (port, _, _) ->
         let (decl, remaining) =
           match remaining with
           | [] -> failwith "No enough parameters"
           | h :: t -> (h, t)
         in (((decl, port) :: params), remaining))
      ([], params) action.Calast.a_inputs in
  let (out_params, remaining) =
    List.fold_left
      (fun (params, remaining) (out_decl, _) ->
         let (decl, remaining) =
           match remaining with
           | [] -> failwith "No enough parameters"
           | h :: t -> (h, t)
         in (((decl, (out_decl.Calast.d_name)) :: params), remaining))
      ([], remaining) action.Calast.a_outputs in
  let () =
    match remaining with
    | [] -> ()
    | [ decl ] when decl.Calast.d_name = "()" -> ()
    | _ -> failwith "Some output parameters remain"
  in ((List.rev in_params), (List.rev out_params))
  
(** [get_actions actor] returns a list (action, decl) where action is an
 action of [actor], and decl the declaration associated. *)
let get_actions actor =
  let actions =
    List.map
      (fun action ->
         let aname = action.Calast.a_name in
         let decl =
           try
             List.find (fun { Calast.d_name = dname } -> dname = aname)
               actor.Calast.ac_locals
           with | Not_found -> (print_endline aname; raise Not_found) in
         let params =
           match decl.Calast.d_value with
           | Some (Calast.Function (params, _, _)) -> params
           | _ ->
               failwith ("Action " ^ (decl.Calast.d_name ^ " has no value!")) in
         let params = associate_params action params
         in (action, decl, params))
      actor.Calast.ac_actions
  in actions
  
(** [print_body out actor] writes the SystemC body of [actor] in the output
 channel [out]. Basically we just print the outline of the actor, and fill
 the holes with information generated by [generate_process_body]. *)
let print_body out actor globals =
  let name = actor.Calast.ac_name in
  let actions = get_actions actor in
  let extern = Schedcil.generate_extern globals in
  let body = Schedcil.generate_process_body actor actions globals
  in
    fprintf out
      "include(%s)dnl
__INCLUDE__
#include \"sched_%s.h\"

#ifdef __cplusplus
extern \"C\" {
#endif
%s
#ifdef __cplusplus
}
#endif

__PROCESS_BEGIN__(sched_%s)
%s
__PROCESS_END__
"
      "tlm.m4" name extern name body
  
(** [translate od includes actor globals] creates a dynamic schedule of [actor]. It
 produces a header file (.h) and a body one (.cpp) in the output directory
 [od]. We first print the header, and then the body using the functions
 print_header and print_body. *)
let translate od includes actor globals =
  let filename =
    Filename.concat od ("sched_" ^ (actor.Calast.ac_name ^ ".h")) in
  let () = m4 includes filename (fun outch -> print_header outch actor) in
  let filename =
    Filename.concat od ("sched_" ^ (actor.Calast.ac_name ^ ".cpp")) in
  let () = m4 includes filename (fun outch -> print_body outch actor globals) in ()
  
