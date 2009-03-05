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
  Cal2c_util.Exception.Make(struct let module_name = "Schedastgenbase"
                                      end)
  
open E
  
(** [generate_used_test action decl port] generates a test on the number
 of tokens required by [action] on the FIFO [port] to run. [decl] is the
 parameter of [action] in which the content read from [port] is placed. *)
let generate_used_test decl port =
  let n =
    match snd decl.Calast.d_type with
    | Calast.Type.TL (length, _) ->
        (match length with
         | None ->
             failwith
               (sprintf "Action %s, port %s: type %s has no size"
                  decl.Calast.d_name port
                  (Typeinf.string_of_type_scheme decl.Calast.d_type))
         | Some n -> n)
    | Calast.Type.TV _ | Calast.Type.TP _ -> 1
    | _ ->
        failwith
          (sprintf "Action %s, port %s: unexpected type %s"
             decl.Calast.d_name port
             (Typeinf.string_of_type_scheme decl.Calast.d_type)) in
  let var_name = sprintf "__AVAILABLE__(%s)" port in
  let test =
    Calast.BinaryOp (Calast.Var var_name, Calast.GreaterThanOrEqual,
      Calast.Literal (Calast.Integer n))
  in test
  
(** [wait_of_actions actions] generates a call to __WAIT__ on a list of ports
 that occur in an input pattern of [actions]. If no actions have an input
 pattern, (this may happen in FSM schedules for e.g.), we just don't wait and
 return Unit. *)
let wait_of_actions actions =
  let ip_set =
    List.fold_left
      (fun ip_set (_, _, (in_params, _)) ->
         List.fold_left (fun ip_set (_, port) -> SS.add port ip_set) ip_set
           in_params)
      SS.empty actions
  in
    match SS.elements ip_set with
    | [] -> Calast.Unit
    | h :: t ->
        let args =
          List.fold_left
            (fun args str -> sprintf "__OR__(%s,__EVENT__(%s))" args str)
            (sprintf "__EVENT__(%s)" h) t
        in Calast.Application (Calast.Var "__WAIT__", [ Calast.Var args ])
  
(** This class extends visitExpr to replace every variable read from one of
 [action] ports by a call to __PEEK__ on that port. [params] are the parameters
 of [action]. *)
class peekVisitor action params =
  object inherit Astvisit.nopVisitor as super
           
    method visitExpr =
      fun expr ->
        match expr with
        | Calast.Var var ->
            let var =
              (try
                 let (_, port) =
                   List.find (fun (decl, _) -> decl.Calast.d_name = var)
                     params
                 in sprintf "__PEEK__(%s)" port
               with | Not_found -> var)
            in Calast.Var var
        | _ -> super#visitExpr expr
      
  end
  
(** [generate_guard_test action decl conditions] adds guard tests to the token
 [conditions]. This is not as straightforward as it seems since we need
 to check those variables which are read from [action] FIFOs. *)
let generate_guard_test action params conditions =
  let visitor = new peekVisitor action params
  in
    List.fold_left
      (fun conditions guard ->
         let guard = visitor#visitExpr guard in guard :: conditions)
      conditions action.Calast.a_guards
  
(** [generate_action_test action params] generates the action test for the given
 [action]. This test encompasses the number of tokens used as well as
 guards. *)
let generate_action_test action params =
  let conditions =
    List.fold_left
      (fun conditions (decl, port) ->
         let test = generate_used_test decl port in test :: conditions)
      [] params in
  let conditions = generate_guard_test action params conditions
  in
    match conditions with
    | [] -> Calast.Literal (Calast.Boolean true)
    | condition :: conditions ->
        List.fold_left
          (fun condition test ->
             Calast.BinaryOp (test, Calast.And, condition))
          condition conditions
  
let show_io = true
  
let cnt_in = ref 1
  
(** Adds an input parameter. *)
let add_param action locals stmts params decl port =
  let local =
    {
      Calast.d_name = "_token_" ^ (string_of_int !cnt_in);
      d_type = (IS.empty, (Calast.Type.TP Calast.Type.Int));
      d_value = None;
    } in
  let () = incr cnt_in in
  let locals = local :: locals in
  let get = Calast.Application (Calast.Var "__GET__", [ Calast.Var port ]) in
  let get =
    if show_io
    then
      Calast.Statements
        (Calast.Statements
           (Calast.Statements
              (Calast.Application (Calast.Var "println",
                 [ Calast.Literal
                     (Calast.String
                        ("action " ^
                           (action.Calast.a_name ^ (": get from " ^ port)))) ]),
              Calast.Assign (Calast.Var local.Calast.d_name, get)),
           Calast.Application (Calast.Var "println",
             [ Calast.BinaryOp
                 (Calast.BinaryOp
                    (Calast.Literal
                       (Calast.String
                          ("action " ^ (action.Calast.a_name ^ ": got "))),
                    Calast.Plus, Calast.Var local.Calast.d_name),
                 Calast.Plus,
                 Calast.Literal (Calast.String (" from " ^ port))) ])),
        Calast.Var local.Calast.d_name)
    else get
  in
    match snd decl.Calast.d_type with
    | Calast.Type.TL ((Some n), _) ->
        let e =
          Calast.List
            (Calast.Generator ([ get ],
               [ {
                   Calast.d_name = "i";
                   d_type = (IS.empty, (Calast.Type.TP Calast.Type.Int));
                   d_value =
                     Some
                       (Calast.Application (Calast.Var "Integers",
                          [ Calast.Literal (Calast.Integer 0);
                            Calast.Literal (Calast.Integer (n - 1)) ]));
                 } ]))
        in (locals, stmts, (e :: params))
    | _ -> let params = get :: params in (locals, stmts, params)
  
let cnt_out = ref 1
  
(** Adds an output parameter. *)
let add_out_param action locals stmts params decl =
  let printf value =
    Calast.Application (Calast.Var "println",
      [ Calast.BinaryOp
          (Calast.Literal
             (Calast.String
                ("action " ^ (action.Calast.a_name ^ ": put value "))),
          Calast.Plus,
          Calast.BinaryOp (value, Calast.Plus,
            Calast.Literal (Calast.String (" to " ^ decl.Calast.d_name)))) ]) in
  let printf_after =
    Calast.Application (Calast.Var "println",
      [ Calast.Literal
          (Calast.String
             ("action " ^
                (action.Calast.a_name ^
                   (": put to " ^ (decl.Calast.d_name ^ " OK"))))) ])
  in
    match decl.Calast.d_type with
    | (is, Calast.Type.TR t) ->
        let local =
          {
            Calast.d_name = "_out_" ^ (string_of_int !cnt_out);
            d_type = (is, t);
            d_value = None;
          } in
        let () = incr cnt_out in
        let locals = local :: locals in
        let params =
          (Calast.UnaryOp (Calast.Reference, Calast.Var local.Calast.d_name)) ::
            params in
        let stmts =
          (match t with
           | Calast.Type.TL ((Some n), _) ->
               let put =
                 Calast.Application (Calast.Var "__PUT__",
                   [ Calast.Var decl.Calast.d_name;
                     Calast.Indexer (Calast.Var local.Calast.d_name,
                       Calast.Var "i") ]) in
               let put =
                 if show_io
                 then
                   Calast.Statements
                     (Calast.Statements
                        (printf
                           (Calast.Indexer (Calast.Var local.Calast.d_name,
                              Calast.Var "i")),
                        put),
                     printf_after)
                 else put
               in
                 Calast.Statements (stmts,
                   Calast.List
                     (Calast.Generator ([ put ],
                        [ {
                            Calast.d_name = "i";
                            d_type =
                              (IS.empty, (Calast.Type.TP Calast.Type.Int));
                            d_value =
                              Some
                                (Calast.Application (Calast.Var "Integers",
                                   [ Calast.Literal (Calast.Integer 0);
                                     Calast.Literal (Calast.Integer (n - 1)) ]));
                          } ])))
           | _ ->
               let put =
                 Calast.Application (Calast.Var "__PUT__",
                   [ Calast.Var decl.Calast.d_name;
                     Calast.Var local.Calast.d_name ]) in
               let put =
                 if show_io
                 then
                   Calast.Statements
                     (Calast.Statements
                        (printf (Calast.Var local.Calast.d_name), put),
                     printf_after)
                 else put
               in Calast.Statements (stmts, put))
        in (locals, stmts, params)
    | _ -> failwith (decl.Calast.d_name ^ " should be a reference!")
  
(** [generate_action_call action params] generates the call to an action,
 using parameters [params]. *)
let generate_action_call action inp outp locals =
  let (locals, in_stmts, params) =
    List.fold_left
      (fun (locals, stmts, params) (decl, port) ->
         add_param action locals stmts params decl port)
      (locals, Calast.Unit, []) inp in
  let (locals, out_stmts, params) =
    List.fold_left
      (fun (locals, stmts, params) (decl, _) ->
         add_out_param action locals stmts params decl)
      (locals, Calast.Unit, params) outp in
  let params = List.rev params in
  let e =
    Calast.Statements
      (Calast.Statements
         (Calast.Statements (in_stmts,
            Calast.Application (Calast.Var action.Calast.a_name, params)),
         out_stmts),
      Calast.Unit)
  in (locals, e)
  
