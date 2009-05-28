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
  
module E =
  Cal2c_util.Exception.Make(struct let module_name = "Cal2cil_common"
                                      end)
  
open E
  
let parameters = SH.create 10
  
let locals = SH.create 10
  
let globals = SH.create 10
  
let varinfo_of_var_name var_name =
  try SH.find locals var_name
  with
  | Not_found ->
      (try SH.find parameters var_name
       with | Not_found -> SH.find globals var_name)
  
let lval_of_var_name var_name =
  let varinfo = varinfo_of_var_name var_name in Cil.var varinfo
  
let add_global name var = SH.replace globals name var
  
let get_local f name typ =
  try SH.find locals name
  with
  | Not_found ->
      let var = Cil.makeLocalVar f name typ
      in (SH.replace locals name var; var)
  
let add_parameter f name typ =
  let var = Cil.makeFormalVar f name typ
  in (SH.replace parameters name var; var)
  
let add_temp f name typ =
  let var = Cil.makeTempVar f ~name typ
  in (SH.replace locals var.Cil.vname var; var)
  
let clear_locals () = SH.clear locals
  
let clear_parameters () = SH.clear parameters
  
let declare_printf file =
  let comment =
    Cil.GText "// To enable traces, change 1 by 0 in the #if below" in
  let ifdef = Cil.GText "#if 1" in
  let args = [ ("format", Cil.charConstPtrType, []) ] in
  let t = Cil.TFun (Cil.voidType, Some args, true, []) in
  let var = Cil.makeGlobalVar "libcal_printf" t in
  let printf = Cil.GVarDecl (var, Cil.locUnknown) in
  let define = Cil.GText "#define libcal_printf //" in
  let endif = Cil.GText "#endif"
  in
    (file.Cil.globals <-
       printf :: comment :: ifdef :: define :: endif :: file.Cil.globals;
     add_global "libcal_printf" var)
  
let fresh_file () =
  let file = Cil.dummyFile in
  let () =
    (file.Cil.globals <- [];
     file.Cil.globinit <- None;
     file.Cil.globinitcalled <- false)
  in (declare_printf file; file)
  
let rec cil_of_type =
  function
  | Calast.Type.TP Calast.Type.Bool | Calast.Type.TP Calast.Type.Int |
      Calast.Type.TV _ -> Cil.intType
  | Calast.Type.TP Calast.Type.Float -> Cil.doubleType
  | Calast.Type.TP Calast.Type.String -> Cil.charPtrType
  | Calast.Type.TP Calast.Type.Unit -> Cil.voidType
  | Calast.Type.TL (length, t) | Calast.Type.TR (Calast.Type.TL (length, t))
      ->
      (* a list type and a list reference type have the same C type: an array *)
      (match length with
       | None -> Cil.TArray (cil_of_type t, None, [])
       | Some i -> Cil.TArray (cil_of_type t, Some (Cil.integer i), []))
  | Calast.Type.TR t -> Cil.TPtr (cil_of_type t, [])
  | (* default... *) t ->
      failwith
        ("cil_of_type: " ^ ((Typeinf.string_of_type t) ^ " not translatable"))
  
