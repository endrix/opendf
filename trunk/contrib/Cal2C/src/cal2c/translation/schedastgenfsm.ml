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
  Cal2c_util.Exception.Make(struct let module_name = "Schedastgenfsm"
                                      end)
  
open E
  
let cnt_state = ref 1
  
let states = SH.create 0
  
(** [get_state_value state] returns the integer value associated with the
 given [state]. State values are stored in the [states] hash table, which
 is cleared at each call to [ast_of_fsm]. *)
let get_state_value state =
  try SH.find states state
  with
  | Not_found ->
      (SH.add states state !cnt_state;
       let cnt = !cnt_state in (incr cnt_state; cnt))
  
let fsm_state = "fsm_state"
  
let action_match_name a1 a2 =
  let compare_sub a1 a1_length a2 a2_length =
    let a1_sub = String.sub a1 0 a1_length in
    let a2_sub = String.sub a2 0 a1_length
    in
      if a1_sub = a2_sub
      then
        if a2_length > (a1_length + 5)
        then (String.sub a2 a1_length 5) = "_dot_"
        else false
      else false in
  let a1_length = String.length a1 in
  let a2_length = String.length a2
  in
    if a1_length = a2_length
    then a1 = a2
    else
      if a1_length < a2_length
      then compare_sub a1 a1_length a2 a2_length
      else compare_sub a2 a2_length a1 a1_length
  
(** [order_targets targets actions]: 1) filters, 2) assigns rank, 3)
replaces (action_name, target_state) by (action, target_state, rank)
4) invert sorts by rank. Why invert? Because of the tree construction,
see get_case_body *)
let order_targets targets actions =
  (* Filters actions that are part of [targets]. *)
  let actions =
    List.filter
      (fun ({ Calast.a_name = aname }, _, _) ->
         List.exists (fun (action, _) -> action_match_name action aname)
           targets)
      actions in
  (* Assigns a rank to each of them. *)
  let (actions, _) =
    List.fold_left
      (fun (actions, i) action -> (((action, i) :: actions), (i + 1)))
      ([], 0) actions in
  let actions = List.rev actions in
  (* Retrieve the action associated with the action name in the [targets]
	list. *)
  let targets =
    List.fold_left
      (fun targets (action_name, target_state) ->
         let actions =
           List.filter
             (fun (({ Calast.a_name = aname }, _, _), _) ->
                action_match_name action_name aname)
             actions
         in
           match actions with
           | [] ->
               (fprintf stderr
                  "There is not fucking \"%s\" action in here! IGNORING THIS MOTHERFUCKER!\n"
                  action_name;
                targets)
           | actions ->
               List.fold_left
                 (fun targets (action, rank) ->
                    (action, target_state, rank) :: targets)
                 targets actions)
      [] targets
  in List.sort (fun (_, _, r1) (_, _, r2) -> - (compare r1 r2)) targets
  
(** [get_case_body] *)
let get_case_body targets actions locals =
  let targets = order_targets targets actions in
  let wait =
    Schedastgenbase.wait_of_actions
      (List.map (fun (action, _, _) -> action) targets)
  in
    List.fold_left
      (fun (locals, ast) ((action, _, (in_params, out_params)), target, _) ->
         let test = Schedastgenbase.generate_action_test action in_params in
         let (locals, call) =
           Schedastgenbase.generate_action_call action in_params out_params
             locals in
         let call =
           Calast.Statements (call,
             Calast.Assign (Calast.Var fsm_state,
               Calast.Literal (Calast.Integer (get_state_value target))))
         in (locals, (Calast.If (test, call, ast))))
      (locals, wait) targets
  
(** [ast_of_schedule actions] returns a CAL typed ast containing all the
 tests that should be performed. *)
let ast_of_fsm initial_state transitions actions =
  let () = (cnt_state := 1; SH.clear states) in
  let locals =
    [ {
        Calast.d_name = fsm_state;
        d_type = (IS.empty, (Calast.Type.TP Calast.Type.Int));
        d_value =
          Some
            (Calast.Literal (Calast.Integer (get_state_value initial_state)));
      } ] in
  let transitions =
    List.fold_left
      (fun transitions (sf, action, st) ->
         let targets = try SM.find sf transitions with | Not_found -> [] in
         let targets = (action, st) :: targets
         in SM.add sf targets transitions)
      SM.empty transitions in
  let (locals, cases) =
    SM.fold
      (fun sf targets (locals, cases) ->
         let case = Calast.Literal (Calast.Integer (get_state_value sf)) in
         let (locals, body) = get_case_body targets actions locals
         in (locals, ((case, body) :: cases)))
      transitions (locals, []) in
  let cases =
    List.sort
      (fun (e1, _) (e2, _) ->
         match (e1, e2) with
         | (Calast.Literal (Calast.Integer i1),
            Calast.Literal (Calast.Integer i2)) -> compare i1 i2
         | _ -> 0)
      cases in
  let ast = Calast.Switch (Calast.Var fsm_state, cases) in (locals, ast)
  
