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
  Cal2c_util.Exception.Make(struct let module_name = "Schedastgen"
                                      end)
  
open E
  
let re_notag = Str.regexp "untagged_action_"
  
(** *)
class waitEliminationVisitor =
  object (self)
    inherit Astvisit.nopVisitor as super
      
    method visitExpr =
      fun expr ->
        match expr with
        | Calast.BinaryOp ((Calast.Var var_name), Calast.GreaterThanOrEqual,
            (Calast.Literal (Calast.Integer _))) ->
            if
              ((String.length var_name) > 14) &&
                ((String.sub var_name 0 14) = "__AVAILABLE__(")
            then Calast.Literal (Calast.Boolean true)
            else expr
        | Calast.BinaryOp (e1, Calast.And, e2) ->
            let e1 = self#visitExpr e1 in
            let e2 = self#visitExpr e2
            in
              (match e1 with
               | Calast.Literal (Calast.Boolean true) -> e2
               | _ -> Calast.BinaryOp (e1, Calast.And, e2))
        | Calast.If (test, ethen, eelse) ->
            let no_available_test = self#visitExpr test in
            let ethen = self#visitExpr ethen in
            let eelse = self#visitExpr eelse
            in
              (match no_available_test with
               | Calast.Literal (Calast.Boolean true) ->
                   (match eelse with
                    | Calast.Application ((Calast.Var "__WAIT__"), _) ->
                        ethen
                    | _ ->
                        (* Actions can be executed in the else part, so we keep
											the test as it is. *)
                        Calast.If (test, ethen, eelse))
               | _ ->
                   (match eelse with
                    | Calast.Application ((Calast.Var "__WAIT__"), _) ->
                        Calast.If (no_available_test, ethen, Calast.Unit)
                    | _ -> Calast.If (no_available_test, ethen, eelse)))
        | _ -> super#visitExpr expr
      
  end
  
let wait_elimination ast =
  let visitor = new waitEliminationVisitor in visitor#visitExpr ast
  
(** [ast_of_schedule fsm actions] returns a CAL typed ast with a schedule of
 [actions] according to priorities and/or FSM. *)
let ast_of_schedule fsm actions =
  let () = (Schedastgenbase.cnt_in := 1; Schedastgenbase.cnt_out := 1) in
  let (locals, ast, actions) =
    match fsm with
    | None -> ([], (Schedastgenbase.wait_of_actions actions), actions)
    | Some (state, transitions) ->
        let (locals, ast) =
          Schedastgenfsm.ast_of_fsm state transitions actions in
        let actions =
          List.filter
            (fun ({ Calast.a_name = name }, _, _) ->
               try (ignore (Str.search_forward re_notag name 0); true)
               with | Not_found -> false)
            actions
        in (locals, ast, actions) in
  let (locals, ast) =
    List.fold_right
      (fun (action, _, (in_params, out_params)) (locals, ast) ->
         let test = Schedastgenbase.generate_action_test action in_params in
         let (locals, call) =
           Schedastgenbase.generate_action_call action in_params out_params
             locals
         in (locals, (Calast.If (test, call, ast))))
      actions (locals, ast) in
  let ast = wait_elimination ast
  in
    ((List.rev locals),
     (Calast.While (Calast.Literal (Calast.Boolean true), ast)))
  
