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
  Cal2c_util.Exception.Make(struct let module_name = "Typeactor"
                                      end)
  
open E
  
(** [init_environment parents decls] initializes the environment by adding,
    for each declaration [decl] of [decls], a binding
    [decl.d_name] -> [(decl, None)]. Additionally, for Parameter declarations,
    a value is obtained by looking into [parents]. *)
let init_environment actor =
  let env = Typeinf.initial_environment in
  (* adds declarations to the environment. *)
  let env =
    List.fold_left (fun env decl -> SM.add decl.Calast.d_name decl env) env
      actor.Calast.ac_locals in
  let env =
    List.fold_left (fun env decl -> SM.add decl.Calast.d_name decl env) env
      actor.Calast.ac_inputs in
  let env =
    List.fold_left (fun env decl -> SM.add decl.Calast.d_name decl env) env
      actor.Calast.ac_outputs
  in
    List.fold_left (fun env decl -> SM.add decl.Calast.d_name decl env) env
      actor.Calast.ac_parameters
  
(** [type_inference debug] infers types of parameters, variables and functions
    present in the environment. *)
let type_inference debug actor =
  let env = init_environment actor
  in
    List.iter
      (fun decl ->
         match decl.Calast.d_value with
         | None -> ()
         | Some e ->
             let t =
               (try Typeinf.type_of_expr debug.d_type_inference env e
                with
                | Failure s ->
                    (fprintf stderr "Type error in %s\n%!"
                       decl.Calast.d_name;
                     failwith (sprintf "%s: %s" decl.Calast.d_name s))) in
             let () =
               (try
                  ignore
                    (Typeinf.mgu env false (snd decl.Calast.d_type) (snd t))
                with
                | Typedefs.Not_unifiable _ ->
                    (fprintf stderr "Type error in %s\n%!"
                       decl.Calast.d_name;
                     failwith
                       (sprintf "%s: %s and %s are not compatible"
                          decl.Calast.d_name
                          (Typedefs.string_of_type_scheme
                             decl.Calast.d_type)
                          (Typedefs.string_of_type_scheme t))))
             in decl.Calast.d_type <- t)
      (actor.Calast.ac_inputs @
         (actor.Calast.ac_outputs @ actor.Calast.ac_locals))
  
