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
  
open Cal2cil_common
  
open Cal2cil_expr
  
open Cal2c_util
  
module E =
  Cal2c_util.Exception.Make(struct let module_name = "Transl_action"
                                      end)
  
open E
  
let builtin_functions =
  [ Cal2c_util.SM.find "currentSystemTime" Typeinf.initial_environment;
    Cal2c_util.SM.find "openFile" Typeinf.initial_environment;
    Cal2c_util.SM.find "picture_displayImage" Typeinf.initial_environment;
    Cal2c_util.SM.find "picture_setPixel" Typeinf.initial_environment;
    Cal2c_util.SM.find "readByte" Typeinf.initial_environment;
    Cal2c_util.SM.find "JFrame" Typeinf.initial_environment;
    Cal2c_util.SM.find "Picture" Typeinf.initial_environment ]
  
(** [declare_and_define file decls] declares functions (writes only the
 protoypes), and defines them afterwards.
 Just a filter + iter. Could be improved but I don't care :) *)
let declare_and_define file actor_struct decls =
  let fun_defs =
    List.sort
      (fun decl1 decl2 -> compare decl1.Calast.d_name decl2.Calast.d_name)
      (List.filter
         (fun decl ->
            match snd decl.Calast.d_type with
            | Calast.Type.TA _ -> true
            | _ -> false)
         decls)
  in
    (List.iter
       (fun decl ->
          match decl.Calast.d_value with
          | Some (Calast.Function (params, _, _)) ->
              Cal2cil_function.declare file None decl.Calast.d_name
                (snd decl.Calast.d_type) params
          | _ -> ())
       builtin_functions;
     List.iter
       (fun decl ->
          match decl.Calast.d_value with
          | Some (Calast.Function (params, _, _)) ->
              Cal2cil_function.declare file (Some actor_struct)
                decl.Calast.d_name (snd decl.Calast.d_type) params
          | _ -> ())
       fun_defs;
     List.iter
       (fun decl ->
          match decl.Calast.d_value with
          | Some (Calast.Function (params, locals, expr)) ->
              Cal2cil_function.define file (Some actor_struct)
                decl.Calast.d_name (snd decl.Calast.d_type) params locals
                expr
          | _ -> ())
       fun_defs)
  
(** [struct_of_locals file actor_name locals] creates a structure named
 [actor_name ^ "_variables"], containing all [locals] fields, and adds it at
 the beginning of [file]. *)
let struct_of_locals file actor_name locals =
  let locals =
    List.filter
      (fun decl ->
         match snd decl.Calast.d_type with
         | Calast.Type.TA _ -> false
         | _ -> true)
      locals in
  let structure =
    Cil.mkCompInfo true (actor_name ^ "_variables")
      (fun _ ->
         List.map
           (fun decl ->
              ((decl.Calast.d_name),
               (Cal2cil_common.cil_of_type (snd decl.Calast.d_type)), None,
               [], Cil.locUnknown))
           locals)
      []
  in
    ((match locals with
      | [] ->
          (* MSVC does not like it when a C structure is empty, so we just
			    declare, and it seems to please it. *)
          file.Cil.globals <-
            (Cil.GCompTagDecl (structure, Cil.locUnknown)) ::
              file.Cil.globals
      | _ ->
          file.Cil.globals <-
            (Cil.GCompTag (structure, Cil.locUnknown)) :: file.Cil.globals);
     structure)
  
(** This visitors starts by registering the locals of the actor it visits, as
 well as the actor name. Then, each action is renamed ('.' replaced by
 "_dot_", and action name prefixed by the actor name). Each function
 declaration is renamed in the same manner. Actions are renamed in the FSM
 transitions too. And finally, any reference to a local is also renamed (in an
 expression). *)
let prefixNamesVisitor =
  object inherit Astvisit.nopVisitor as super
           
    val mutable m_actorName = ""
      
    val m_locals = SH.create 0
      
    method visitAction =
      fun action ->
        let action_name =
          Str.global_replace re_dot "_dot_" action.Calast.a_name in
        let action_name = m_actorName ^ ("_" ^ action_name) in
        let action = { (action) with Calast.a_name = action_name; }
        in super#visitAction action
      
    method visitActor =
      fun actor ->
        (m_actorName <- actor.Calast.ac_name;
         List.iter
           (fun decl ->
              match decl.Calast.d_value with
              | Some (Calast.Function (_, _, _)) ->
                  SH.add m_locals decl.Calast.d_name ()
              | _ -> ())
           actor.Calast.ac_locals;
         super#visitActor actor)
      
    method visitDecl =
      fun decl ->
        let decl =
          match decl.Calast.d_value with
          | Some (Calast.Function (_, _, _)) ->
              {
                (decl)
                with
                Calast.d_name = m_actorName ^ ("_function_" ^ decl.Calast.d_name);
              }
          | _ -> decl
        in super#visitDecl decl
      
    method visitFsm =
      fun fsm ->
        match fsm with
        | None -> None
        | Some (state, transitions) ->
            let transitions =
              List.map
                (fun (sf, action, st) ->
                   let action = Str.global_replace re_dot "_dot_" action in
                   let action = m_actorName ^ ("_" ^ action)
                   in (sf, action, st))
                transitions
            in Some (state, transitions)
      
    method visitExpr =
      fun expr ->
        match expr with
        | Calast.Var name ->
            let name =
              if SH.mem m_locals name
              then m_actorName ^ ("_function_" ^ name)
              else name
            in Calast.Var name
        | expr -> super#visitExpr expr
      
  end
  
(** Removes the Contents and Reference operators that were added by
 Visitb4typ.addReferencesVisitor to correctly type the AST. Also alters the
 types of locals (in functions) to remove the reference type qualifier, because
 unlike ML, C locals are mutable. *)
let removeReferencesVisitor =
  object (self)
    inherit Astvisit.nopVisitor as super
      
    method private transformDecl =
      fun decl ->
        let (tv, t) = decl.Calast.d_type in
        let t = match t with | Calast.Type.TR t -> t | t -> t
        in decl.Calast.d_type <- (tv, t)
      
    method visitExpr =
      fun e ->
        match e with
        | Calast.Function (_, locals, _) ->
            (List.iter self#transformDecl locals; super#visitExpr e)
        | Calast.UnaryOp (Calast.Contents, e) |
            Calast.UnaryOp (Calast.Reference, e) -> self#visitExpr e
        | e -> super#visitExpr e
      
  end

let printlnArgsVisitor =
  object (self)
    inherit Astvisit.nopVisitor
      
    method visitExpr =
      fun expr ->
        match expr with
        | Calast.BinaryOp (e1, Calast.Plus, e2) ->
            let e1 = self#visitExpr e1 in
            let e2 = self#visitExpr e2
            in Calast.BinaryOp (e1, Calast.Plus, e2)
        | expr -> Calast.Application (Calast.Var "toString", [ expr ])
      
  end
  
let printlnVisitor =
  object
    inherit Astvisit.nopVisitor as super
		
    method visitExpr =
      fun expr ->
        match expr with
        | Calast.Application ((Calast.Var "println"), args) ->
            let args = List.map printlnArgsVisitor#visitExpr args
            in Calast.Application (Calast.Var "println", args)
        | _ -> super#visitExpr expr
      
  end

let removeToStringVisitor =
	object
    inherit Astvisit.nopVisitor as super
		
    method visitExpr =
      fun expr ->
        match expr with
        | Calast.Application ((Calast.Var "toString"), [ expr ]) -> expr
        | _ -> super#visitExpr expr
      
  end
  
(* Entry point of this module. *)
let translate debug od actor =
  let filename = Filename.concat od (actor.Calast.ac_name ^ ".c") in
  let () = print_endline ("*** Printing " ^ (filename ^ " ***")) in
  let tr = [] in
  let tr = (new Order_actions.orderActionsVisitor) :: tr in
  let tr = prefixNamesVisitor :: tr in
  let tr = new Visitb4typ.solveInstantiationParameters :: tr in
  let tr = new Visitb4typ.convertActionsToFunctionsVisitor :: tr in
  let tr = new Visitb4typ.addUnitParameterVisitor :: tr in
  let tr = new Visitb4typ.addReferencesVisitor :: tr in
  let tr = new Visitb4typ.orderDeclarationsVisitor :: tr in
	let tr = printlnVisitor :: tr in
  let tr = List.rev tr in
  let actor = List.fold_left (fun actor v -> v#visitActor actor) actor tr in
  let () = Typeactor.type_inference debug actor in
  let tr = [] in
	let tr = removeToStringVisitor :: tr in
  let tr = removeReferencesVisitor :: tr in
  let tr = new Visitconstfold.globalConstFoldVisitor :: tr in
  let tr = new Visitinlinelist.inlineListVisitor :: tr in
  let tr = new Visitlistsize.solveListLengthVisitor :: tr in
  let tr = List.rev tr in
  let actor = List.fold_left (fun actor v -> v#visitActor actor) actor tr in
  (* cleanup of file declarations. *)
  let file = Cal2cil_common.fresh_file () in
  let actor_vars_struct =
    struct_of_locals file actor.Calast.ac_name actor.Calast.ac_locals in
  let () =
    (declare_and_define file actor_vars_struct actor.Calast.ac_locals;
     Cil.visitCilFileSameGlobals (Cil.constFoldVisitor false) file) in
  let out = open_out filename in
  let printer = Cil.defaultCilPrinter in
  let () = (Cil.dumpFile printer out filename file; close_out out)
  in (actor, (file.Cil.globals))
  
