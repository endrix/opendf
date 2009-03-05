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
  
module E = Cal2c_util.Exception.Make(struct let module_name = "Schedcil"
                                               end)
  
open E
  
let call_malloc actor_struct actor_vars =
  let lval = Cil.var actor_vars
  in
    Clist.single
      (Cil.mkStmtOneInstr
         (Cil.Call (Some lval,
            Cil.Lval (Cal2cil_common.lval_of_var_name "malloc"),
            [ Cil.sizeOf (Cil.TComp (actor_struct, [])) ], Cil.locUnknown)))
  
(** [init_local_no_value f stmts params decl] initializes a local variable
 that has no value and is a parameter. *)
let init_local_no_value f stmts params decl =
  if
    List.exists (fun { Calast.d_name = name } -> name = decl.Calast.d_name)
      params
  then
    (let member = "m_" ^ (String.lowercase decl.Calast.d_name) in
     let (_, stmts) =
       Cal2cil_expr.exp_of_expr f stmts
         (Calast.Assign (Calast.Var decl.Calast.d_name, Calast.Var member))
     in stmts)
  else stmts
  
(** [make_inits f actor_struct actor_vars params locals] creates all the
 statements needed to initialize the fields of [actor_struct]. *)
let make_inits f actor_struct actor_vars params locals =
  let stmts = call_malloc actor_struct actor_vars in
  let fun_defs =
    List.sort
      (fun decl1 decl2 -> compare decl1.Calast.d_name decl2.Calast.d_name)
      (List.filter
         (fun decl ->
            match snd decl.Calast.d_type with
            | Calast.Type.TA _ -> false
            | _ -> true)
         locals) in
  let stmts =
    List.fold_left
      (fun stmts decl ->
         match decl.Calast.d_value with
         | None -> init_local_no_value f stmts params decl
         | Some e ->
             let (_, stmts) =
               Cal2cil_expr.exp_of_expr f stmts
                 (Calast.Assign (Calast.Var decl.Calast.d_name, e))
             in stmts)
      stmts fun_defs
  in
    f.Cil.sbody.Cil.bstmts <-
      Clist.toList
        (Clist.append stmts (Clist.fromList f.Cil.sbody.Cil.bstmts))
  
(** [init_struct file actor_struct params locals] transforms the process
 function by creating a local structure (instead of having it as a
 parameter), and calls [make_inits]. *)
let init_struct file actor_struct params locals =
  let f =
    match file.Cil.globals with
    | [ Cil.GFun (f, _) ] -> f
    | _ -> failwith "No process function" in
  let formals =
    match f.Cil.sformals with | [] -> failwith "No arguments!" | _ :: t -> t in
  let () = Cil.setFormals f formals in
  let actor_vars =
    Cal2cil_common.get_local f "_actor_variables"
      (Cil.TPtr (Cil.TComp (actor_struct, []), []))
  in make_inits f actor_struct actor_vars params locals
  
(** [add_process_function file code] should be pretty obvious. *)
let add_process_function file actor_struct locals code =
  let t =
    Calast.Type.TA (Calast.Type.TP Calast.Type.Unit, Calast.TypeSchemeSet.
      empty, Calast.Type.TP Calast.Type.Unit)
  in
    Cal2cil_function.define file (Some actor_struct) "process" t [] locals
      code
  
let re_process = Str.regexp "void process(void)"
  
(** [add_builtins file ] adds the __GET__ and __PUT__ functions to [file]. *)
let add_builtins file =
  let t =
    Calast.Type.TA (Calast.Type.TP Calast.Type.Unit, Calast.TypeSchemeSet.
      empty, Calast.Type.TP Calast.Type.Int) in
  let () = Cal2cil_function.declare file None "__GET__" t [] in
  let t =
    Calast.Type.TA (Calast.Type.TP Calast.Type.Unit, Calast.TypeSchemeSet.
      empty, Calast.Type.TP Calast.Type.Unit) in
  let () = Cal2cil_function.declare file None "__WAIT__" t [] in
  let t =
    Calast.Type.TA (Calast.Type.TP Calast.Type.Int, Calast.TypeSchemeSet.
      empty,
      Calast.Type.TA (Calast.Type.TP Calast.Type.Int, Calast.TypeSchemeSet.
        empty, Calast.Type.TP Calast.Type.Unit)) in
  let params =
    [ {
        Calast.d_name = "port";
        d_type = (IS.empty, (Calast.Type.TP Calast.Type.Int));
        d_value = None;
      };
      {
        Calast.d_name = "v";
        d_type = (IS.empty, (Calast.Type.TP Calast.Type.Int));
        d_value = None;
      } ] in
  let () = Cal2cil_function.declare file None "__PUT__" t params in
  let name = "malloc" in
  let args = [ ("size", Cil.intType, []) ] in
  let t = Cil.TFun (Cil.voidPtrType, Some args, false, []) in
  let var = Cil.makeGlobalVar name t
  in
    (Cal2cil_common.add_global name var;
     file.Cil.globals <-
       file.Cil.globals @ [ Cil.GVarDecl (var, Cil.locUnknown) ])
  
(** This class extends visitExpr to replace each reference to a parameter
 of [parameters] by its value. *)
class networkParametersVisitor parameters =
  object inherit Astvisit.nopVisitor as super
           
    method visitExpr =
      fun expr ->
        match expr with
        | Calast.Var var ->
            (try
               let decl =
                 List.find (fun { Calast.d_name = dname } -> dname = var)
                   parameters in
               let e =
                 match decl.Calast.d_value with
                 | None -> failwith "This is all fucked up!"
                 | Some e -> e
               in e
             with | Not_found -> Calast.Var var)
        | _ -> super#visitExpr expr
      
  end
  
let replace_params actor code =
  let visitor = new networkParametersVisitor actor.Calast.ac_parameters
  in visitor#visitExpr code
  
(** [generate_process_body actor] generates the body of the process function
 of [actor]. We first retrieve the structure containing the actor variables,
 get the code of the process function using Schedastgen.ast_of_schedule,
 replace the references to parameters by their values. We add builtin
 functions to a CIL file, add the process function, and initialize the struct
 containing the actor variables. *)
let generate_process_body actor actions globals =
  let actor_struct =
    match globals with
    | Cil.GCompTag (compinfo, _) :: _ | Cil.GCompTagDecl (compinfo, _) :: _
        -> compinfo
    | _ -> failwith "No actor variables struct found" in
  let file = Cal2cil_common.fresh_file () in
  let (locals, code) =
    Schedastgen.ast_of_schedule actor.Calast.ac_fsm actions in
  let code = replace_params actor code in
  let () = add_builtins file in
  let () =
    (file.Cil.globals <- [];
     add_process_function file actor_struct locals code;
     init_struct file actor_struct actor.Calast.ac_parameters
       actor.Calast.ac_locals) in
  let printer = Cil.defaultCilPrinter in
  let contents = get_output_of (fun out -> Cil.dumpFile printer out "" file)
  in Str.global_replace re_process "" contents
  
(** [generate_extern actions] generates the C prototypes of [actions]. See
 the function [get_output_of] to see how we get the output of CIL. *)
let generate_extern globals =
  let file = Cal2cil_common.fresh_file () in
  let globals =
    List.filter
      (fun global ->
         match global with
         | Cil.GCompTag (_, _) | Cil.GCompTagDecl (_, _) |
             Cil.GVarDecl (_, _) | Cil.GText _ -> true
         | _ -> false)
      globals in
  (* MSVC hack part 2 (part 1 is in transl_action): in C++ we need to have
	an empty structure! *)
  let globals =
    match globals with
    | Cil.GCompTagDecl (compinfo, attrs) :: globals ->
        (Cil.GCompTag (compinfo, attrs)) :: globals
    | globals -> globals in
  let () = file.Cil.globals <- globals in
  let printer = Cil.defaultCilPrinter in
  let contents = get_output_of (fun out -> Cil.dumpFile printer out "" file)
  in contents
  
