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
  Cal2c_util.Exception.Make(struct let module_name = "Visitconstfold"
                                      end)
  
open E
  
(****************************************************************************)
(** This class visits the Calast tree and folds constants. *)
class constFoldVisitor env =
  object (self)

    method private compute_uop =
      fun uop lit ->
        match (uop, lit) with
        | (Calast.Not, Calast.Boolean b) ->
            Calast.Literal (Calast.Boolean (not b))
        | (Calast.UMinus, Calast.Integer i) ->
            Calast.Literal (Calast.Integer (- i))
        | (Calast.UMinus, Calast.Real f) ->
            Calast.Literal (Calast.Real (-. f))
        | _ -> failwith "compute_uop"
      
    method private compute_bop =
      fun bop lit1 lit2 ->
        match bop with
        | Calast.And ->
            (match (lit1, lit2) with
             | (Calast.Boolean b1, Calast.Boolean b2) ->
                 Calast.Literal (Calast.Boolean (b1 && b2))
             | _ -> failwith "compute_bop")
        | Calast.Div ->
            (match (lit1, lit2) with
             | (Calast.Integer i1, Calast.Integer i2) ->
                 Calast.Literal (Calast.Integer (i1 / i2))
             | (Calast.Real f1, Calast.Real f2) ->
                 Calast.Literal (Calast.Real (f1 /. f2))
             | _ -> failwith "compute_bop")
        | Calast.Equal -> Calast.Literal (Calast.Boolean (lit1 = lit2))
        | Calast.GreaterThan ->
            (match (lit1, lit2) with
             | (Calast.Integer i1, Calast.Integer i2) ->
                 Calast.Literal (Calast.Boolean (i1 > i2))
             | (Calast.Real f1, Calast.Real f2) ->
                 Calast.Literal (Calast.Boolean (f1 > f2))
             | (Calast.String s1, Calast.String s2) ->
                 Calast.Literal (Calast.Boolean (s1 > s2))
             | _ -> failwith "compute_bop")
        | Calast.GreaterThanOrEqual ->
            (match (lit1, lit2) with
             | (Calast.Integer i1, Calast.Integer i2) ->
                 Calast.Literal (Calast.Boolean (i1 >= i2))
             | (Calast.Real f1, Calast.Real f2) ->
                 Calast.Literal (Calast.Boolean (f1 >= f2))
             | (Calast.String s1, Calast.String s2) ->
                 Calast.Literal (Calast.Boolean (s1 >= s2))
             | _ -> failwith "compute_bop")
        | Calast.LessThan ->
            (match (lit1, lit2) with
             | (Calast.Integer i1, Calast.Integer i2) ->
                 Calast.Literal (Calast.Boolean (i1 < i2))
             | (Calast.Real f1, Calast.Real f2) ->
                 Calast.Literal (Calast.Boolean (f1 < f2))
             | (Calast.String s1, Calast.String s2) ->
                 Calast.Literal (Calast.Boolean (s1 < s2))
             | _ -> failwith "compute_bop")
        | Calast.LessThanOrEqual ->
            (match (lit1, lit2) with
             | (Calast.Integer i1, Calast.Integer i2) ->
                 Calast.Literal (Calast.Boolean (i1 <= i2))
             | (Calast.Real f1, Calast.Real f2) ->
                 Calast.Literal (Calast.Boolean (f1 <= f2))
             | (Calast.String s1, Calast.String s2) ->
                 Calast.Literal (Calast.Boolean (s1 <= s2))
             | _ -> failwith "compute_bop")
        | Calast.Minus ->
            (match (lit1, lit2) with
             | (Calast.Integer i1, Calast.Integer i2) ->
                 Calast.Literal (Calast.Integer (i1 - i2))
             | (Calast.Real f1, Calast.Real f2) ->
                 Calast.Literal (Calast.Real (f1 -. f2))
             | _ -> failwith "compute_bop")
        | Calast.Mod ->
            (match (lit1, lit2) with
             | (Calast.Integer i1, Calast.Integer i2) ->
                 Calast.Literal (Calast.Integer (i1 mod i2))
             | (Calast.Real f1, Calast.Real f2) ->
                 Calast.Literal (Calast.Real (mod_float f1 f2))
             | _ -> failwith "compute_bop")
        | Calast.NotEqual -> Calast.Literal (Calast.Boolean (lit1 <> lit2))
        | Calast.Or ->
            (match (lit1, lit2) with
             | (Calast.Boolean b1, Calast.Boolean b2) ->
                 Calast.Literal (Calast.Boolean (b1 || b2))
             | _ -> failwith "compute_bop")
        | Calast.Plus ->
            (match (lit1, lit2) with
             | (Calast.Integer i1, Calast.Integer i2) ->
                 Calast.Literal (Calast.Integer (i1 + i2))
             | (Calast.Real f1, Calast.Real f2) ->
                 Calast.Literal (Calast.Real (f1 +. f2))
             | (Calast.String s1, Calast.String s2) ->
                 Calast.Literal (Calast.String (s1 ^ s2))
             | _ -> failwith "compute_bop")
        | Calast.Times ->
            (match (lit1, lit2) with
             | (Calast.Integer i1, Calast.Integer i2) ->
                 Calast.Literal (Calast.Integer (i1 * i2))
             | (Calast.Real f1, Calast.Real f2) ->
                 Calast.Literal (Calast.Real (f1 *. f2))
             | _ -> failwith "compute_bop")
      
    method visitExpr =
      fun expr ->
        match expr with
        | Calast.Application (e, args) -> self#const_fold_application e args
        | Calast.BinaryOp (e1, bop, e2) ->
            let e1 = self#visitExpr e1 in
            let e2 = self#visitExpr e2
            in
              (match (e1, e2) with
               | (Calast.Literal lit1, Calast.Literal lit2) ->
                   self#compute_bop bop lit1 lit2
               | (e1, e2) -> Calast.BinaryOp (e1, bop, e2))
        | Calast.If (etest, ethen, eelse) ->
            (match self#visitExpr etest with
             | Calast.Literal (Calast.Boolean b) ->
                 if b then self#visitExpr ethen else self#visitExpr eelse
             | _ -> failwith "self#visitExpr: if condition should be boolean")
        | Calast.Literal literal ->
            (match literal with
             | Calast.Null -> failwith "self#visitExpr: Null literal"
             | lit -> Calast.Literal lit)
        | Calast.UnaryOp (uop, e) ->
            (match self#visitExpr e with
             | Calast.Literal lit -> self#compute_uop uop lit
             | _ -> Calast.UnaryOp (uop, e))
        | Calast.Var str ->
            (try
               let expr = self#visitExpr (SM.find str env)
               in
                 match expr with
                 | Calast.Application _ -> Calast.Var str
                 | expr -> expr
             with | Not_found -> Calast.Var str)
        | Calast.List list ->
            Calast.List
              (match list with
               | Calast.Generator (el, dl) ->
                   let el = List.map self#visitExpr el
                   in
                     (List.iter
                        (fun decl ->
                           match decl.Calast.d_value with
                           | None -> ()
                           | Some e ->
                               decl.Calast.d_value <- Some (self#visitExpr e))
                        dl;
                      Calast.Generator (el, dl))
               | Calast.Comprehension el ->
                   let el = List.map self#visitExpr el
                   in Calast.Comprehension el)
        | Calast.Function (params, locals, e) ->
            Calast.Function (params, locals, self#visitExpr e)
        | Calast.Assign (e1, e2) ->
            Calast.Assign (self#visitExpr e1, self#visitExpr e2)
        | Calast.Indexer (e1, e2) ->
            Calast.Indexer (self#visitExpr e1, self#visitExpr e2)
        | Calast.Statements (e1, e2) ->
            Calast.Statements (self#visitExpr e1, self#visitExpr e2)
        | Calast.Switch (e, el) ->
            let el =
              List.map
                (fun (e1, e2) -> ((self#visitExpr e1), (self#visitExpr e2)))
                el
            in Calast.Switch (self#visitExpr e, el)
        | Calast.Unit -> Calast.Unit
        | Calast.While (e1, e2) ->
            Calast.While (self#visitExpr e1, self#visitExpr e2)
      
    method private const_fold_application =
      fun e args ->
        match (e, args) with
        | (Calast.Var "rshift", [ e1; e2 ]) ->
            let e1 = self#visitExpr e1 in
            let e2 = self#visitExpr e2
            in
              (match (e1, e2) with
               | (Calast.Literal (Calast.Integer i),
                  Calast.Literal (Calast.Integer n)) ->
                   Calast.Literal (Calast.Integer (i lsr n))
               | _ ->
                   let e = self#visitExpr e in
                   let args = List.map self#visitExpr args
                   in Calast.Application (e, args))
        | _ ->
            let e = self#visitExpr e in
            let args = List.map self#visitExpr args
            in Calast.Application (e, args)
      
  end
  
(****************************************************************************)
(** This class visits the Calast tree and folds constants. *)
class globalConstFoldVisitor =
  object inherit Astvisit.nopVisitor as super
           
    (** Stores variable name -> Calast.expr associations. *)
    val mutable m_globals = SM.empty
      
    val mutable m_visit_expr = false
      
    (** Registers the globals of the actors that are not function and
    have a value. *)
    method visitActor =
      fun actor ->
        (List.iter
           (fun decl ->
              match ((decl.Calast.d_type), (decl.Calast.d_value)) with
              | (_, None) | ((_, Calast.Type.TA _), _) -> ()
              | (_, Some e) ->
                  m_globals <- SM.add decl.Calast.d_name e m_globals)
           actor.Calast.ac_locals;
         super#visitActor actor)
      
    (** Sets the m_visit_expr flag to true when we have a global. Updates
    the expression associated with decl.Calast.d_name in the m_globals
		string map. *)
    method visitDecl =
      fun decl ->
        (m_visit_expr <- SM.mem decl.Calast.d_name m_globals;
         if m_visit_expr
         then
           (match decl.Calast.d_value with
            | Some e ->
                let e = (new constFoldVisitor m_globals)#visitExpr e
                in
                  (m_globals <- SM.add decl.Calast.d_name e m_globals;
                   decl.Calast.d_value <- Some e)
            | None -> ())
         else ();
         decl)
      
  end
  
