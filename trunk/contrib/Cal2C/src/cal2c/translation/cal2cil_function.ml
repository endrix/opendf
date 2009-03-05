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
  
module E = Exception.Make(struct let module_name = "Cal2cil_function"
                                    end)
  
open E
  
(** [set_function_signature f t params] adds the parameters [params] to the
 function [f], and sets its return type, using the type [t]. If [f] has no
 arguments, it sets them as [Some []] which gets translated as by CIL as
 f(). *)
let set_function_signature f t params =
  (* adds parameters to the function, and gives back its return type. *)
  let t =
    match t with
    | Calast.Type.TA ((Calast.Type.TP Calast.Type.Unit), _, t) -> t
    | t ->
        List.fold_left
          (fun t decl ->
             let remaining =
               match t with
               | Calast.Type.TA (_, _, remaining) -> remaining
               | _ ->
                   failwith
                     ("Incorrect type in function: " ^
                        (Typeinf.string_of_type t)) in
             let t = snd decl.Calast.d_type in
             let typ = Cal2cil_common.cil_of_type t
             in
               (ignore
                  (Cal2cil_common.add_parameter f decl.Calast.d_name typ);
                remaining))
          t params in
  let () =
    match f.Cil.svar.Cil.vtype with
    | Cil.TFun (_, args, false, []) ->
        let args = (match args with | None -> Some [] | _ -> args)
        in
          f.Cil.svar.Cil.vtype <-
            Cil.TFun (Cal2cil_common.cil_of_type t, args, false, [])
    | _ -> ()
  in t
  
let add_locals f t locals expr =
  let (locals, expr) =
    match t with
    | Calast.Type.TP Calast.Type.Unit -> (locals, expr)
    | t ->
        let res =
          {
            Calast.d_name = "res";
            d_type = (IS.empty, t);
            d_value = Some expr;
          }
        in ((locals @ [ res ]), (Calast.Var "res")) in
  let expr =
    List.fold_right
      (fun decl expr ->
         match decl.Calast.d_value with
         | None -> expr
         | Some e ->
             Calast.Statements
               (Calast.Assign (Calast.Var decl.Calast.d_name, e), expr))
      locals expr
  in
    (List.iter
       (fun decl ->
          let t = snd decl.Calast.d_type in
          let typ = Cal2cil_common.cil_of_type t
          in ignore (Cal2cil_common.get_local f decl.Calast.d_name typ))
       locals;
     expr)
  
(** [define debug file name t params locals expr] defines a function named
 [name], in the CIL file [file], with the type [t], parameters [params],
 local variables [locals], and a body [expr]. We first clear parameters and
 locals, and adds the parameters (with their type). We then declare and
 initialize the locals. Finally, we translate the function body. *)
let define file actor_struct name t params locals expr =
  (Cal2cil_common.clear_parameters ();
   Cal2cil_common.clear_locals ();
   let f = Cil.emptyFunction name in
   (* adds the _actor_variables parameter. *)
   let () =
     match actor_struct with
     | None -> ()
     | Some actor_struct ->
         ignore
           (Cal2cil_common.add_parameter f "_actor_variables"
              (Cil.TPtr (Cil.TComp (actor_struct, []), []))) in
   let t = set_function_signature f t params in
   let expr = add_locals f t locals expr in
   let stmts =
     match t with
     | Calast.Type.TP Calast.Type.Unit ->
         let (_, stmts) = Cal2cil_expr.exp_of_expr f Clist.empty expr
         in stmts
     | _ ->
         let (_, stmts) = Cal2cil_expr.exp_of_expr f Clist.empty expr
         in
           (match Clist.toList stmts with
            | [ { Cil.skind = Cil.Instr ([ Cil.Set (lval, e, _) ]) } ] when
                (lval = (Cal2cil_common.lval_of_var_name "res")) &&
                  (e = Cal2cil_expr.cst_unit)
                -> Clist.empty
            | _ ->
                let stmt_return =
                  Cil.mkStmt
                    (Cil.Return
                       (Some
                          (Cil.Lval (Cal2cil_common.lval_of_var_name "res")),
                       Cil.locUnknown))
                in Clist.append stmts (Clist.single stmt_return))
   in
     (f.Cil.sbody <- Cil.mkBlock (Clist.toList stmts);
      file.Cil.globals <- file.Cil.globals @ [ Cil.GFun (f, Cil.locUnknown) ]))
  
(* Translates a function. *)
let declare file actor_struct name t params =
  let (t, args) =
    match t with
    | Calast.Type.TA ((Calast.Type.TP Calast.Type.Unit), _, t) -> (t, [])
    | t ->
        List.fold_left
          (fun (t, args) decl ->
             let t =
               match t with
               | Calast.Type.TA (_, _, remaining) -> remaining
               | _ ->
                   failwith
                     ("Incorrect type in function: " ^
                        (Typeinf.string_of_type t)) in
             let typ = Cal2cil_common.cil_of_type (snd decl.Calast.d_type)
             in (t, (((decl.Calast.d_name), typ, []) :: args)))
          (t, []) params in
  let args = List.rev args in
  let args =
    match actor_struct with
    | None -> args
    | Some actor_struct ->
        let arg =
          ("_actor_variables", (Cil.TPtr (Cil.TComp (actor_struct, []), [])),
           [])
        in arg :: args in
  let t = Cal2cil_common.cil_of_type t in
  let t = Cil.TFun (t, Some args, false, []) in
  let var = Cil.makeGlobalVar name t
  in
    (Cal2cil_common.add_global name var;
     file.Cil.globals <-
       file.Cil.globals @ [ Cil.GVarDecl (var, Cil.locUnknown) ])
  
