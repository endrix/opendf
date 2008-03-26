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
  
open Printf
  
module E = Cal2c_util.Exception.Make(struct let module_name = "Astvisit"
                                               end)
  
(*****************************************************************************)
(* Expressions *)
class type calVisitor =
  object
    method visitAction : Calast.action -> Calast.action
      
    method visitActor : Calast.actor -> Calast.actor
      
    method visitAssign : Calast.expr -> Calast.expr -> Calast.expr
      
    method visitChild : Calast.child -> Calast.child
      
    method visitCollection : Calast.collection -> Calast.collection
      
    method visitDecl : Calast.decl -> Calast.decl
      
    method visitEntity : Calast.entity -> Calast.entity
      
    method visitEntityExpr : Calast.entity_expr -> Calast.entity_expr
      
    method visitExpr : Calast.expr -> Calast.expr
      
    method visitFsm :
      (string * ((string * string * string) list)) option ->
        (string * ((string * string * string) list)) option
      
    method visitInput :
      (string * Calast.decl * Calast.expr) ->
        (string * Calast.decl * Calast.expr)
      
    method visitNetwork : Calast.network -> Calast.network
      
    method visitOutput :
      (Calast.decl * Calast.expr) -> (Calast.decl * Calast.expr)
      
    method visitVar : Calast.var -> Calast.expr
      
  end
  
class nopVisitor =
  object (self)

    method visitAction =
      fun action ->
        {
          Calast.a_decls = List.map self#visitDecl action.Calast.a_decls;
          a_delay = self#visitExpr action.Calast.a_delay;
          a_inputs = List.map self#visitInput action.Calast.a_inputs;
          a_guards = List.map self#visitExpr action.Calast.a_guards;
          a_name = action.Calast.a_name;
          a_outputs = List.map self#visitOutput action.Calast.a_outputs;
          a_stmts = self#visitExpr action.Calast.a_stmts;
        }
      
    method visitActor =
      fun actor ->
        {
          Calast.ac_actions =
            List.map self#visitAction actor.Calast.ac_actions;
          ac_fsm = self#visitFsm actor.Calast.ac_fsm;
          ac_inputs = List.map self#visitDecl actor.Calast.ac_inputs;
          ac_locals = List.map self#visitDecl actor.Calast.ac_locals;
          ac_name = actor.Calast.ac_name;
          ac_outputs = List.map self#visitDecl actor.Calast.ac_outputs;
          ac_parameters = List.map self#visitDecl actor.Calast.ac_parameters;
          ac_priorities = actor.Calast.ac_priorities;
        }
      
    method visitAssign =
      fun e1 e2 ->
        let e1 = self#visitExpr e1 in
        let e2 = self#visitExpr e2 in Calast.Assign (e1, e2)
      
    method visitChild =
      fun child ->
        match child with
        | Calast.Actor actor -> Calast.Actor (self#visitActor actor)
        | Calast.Network network ->
            Calast.Network (self#visitNetwork network)
      
    method visitCollection =
      function
      | Calast.Generator (el, dl) ->
          let el = List.map self#visitExpr el in
          let dl = List.map self#visitDecl dl in Calast.Generator (el, dl)
      | Calast.Comprehension el ->
          Calast.Comprehension (List.map self#visitExpr el)
      
    method visitDecl =
      fun decl ->
        let v =
          match decl.Calast.d_value with
          | None -> None
          | Some e -> Some (self#visitExpr e)
        in
          {
            Calast.d_name = decl.Calast.d_name;
            d_type = decl.Calast.d_type;
            d_value = v;
          }
      
    method visitEntity =
      fun entity ->
        {
          Calast.e_name = entity.Calast.e_name;
          e_expr = self#visitEntityExpr entity.Calast.e_expr;
          e_filename = entity.Calast.e_filename;
          e_child = self#visitChild entity.Calast.e_child;
        }
      
    method visitEntityExpr =
      fun ee ->
        match ee with
        | Calast.DirectInst (name, params) ->
            Calast.DirectInst (name,
              List.map (fun (pn, pv) -> (pn, (self#visitExpr pv))) params)
        | Calast.CondInst (etest, ethen, eelse) ->
            let etest = self#visitExpr etest in
            let ethen = self#visitEntityExpr ethen in
            let eelse = self#visitEntityExpr eelse
            in Calast.CondInst (etest, ethen, eelse)
      
    method visitExpr =
      function
      | Calast.Application (e, el) ->
          let e = self#visitExpr e in
          let el = List.map self#visitExpr el in Calast.Application (e, el)
      | Calast.Assign (e1, e2) -> self#visitAssign e1 e2
      | Calast.BinaryOp (e1, bop, e2) ->
          let e1 = self#visitExpr e1 in
          let e2 = self#visitExpr e2 in Calast.BinaryOp (e1, bop, e2)
      | Calast.Function (params, locals, e) ->
          let params = List.map self#visitDecl params in
          let locals = List.map self#visitDecl locals in
          let e = self#visitExpr e in Calast.Function (params, locals, e)
      | Calast.If (e1, e2, e3) ->
          let e1 = self#visitExpr e1 in
          let e2 = self#visitExpr e2 in
          let e3 = self#visitExpr e3 in Calast.If (e1, e2, e3)
      | Calast.Indexer (e1, e2) ->
          let e1 = self#visitExpr e1 in
          let e2 = self#visitExpr e2 in Calast.Indexer (e1, e2)
      | Calast.List collection ->
          Calast.List (self#visitCollection collection)
      | Calast.Literal literal -> Calast.Literal literal
      | Calast.Statements (e1, e2) ->
          let e1 = self#visitExpr e1 in
          let e2 = self#visitExpr e2 in Calast.Statements (e1, e2)
      | Calast.Switch (e, el) ->
          let e = self#visitExpr e in
          let el =
            List.map
              (fun (e1, e2) -> ((self#visitExpr e1), (self#visitExpr e2))) el
          in Calast.Switch (e, el)
      | Calast.UnaryOp (uop, e) -> Calast.UnaryOp (uop, self#visitExpr e)
      | Calast.Unit -> Calast.Unit
      | Calast.Var var -> self#visitVar var
      | Calast.While (e1, e2) ->
          let e1 = self#visitExpr e1 in
          let e2 = self#visitExpr e2 in Calast.While (e1, e2)
      
    method visitFsm =
      fun fsm -> match fsm with | None -> None | Some fsm -> Some fsm
      
    method visitInput =
      fun (s, d, e) -> (s, (self#visitDecl d), (self#visitExpr e))
      
    method visitNetwork =
      fun network ->
        {
          Calast.n_entities =
            List.map self#visitEntity network.Calast.n_entities;
          n_inputs = List.map self#visitDecl network.Calast.n_inputs;
          n_locals = List.map self#visitDecl network.Calast.n_locals;
          n_name = network.Calast.n_name;
          n_outputs = List.map self#visitDecl network.Calast.n_outputs;
          n_parameters = List.map self#visitDecl network.Calast.n_parameters;
          n_structure = network.Calast.n_structure;
        }
      
    method visitOutput =
      fun (d, e) -> ((self#visitDecl d), (self#visitExpr e))
      
    method visitVar = fun var -> Calast.Var var
      
  end
  
