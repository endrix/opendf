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
  
module E =
  Cal2c_util.Exception.Make(struct let module_name = "Visitb4typ"
                                      end)
  
open E
  
module V =
  struct
    type t = string
    
    let equal (a : string) (b : string) = a = b
      
    let hash (s : string) = Hashtbl.hash s
      
    let compare = String.compare
      
  end
  
module G = Graph.Imperative.Digraph.ConcreteBidirectional(V)
  
module T = Graph.Topological.Make(G)
  
(****************************************************************************)
class convertActionsToFunctionsVisitor =
  object (self)
    inherit Astvisit.nopVisitor as super
      
    method private functionOfAction =
      fun action ->
        let params =
          List.fold_left
            (fun params (_, decl, repeat) ->
               let (is, t) = ((IS.singleton 0), (Calast.Type.TV 0)) in
               let (is, t) =
                 match repeat with
                 | Calast.Literal (Calast.Integer 1) -> (is, t)
                 | expr ->
                     let v = new Visitconstfold.constFoldVisitor SM.empty in
                     let expr = v#visitExpr expr
                     in
                       (match expr with
                        | Calast.Literal (Calast.Integer length) ->
                            (is, (Calast.Type.TL (Some length, t)))
                        | _ -> failwith "fuck")
               in (decl.Calast.d_type <- (is, t); decl :: params))
            [] action.Calast.a_inputs in
        let (params, stmts) =
          List.fold_left
            (fun (params, stmts) (decl, repeat) ->
               let stmts =
                 match decl.Calast.d_value with
                 | None ->
                     failwith
                       ("convert_action: in action " ^
                          (action.Calast.a_name ^
                             (", output " ^
                                (decl.Calast.d_name ^ " has no value."))))
                 | Some e ->
                     Calast.Statements (stmts,
                       Calast.Assign (Calast.Var decl.Calast.d_name, e)) in
               let (is, t) = ((IS.singleton 0), (Calast.Type.TV 0)) in
               let (is, t) =
                 match repeat with
                 | Calast.Literal (Calast.Integer 1) ->
                     (is, (Calast.Type.TR t))
                 | expr ->
                     let v = new Visitconstfold.constFoldVisitor SM.empty in
                     let expr = v#visitExpr expr
                     in
                       (match expr with
                        | Calast.Literal (Calast.Integer length) ->
                            (is,
                             (Calast.Type.TR
                                (Calast.Type.TL (Some length, t))))
                        | _ -> failwith "fuck")
               in
                 (decl.Calast.d_type <- (is, t);
                  decl.Calast.d_value <- None;
                  ((decl :: params), stmts)))
            (params, (action.Calast.a_stmts)) action.Calast.a_outputs in
        let params = List.rev_map self#visitDecl params in
        let locals = List.map self#visitDecl action.Calast.a_decls in
        let stmts = self#visitExpr stmts
        in
          {
            Calast.d_name = action.Calast.a_name;
            d_type = ((IS.singleton 0), (Calast.Type.TV 0));
            d_value = Some (Calast.Function (params, locals, stmts));
          }
      
    method private transformAction =
      fun action ->
        let f = self#functionOfAction action in
        let action =
          {
            Calast.a_decls = List.map self#visitDecl action.Calast.a_decls;
            Calast.a_delay = self#visitExpr action.Calast.a_delay;
            Calast.a_inputs = List.map self#visitInput action.Calast.a_inputs;
            Calast.a_guards = List.map self#visitExpr action.Calast.a_guards;
            Calast.a_name = action.Calast.a_name;
            Calast.a_outputs =
              List.map self#visitOutput action.Calast.a_outputs;
            Calast.a_stmts = Calast.Unit;
          }
        in (action, f)
      
    method visitActor =
      fun actor ->
        let (actions, locals) =
          List.fold_left
            (fun (actions, locals) action ->
               let (action, f) = self#transformAction action
               in ((action :: actions), (f :: locals)))
            ([], (List.map self#visitDecl actor.Calast.ac_locals))
            actor.Calast.ac_actions in
        let (actions, locals) = ((List.rev actions), (List.rev locals))
        in
          {
            Calast.ac_actions = actions;
            ac_fsm = actor.Calast.ac_fsm;
            ac_inputs = List.map super#visitDecl actor.Calast.ac_inputs;
            ac_locals = locals;
            ac_name = actor.Calast.ac_name;
            ac_outputs = List.map super#visitDecl actor.Calast.ac_outputs;
            ac_parameters =
              List.map super#visitDecl actor.Calast.ac_parameters;
            ac_priorities = actor.Calast.ac_priorities;
          }
      
  end
  
(****************************************************************************)
(** This class adds a [Calast.Unit] parameter to function declarations and
 calls that have no parameters, to have their type correctly inferred
 as functions, and not as values. *)
class addUnitParameterVisitor =
  object inherit Astvisit.nopVisitor as super
           
    method visitExpr =
      fun e ->
        match e with
        | Calast.Application (call, []) ->
            Calast.Application (call, [ Calast.Unit ])
        | Calast.Function ([], locals, stmts) ->
            let decl =
              {
                Calast.d_name = "()";
                Calast.d_type = (IS.empty, (Calast.Type.TP Calast.Type.Unit));
                Calast.d_value = Some Calast.Unit;
              }
            in super#visitExpr (Calast.Function ([ decl ], locals, stmts))
        | _ -> super#visitExpr e
      
  end
  
(****************************************************************************)
class addReferencesVisitor =
  object (self)
    inherit Astvisit.nopVisitor as super
      
    val mutable m_vars = SH.create 0
      
    method private transformDecl =
      fun decl ->
        let t = decl.Calast.d_type in
        let e =
          match decl.Calast.d_value with
          | None -> None
          | Some e ->
              Some (Calast.UnaryOp (Calast.Reference, self#visitExpr e))
        in
          (decl.Calast.d_type <- ((fst t), (Calast.Type.TR (snd t)));
           decl.Calast.d_value <- e)
      
    method visitExpr =
      fun e ->
        match e with
        | Calast.Function (params, locals, e) ->
            (m_vars <- Calast.decl_hashmap_of_list (locals @ params);
             List.iter self#transformDecl locals;
             Calast.Function (params, locals, self#visitExpr e))
        | Calast.Var var ->
            (try
               let decl = SH.find m_vars var
               in
                 match snd decl.Calast.d_type with
                 | Calast.Type.TR _ -> Calast.UnaryOp (Calast.Contents, e)
                 | _ -> e
             with | Not_found -> e)
        | e -> super#visitExpr e
      
  end
  
(****************************************************************************)
(** This class visits the Calast tree and solves instantiation parameters. *)
class solveInstantiationParameters =
  object inherit Astvisit.nopVisitor as super
           
    val m_parameters = SH.create 0
      
    (** Registers the parameters of the actors that have a value. Others are
    variables parameters that are considered local variables. *)
    method visitActor =
      fun actor ->
        let locals =
          List.fold_left
            (fun locals decl ->
               match decl.Calast.d_value with
               | None -> decl :: locals
               | Some e ->
                   let () = SH.add m_parameters decl.Calast.d_name e
                   in locals)
            actor.Calast.ac_locals actor.Calast.ac_parameters in
        let actor = { (actor) with Calast.ac_locals = locals; }
        in super#visitActor actor
      
    (** Replaces instantiation parameters. *)
    method visitExpr =
      fun e ->
        match e with
        | Calast.Var var ->
            (try SH.find m_parameters var with | Not_found -> e)
        | _ -> super#visitExpr e
      
  end
  
(****************************************************************************)
class orderDeclarationsVisitor =
  object (self)
    inherit Astvisit.nopVisitor as super
      
    val m_graph = G.create ()
      
    val mutable m_name = ""
      
    (** [dependencyGraphOfDeclarations locals] iterates over the locals.
     For each local declaration, it sets m_name to the declaration name, and
		 visits the declaration value (if present). For functions, it also visits
		 the local variables. *)
    method private dependencyGraphOfDeclarations =
      fun locals ->
        List.iter
          (fun decl ->
             match decl.Calast.d_value with
             | Some e ->
                 (m_name <- decl.Calast.d_name;
                  let e =
                    (match e with
                     | Calast.Function (_, locals, e) ->
                         (List.iter
                            (fun decl -> ignore (super#visitDecl decl))
                            locals;
                          e)
                     | e -> e)
                  in ignore (self#visitExpr e))
             | _ -> ())
          locals
      
    (** [orderGraph locals parameters] orders [m_graph] by topological order. *)
    method private orderGraph =
      fun locals ->
        let locals =
          List.fold_left (fun sm decl -> SM.add decl.Calast.d_name decl sm)
            SM.empty locals in
        let (decls, locals) =
          T.fold
            (fun vertex (decls, locals) ->
               if SM.mem vertex locals
               then
                 (let decl = SM.find vertex locals in
                  let locals = SM.remove vertex locals
                  in ((decl :: decls), locals))
               else (decls, locals))
            m_graph ([], locals) in
        let decls = SM.fold (fun _ decl decls -> decl :: decls) locals decls
        in List.rev decls
      
    (** [visitActor actor] establishes a dependency graph among the local
     declarations of [actor], and then orders this graph. It returns a new
		 actor with its locals updated. *)
    method visitActor =
      fun actor ->
        let () = self#dependencyGraphOfDeclarations actor.Calast.ac_locals in
        let locals = self#orderGraph actor.Calast.ac_locals
        in { (actor) with Calast.ac_locals = locals; }
      
    (** [visitExpr e] adds an edge in the graph for each referenced variable
     to the variable name contained in [m_name]. Otherwise, it just calls
		 [super#visitExpr]. *)
    method visitExpr =
      fun e ->
        match e with
        | Calast.Var var -> (G.add_edge m_graph var m_name; e)
        | _ -> super#visitExpr e
      
  end
  
