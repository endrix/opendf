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
  
module E =
  Cal2c_util.Exception.Make(struct let module_name = "Cal2cil_expr"
                                      end)
  
open E
  
exception Make_call
  
let cst_unit = Cil.Const (Cil.CStr "__UNIT__")
  
(** This class checks if the variable [var] is used inside a collection
 (comprehension or generator). *)
class checkVarUseVisitor var isVarUsed =
  object inherit Astvisit.nopVisitor as super
           
    val mutable m_visitingCollection = false
      
    method visitVar =
      fun varUsed ->
        (if not !isVarUsed
         then isVarUsed := m_visitingCollection && (var = varUsed)
         else ();
         Calast.Var varUsed)
      
    method visitCollection =
      fun collection ->
        (m_visitingCollection <- true;
         let collection = super#visitCollection collection
         in (m_visitingCollection <- false; collection))
      
  end
  
let lval_of_exp =
  function
  | Cil.Lval lval -> lval
  | e ->
      (print_endline (Pretty.sprint ~width: 80 (Cil.d_exp () e));
       failwith "lval_of_exp: expression is not an l-value")
  
(** [compute_size el dl] computes the size of the generator list where [el]
 is a list of expressions, and [dl] the generator declarations list. *)
let compute_size fundec el dl =
  let (iterations, offset) =
    List.fold_left
      (fun (iterations, offset) decl ->
         match decl.Calast.d_value with
         | Some
             (Calast.Application ((Calast.Var "Integers"),
                ([ Calast.Literal (Calast.Integer f);
                   Calast.Literal (Calast.Integer e)
                 ])))
             ->
             let loop_size = (e + 1) - f in
             let offset_var =
               Cil.var (get_local fundec decl.Calast.d_name Cil.intType) in
             let offset =
               (match offset with
                | None -> Some (Cil.Lval offset_var)
                | Some e ->
                    Some
                      (Cil.BinOp (Cil.PlusA,
                         Cil.BinOp (Cil.Mult, Cil.integer loop_size, e, Cil.
                           intType),
                         Cil.Lval offset_var, Cil.intType)))
             in ((iterations * loop_size), offset)
         | Some
             (Calast.Application ((Calast.Var "Integers"),
                ([ Calast.Literal (Calast.Integer _); Calast.Var _ ])))
             -> (1, (Some (Cil.Lval (lval_of_var_name decl.Calast.d_name))))
         | _ -> failwith "compute_size: unable to compute size")
      (1, None) dl in
  let offset =
    match offset with
    | None -> failwith "compute_size: unexpected None offset"
    | Some offset -> offset in
  let el_length = List.length el in
  let size = iterations * el_length in
  let offset =
    Cil.BinOp (Cil.Mult, Cil.integer el_length, offset, Cil.intType)
  in (size, offset)
  
(** [add_assignment f lval stmts e] ... 

[call_of_application f stmts e args] ...

[create_list_statements f stmts offset tmp_list el] ...
[create_list_statements f stmts offset tmp_list el] adds statements to
 [stmts]. For each element ei (whose index is i) of el, a statement of the
 form: tmp_list[offset + i] = ei; is created.

[exp_of_application f stmts call args] ...
Returns a Cil.UnOp or Cil.BinOp expression if the application uses a
well-known operation (such as ">>", "&", "-"), or a Cil.Call otherwise, using
[call_of_application].

[exp_of_binop f stmts cal_e1 op cal_e2] ...

[exp_of_expr f stmts e] ...

[exp_of_if f stmts etest ethen eelse] ...
Cheap elimination of useless _if_ temp variables when one branch
returns the unit constant (called cst_unit). Thanks to type inference,
we are pretty damn sure we won't remove any important information.

[exp_of_indexer f stmts e1 e2] ...

[exp_of_list f stmts list] ...

[exp_of_switch f stmts test cases] transforms the Switch abstract node
to a Cil.Switch stmt. Note that the Switch statement kind in CIL is a bit
strange, it has first a block, in which the "implementation" of cases go,
which means it is where the "real" code is, augmented with labels, and
secondly it has a statement list, which basically is just a list of empty
statements with the same labels as the implementation statements. It is
probably used for CFG and stuff, but anyway we don't really care :)

[exp_of_unop f stmts op e] ...

[exp_of_var f stmts var] looks for an _actor_variables variable (either local
or parameter of [f]), to check if var is part of it or not.

[exp_of_while f stmts e1 e2] ...
*)
let rec add_assignment f lval stmts e =
  match Cil.typeOfLval lval with
  | Cil.TInt _ | Cil.TFloat _ ->
      Clist.append stmts
        (Clist.single
           (Cil.mkStmtOneInstr (Cil.Set (lval, e, Cil.locUnknown))))
  | Cil.TArray (_, length_opt, _) ->
      let length =
        (match length_opt with
         | None ->
             (match Cil.typeOfLval (lval_of_exp e) with
              | Cil.TArray (_, (Some length), _) -> length
              | _ -> failwith "add_assignment: no length!")
         | Some length -> length)
      in add_list_assignment f stmts e lval length
  | Cil.TPtr (_, _) ->
      let lval =
        (match Cil.typeOf e with
         | Cil.TArray _ -> lval
         | _ -> Cil.mkMem ~addr: (Cil.Lval lval) ~off: Cil.NoOffset)
      in
        Clist.append stmts
          (Clist.single
             (Cil.mkStmtOneInstr (Cil.Set (lval, e, Cil.locUnknown))))
  | _ -> failwith "add_assignment: type of lval"

and add_list_assignment f stmts e lval length =
  let iter = get_local f "i" Cil.intType in
  let lval =
    Cil.addOffsetLval (Cil.Index (Cil.Lval (Cil.var iter), Cil.NoOffset))
      lval in
  let e =
    Cil.Lval
      (Cil.addOffsetLval (Cil.Index (Cil.Lval (Cil.var iter), Cil.NoOffset))
         (lval_of_exp e)) in
  let body = [ Cil.mkStmtOneInstr (Cil.Set (lval, e, Cil.locUnknown)) ] in
  let for_loop =
    Cil.mkForIncr ~iter ~first: (Cil.integer 0) ~stopat: length
      ~incr: (Cil.integer 1) ~body
  in Clist.append stmts (Clist.fromList for_loop)

and call_of_application f stmts e args =
  let (e, stmts) = exp_of_expr f stmts e in
  let (t, fun_args) =
    match e with
    | Cil.Lval (((Cil.Var v, _) as lval)) ->
        (match Cil.typeOfLval lval with
         | Cil.TFun (t, (Some args), _, _) -> (t, args)
         | _ ->
             failwith
               ("call_of_application: " ^ (v.Cil.vname ^ " has no type!")))
    | _ -> failwith "call_of_application: function is not an l-value!" in
  let (args, stmts) =
    match args with
    | [ Calast.Unit ] -> (Clist.empty, stmts)
    | args ->
        List.fold_left
          (fun (args, stmts_args) arg ->
             let (e, stmts) = exp_of_expr f Clist.empty arg
             in
               ((Clist.append args (Clist.single e)),
                (Clist.append stmts_args stmts)))
          (Clist.empty, stmts) args in
  let args = Clist.toList args in
  let args =
    match fun_args with
    | (_, Cil.TPtr ((Cil.TComp (_, [])), []), _) :: _ ->
        (Cil.Lval (lval_of_var_name "_actor_variables")) :: args
    | _ -> args
  in
    if Cil.isVoidType t
    then
      (Cil.zero,
       (Clist.append stmts
          (Clist.single
             (Cil.mkStmtOneInstr (Cil.Call (None, e, args, Cil.locUnknown))))))
    else
      (let tmp = add_temp f "_call_" t in
       let lval_tmp = Cil.var tmp
       in
         ((Cil.Lval lval_tmp),
          (Clist.append stmts
             (Clist.single
                (Cil.mkStmtOneInstr
                   (Cil.Call (Some lval_tmp, e, args, Cil.locUnknown)))))))

and create_list_statements f stmts offset tmp_list el =
  snd
    (List.fold_left
       (fun (index, stmts) e ->
          let offset =
            Cil.BinOp (Cil.PlusA, offset, Cil.integer index, Cil.intType) in
          let lval =
            Cil.addOffsetLval (Cil.Index (offset, Cil.NoOffset)) tmp_list in
          let (e, stmts) = exp_of_expr f stmts e
          in ((index + 1), (add_assignment f lval stmts e)))
       (0, stmts) el)

and exp_of_application f stmts call args =
  match (call, args) with
  | (Calast.Var "println", args) ->
      call_of_application f stmts (Calast.Var "libcal_printf")
        (format_and_arguments f stmts args)
  | (Calast.Var func, [ e ]) ->
      (match func with
       | "abs" ->
           let (e, stmts) = exp_of_expr f stmts e
           in ((Cil.UnOp (Cil.Neg, e, Cil.typeOf e)), stmts)
       | "bitnot" ->
           let (e, stmts) = exp_of_expr f stmts e
           in ((Cil.UnOp (Cil.BNot, e, Cil.typeOf e)), stmts)
       | "Math_ceil" -> call_of_application f stmts (Calast.Var "ceil") args
       | "Math_log" -> call_of_application f stmts (Calast.Var "log") args
       | "Math_round" ->
           let call = Calast.Var "round"
           in call_of_application f stmts call args
       | _ -> call_of_application f stmts call args)
  | (Calast.Var func, [ e1; e2 ]) ->
      (try
         let op =
           match func with
           | "bitand" -> Cil.BAnd
           | "bitor" -> Cil.BOr
           | "bitxor" -> Cil.BXor
           | "lshift" -> Cil.Shiftlt
           | "rshift" -> Cil.Shiftrt
           | _ -> raise Make_call in
         let (e1, stmts) = exp_of_expr f stmts e1 in
         let (e2, stmts) = exp_of_expr f stmts e2
         in ((Cil.BinOp (op, e1, e2, Cil.intType)), stmts)
       with | Make_call -> call_of_application f stmts call args)
  | _ -> call_of_application f stmts call args

and exp_of_assign f stmts e1 e2 =
  let (cal_e1, stmts) = exp_of_expr f stmts e1
  in
    match (e1, e2) with
    | (Calast.Var var, Calast.List list) ->
        let isVarUsed = ref false
        in
          (ignore ((new checkVarUseVisitor var isVarUsed)#visitExpr e2);
           if !isVarUsed
           then
             (let (e2, stmts) = exp_of_expr f stmts e2
              in (cst_unit, (add_assignment f (lval_of_exp cal_e1) stmts e2)))
           else exp_of_list f stmts list (lval_of_exp cal_e1))
    | _ ->
        let (e2, stmts) = exp_of_expr f stmts e2
        in (cst_unit, (add_assignment f (lval_of_exp cal_e1) stmts e2))

and exp_of_binop f stmts cal_e1 op cal_e2 =
  let binop =
    match op with
    | Calast.Equal -> Cil.Eq
    | Calast.NotEqual -> Cil.Ne
    | Calast.Plus -> Cil.PlusA
    | Calast.Minus -> Cil.MinusA
    | Calast.Times -> Cil.Mult
    | Calast.Div -> Cil.Div
    | Calast.LessThan -> Cil.Lt
    | Calast.LessThanOrEqual -> Cil.Le
    | Calast.GreaterThan -> Cil.Gt
    | Calast.GreaterThanOrEqual -> Cil.Ge
    | Calast.Or -> Cil.LOr
    | Calast.And -> Cil.LAnd
    | Calast.Mod -> Cil.Mod in
  let (e1, stmts) = exp_of_expr f stmts cal_e1 in
  let e1type = Cil.typeOf e1 in
  let (e2, stmts) = exp_of_expr f stmts cal_e2 in
  let e2type = Cil.typeOf e2 in
  let typ =
    if e1type = e2type
    then e1type
    else
      if
        ((e1type = Cil.doubleType) && (e2type = Cil.intType)) ||
          ((e1type = Cil.intType) && (e2type = Cil.doubleType))
      then Cil.doubleType
      else
        (let doc1 = Cil.d_type () e1type in
         let doc2 = Cil.d_type () e2type
         in
           (Pretty.fprint stdout ~width: 80 doc1;
            Pretty.fprint stdout ~width: 80 doc2;
            fprintf stderr "exp_of_binop: incompatible type";
            e1type)) in
  let e = Cil.BinOp (binop, e1, e2, typ) in (e, stmts)

and exp_of_expr f stmts =
  function
  | Calast.Application (call, args) -> exp_of_application f stmts call args
  | Calast.Assign (e1, e2) -> exp_of_assign f stmts e1 e2
  | Calast.Function (_locals, _params, _e) ->
      failwith "exp_of_expr: Function"
  | Calast.BinaryOp (cal_e1, op, cal_e2) ->
      exp_of_binop f stmts cal_e1 op cal_e2
  | Calast.If (etest, ethen, eelse) -> exp_of_if f stmts etest ethen eelse
  | Calast.Indexer (e1, e2) -> exp_of_indexer f stmts e1 e2
  | Calast.List list ->
      let (size, _offset) =
        (match list with
         | Calast.Comprehension el -> ((List.length el), Cil.zero)
         | Calast.Generator (el, dl) -> compute_size f el dl) in
      let lval_list =
        let typ = Cil.TArray (Cil.intType, Some (Cil.integer size), [])
        in Cil.var (add_temp f "_list_" typ)
      in exp_of_list f stmts list lval_list
  | Calast.Literal literal ->
      let e =
        (match literal with
         | Calast.Boolean b -> if b then Cil.one else Cil.zero
         | Calast.Integer i -> Cil.integer i
         | Calast.Null -> Cil.Const (Cil.CStr "NULL")
         | Calast.Real f -> Cil.Const (Cil.CReal (f, Cil.FDouble, None))
         | Calast.String s -> Cil.Const (Cil.CStr s))
      in (e, stmts)
  | Calast.Statements (e1, e2) ->
      let (_, stmts) = exp_of_expr f stmts e1 in
      let (e, stmts) = exp_of_expr f stmts e2 in (e, stmts)
  | Calast.Switch (e, el) -> exp_of_switch f stmts e el
  | Calast.UnaryOp (op, e) -> exp_of_unop f stmts op e
  | Calast.Unit -> (cst_unit, stmts)
  | Calast.Var var -> exp_of_var f stmts var
  | Calast.While (e1, e2) -> exp_of_while f stmts e1 e2

and exp_of_if f stmts etest ethen eelse =
  let (etest, stest) = exp_of_expr f stmts etest in
  let (ethen, sthen) = exp_of_expr f Clist.empty ethen in
  let (eelse, selse) = exp_of_expr f Clist.empty eelse in
  let (eif, sthen, selse) =
    if (ethen = cst_unit) || (eelse = cst_unit)
    then (cst_unit, sthen, selse)
    else
      (let typ = Cil.typeOf ethen in
       let typ =
         match typ with
         | Cil.TArray (t, _, attrs) -> Cil.TPtr (t, attrs)
         | t -> t in
       let lval_tmp = Cil.var (add_temp f "_if_" typ) in
       let sthen = add_assignment f lval_tmp sthen ethen in
       let selse = add_assignment f lval_tmp selse eelse
       in ((Cil.Lval lval_tmp), sthen, selse)) in
  let sthen = Clist.toList sthen in
  let selse = Clist.toList selse in
  let stmtkind =
    Cil.If (etest, Cil.mkBlock sthen, Cil.mkBlock selse, Cil.locUnknown)
  in (eif, (Clist.append stest (Clist.single (Cil.mkStmt stmtkind))))

and exp_of_indexer f stmts e1 e2 =
  let (e1, stmts) = exp_of_expr f stmts e1
  in
    match e1 with
    | Cil.Lval lval ->
        let (e2, stmts) = exp_of_expr f stmts e2 in
        let e =
          Cil.Lval (Cil.addOffsetLval (Cil.Index (e2, Cil.NoOffset)) lval)
        in (e, stmts)
    | _ -> failwith "Indexer on a non-lvalue"

and exp_of_list f stmts list lval_list =
  let stmts =
    match list with
    | Calast.Comprehension el ->
        let offset = Cil.zero
        in create_list_statements f stmts offset lval_list el
    | Calast.Generator (el, dl) ->
        let (_size, offset) = compute_size f el dl in
        let for_stmts =
          Clist.toList
            (create_list_statements f Clist.empty offset lval_list el) in
        let for_stmts =
          List.fold_right
            (fun decl body ->
               let (first, stopat) =
                 match decl.Calast.d_value with
                 | Some
                     (Calast.Application ((Calast.Var "Integers"),
                        ([ Calast.Literal (Calast.Integer f);
                           Calast.Literal (Calast.Integer e)
                         ])))
                     -> ((Cil.integer 0), (Cil.integer ((e + 1) - f)))
                 | Some
                     (Calast.Application ((Calast.Var "Integers"),
                        ([ Calast.Literal (Calast.Integer f); Calast.Var n ])))
                     -> ((Cil.integer f), (Cil.Lval (lval_of_var_name n)))
                 | _ -> failwith "exp_of_list: loop not translatable" in
               let iter = get_local f decl.Calast.d_name Cil.intType
               in
                 Cil.mkForIncr ~iter ~first ~stopat ~incr: (Cil.integer 1)
                   ~body)
            dl for_stmts
        in Clist.append stmts (Clist.fromList for_stmts)
  in ((Cil.Lval lval_list), stmts)

and exp_of_switch f stmts test cases =
  let (etest, stmts) = exp_of_expr f stmts test in
  let (block, switch_stmts) =
    List.fold_left
      (fun (block, switch_stmts) (ecase, ebody) ->
         let (ecase, _) = exp_of_expr f Clist.empty ecase in
         let (_, stmt_body) = exp_of_expr f Clist.empty ebody in
         let stmt = Cil.mkEmptyStmt () in
         let () = stmt.Cil.labels <- [ Cil.Case (ecase, Cil.locUnknown) ] in
         let switch_stmts = stmt :: switch_stmts in
         let stmt = Cil.mkStmt (Cil.Break Cil.locUnknown) in
         let stmt_body = Clist.append stmt_body (Clist.single stmt) in
         let stmt =
           Cil.mkStmt (Cil.Block (Cil.mkBlock (Clist.toList stmt_body))) in
         let () = stmt.Cil.labels <- [ Cil.Case (ecase, Cil.locUnknown) ] in
         let block = stmt :: block in (block, switch_stmts))
      ([], []) cases in
  let (block, switch_stmts) = ((List.rev block), (List.rev switch_stmts))
  in
    (Cil.zero,
     (Clist.append stmts
        (Clist.single
           (Cil.mkStmt
              (Cil.Switch (etest, Cil.mkBlock block, switch_stmts, Cil.
                 locUnknown))))))

and exp_of_unop f stmts op e =
  let (e, stmts) = exp_of_expr f stmts e in
  let e =
    match op with
    | Calast.UMinus -> Cil.UnOp (Cil.Neg, e, Cil.typeOf e)
    | Calast.Not -> Cil.UnOp (Cil.LNot, e, Cil.typeOf e)
    | Calast.Reference ->
        (match e with
         | Cil.Lval (((Cil.Var var, _) as lval)) when
             not (Cil.isArrayType var.Cil.vtype) -> Cil.AddrOf lval
         | e -> e)
    | Calast.Contents -> e
  in (e, stmts)

and exp_of_var _f stmts var =
  try ((Cil.Lval (lval_of_var_name var)), stmts)
  with
  | Not_found ->
      (try
         let varinfo = varinfo_of_var_name "_actor_variables"
         in
           match varinfo with
           | { Cil.vtype = Cil.TPtr ((Cil.TComp (compinfo, [])), []) } ->
               (try
                  let fieldinfo = Cil.getCompField compinfo var
                  in
                    ((Cil.Lval (Cil.Mem (Cil.Lval (Cil.var varinfo)),
                        Cil.Field (fieldinfo, Cil.NoOffset))),
                     stmts)
                with
                | Not_found -> ((Cil.Lval (lval_of_var_name var)), stmts))
           | { Cil.vtype = Cil.TComp (compinfo, []) } ->
               (try
                  let fieldinfo = Cil.getCompField compinfo var
                  in
                    ((Cil.Lval (Cil.Var varinfo,
                        Cil.Field (fieldinfo, Cil.NoOffset))),
                     stmts)
                with
                | Not_found -> ((Cil.Lval (lval_of_var_name var)), stmts))
           | _ -> failwith "_actor_variables has a strange type"
       with
       | Not_found ->
           let varinfo = Cil.makeGlobalVar var Cil.intType
           in (add_global var varinfo; ((Cil.Lval (Cil.var varinfo)), stmts)))

and exp_of_while f stmts e1 e2 =
  let (guard, stmts) = exp_of_expr f stmts e1 in
  let (e2, stmts_body) = exp_of_expr f Clist.empty e2
  in
    (e2,
     (Clist.append stmts
        (Clist.fromList (Cil.mkWhile ~guard ~body: (Clist.toList stmts_body)))))

and format_and_arguments f stmts args =
  let rec args_of_println =
    function
    | Calast.BinaryOp (e1, Calast.Plus, e2) ->
        (args_of_println e1) @ (args_of_println e2)
    | expr -> [ expr ] in
  let args =
    match args with
    | [ arg ] -> args_of_println arg
    | _ -> failwith "Wrong argument list for println" in
  let (format, args) =
    List.fold_left
      (fun (format, args) expr ->
         let (e, _) = exp_of_expr f stmts expr in
         let t = Cil.typeOf e
         in
           match t with
           | Cil.TInt _ -> ((format ^ "%i"), (expr :: args))
           | Cil.TFloat _ -> ((format ^ "%f"), (expr :: args))
           | Cil.TPtr ((Cil.TInt (Cil.IChar, _)), _) |
               Cil.TPtr ((Cil.TInt (Cil.ISChar, _)), _) |
               Cil.TPtr ((Cil.TInt (Cil.IUChar, _)), _) ->
               (match expr with
                | Calast.Literal (Calast.String str) ->
                    ((format ^ str), args)
                | _ -> ((format ^ "%s"), (expr :: args)))
           | _ ->
               (print_endline (Pretty.sprint ~width: 80 (Cil.d_type () t));
                failwith "type not recognized"))
      ("", []) args in
  let format = format ^ "\n"
  in (Calast.Literal (Calast.String format)) :: (List.rev args)
  
