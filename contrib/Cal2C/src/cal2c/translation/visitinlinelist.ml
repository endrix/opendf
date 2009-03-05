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
  Cal2c_util.Exception.Make(struct let module_name = "Visitinlinelist"
                                      end)
  
open E
  
let cnt_inline = ref 1
  
class replaceParametersVisitor new_locals new_params =
  object inherit Astvisit.nopVisitor as super
           
    method visitVar =
      fun var ->
        try
          (* replace local name by new local name *)
          let (_, { Calast.d_name = name }) =
            List.find (fun ({ Calast.d_name = name }, _) -> name = var)
              new_locals
          in Calast.Var name
        with
        | Not_found ->
            (try
               (* replace param by its expression *)
               let (_, expr) =
                 List.find (fun ({ Calast.d_name = name }, _) -> name = var)
                   new_params
               in expr
             with | Not_found -> Calast.Var var)
      
  end
  
(****************************************************************************)
(** This class visits the Calast tree and inlines calls to functions contained
 in [globals]. *)
class inlineWithContext ctx_locals globals =
  object (self)
    inherit Astvisit.nopVisitor as super
      
    (** *)
    method private inlineFunctionCall =
      fun params locals e args ->
        let new_locals =
          List.map
            (fun old_decl ->
               let cnt = !cnt_inline in
               let () = incr cnt_inline in
               let new_decl =
                 {
                   (old_decl)
                   with
                   Calast.d_name =
                     sprintf "_inline_%s_%i" old_decl.Calast.d_name cnt;
                 }
               in (old_decl, new_decl))
            locals in
        let new_params =
          List.map2 (fun param arg -> (param, arg)) params args in
        let visitor = new replaceParametersVisitor new_locals new_params in
        let () =
          List.iter
            (fun (_, decl) ->
               ((match decl.Calast.d_value with
                 | None -> ()
                 | Some expr ->
                     let expr = visitor#visitExpr expr
                     in decl.Calast.d_value <- Some expr);
                ctx_locals := decl :: !ctx_locals))
            new_locals
        in visitor#visitExpr e
      
    (** if [expr] is an Application whose name is present in globals,
    we call inlineFunction on it. *)
    method visitExpr =
      fun expr ->
        match expr with
        | Calast.Application ((Calast.Var name), args) ->
            (try
               let fn = SH.find globals name
               in
                 match fn with
                 | Calast.Function (params, locals, e) ->
                     super#visitExpr
                       (self#inlineFunctionCall params locals e args)
                 | _ -> failwith (name ^ " should be a function")
             with | Not_found -> super#visitExpr expr)
        | _ -> super#visitExpr expr
      
  end
  
(****************************************************************************)
(** This class visits the Calast tree and inlines the function calls that
 return lists. *)
class inlineListVisitor =
  object (self)
    inherit Astvisit.nopVisitor as super
      
    val m_globals = SH.create 0
      
    initializer cnt_inline := 1
      
    (** We fold locals, and face two cases: 1) the local is an actor variable,
    so we call the inlineWithContext visitor with all the locals.
		2) the local is a function, so we call the inlineWithContext visitor with
		the function locals. *)
    method private varInitInline =
      fun locals ->
        let locals =
          List.fold_left
            (fun locals decl ->
               match decl.Calast.d_value with
               | None -> decl :: locals
               | Some expr ->
                   (match expr with
                    | Calast.Function (params, f_locals, expr) ->
                        let inline_locals = ref [] in
                        let expr =
                          (new inlineWithContext inline_locals m_globals)#
                            visitExpr expr in
                        let expr =
                          Calast.Function (params,
                            (List.rev !inline_locals) @ f_locals, expr)
                        in (decl.Calast.d_value <- Some expr; decl :: locals)
                    | _ ->
                        let locals = ref locals in
                        let expr =
                          (new inlineWithContext locals m_globals)#visitExpr
                            expr
                        in
                          (decl.Calast.d_value <- Some expr; decl :: !locals)))
            [] locals
        in List.rev locals
      
    method private getFunctionReturnType =
      fun t ->
        match t with
        | Calast.Type.TA (_, _, t) -> self#getFunctionReturnType t
        | t -> t
      
    (** We fill the m_globals hash table with the function in ac_locals
    that return a list. Then we remove them from the locals, and call
		varInitInline, before returning the modified actor. *)
    method visitActor =
      fun actor ->
        (List.iter
           (fun decl ->
              match ((decl.Calast.d_type), (decl.Calast.d_value)) with
              | ((((_, Calast.Type.TA _) as t)), Some e) ->
                  let t = self#getFunctionReturnType (snd t)
                  in
                    (match t with
                     | Calast.Type.TL _ ->
                         SH.add m_globals decl.Calast.d_name e
                     | _ -> ())
              | _ -> ())
           actor.Calast.ac_locals;
         let locals =
           List.filter
             (fun { Calast.d_name = name } -> not (SH.mem m_globals name))
             actor.Calast.ac_locals in
         let locals = self#varInitInline locals in
         let actor = { (actor) with Calast.ac_locals = locals; }
         in super#visitActor actor)
      
  end
  
