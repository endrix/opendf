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
  
open Calast
  
open Cal2c_util
  
module E = Exception.Make(struct let module_name = "Typeinf"
                                    end)
  
open E
  
open Typedefs
  
(*** Imports from Typedefs. *)
type environment = Typedefs.environment

let string_of_env = string_of_env
  
let string_of_type_scheme = string_of_type_scheme
  
let string_of_type = string_of_type
  
let mgu = mgu
  
(*** Fresh type variables and environment management ***)
let ftv_counter = ref 0
  
let ftv_counter_reset () = ftv_counter := 0
  
(** [ftv_create ()] creates a fresh type variable not occurring in, and
 returns the type variable number. *)
let ftv_create () = let n = !ftv_counter in (incr ftv_counter; n)
  
let add_decl env decl = SM.add decl.Calast.d_name decl env
  
(*** Instantiation and generalization ***)
let instantiate (vars, t) =
  (if !debug
   then
     printf "instantiate type_scheme %s\n%!"
       (string_of_type_scheme (vars, t))
   else ();
   let s =
     IS.fold
       (fun alpha s ->
          let fresh_beta = ftv_create ()
          in substitution_add s alpha (Type.TV fresh_beta))
       vars substitution_empty
   in
     (if !debug
      then printf "instantiate: %s\n%!" (string_of_substitution s)
      else ();
      substitute s t))
  
let generalize env t =
  ((IS.diff (freevars_of_type t) (freevars_of_env env)), t)
  
(*** Function types ***)
let initial_environment = Typeinit.initial_environment
  
let is_list_with_size = function
	| Type.TL (Some _, _)
	| Type.TR (Type.TL (Some _, _)) -> true
	| _ -> false
	
(** [w indent env l] returns a couple [(s, t)], where [s] is a
    substitution and [t] a type, for the lambda expression [l],
		using the local environment [env]. *)
let rec w ?(level = 0) env e =
  try
    match e with
    | Term term ->
        let ts = type_of_term env term
        in (substitution_empty, (instantiate ts))
    | Pair (e1, e2) ->
        let (s1, t1) = w ~level: (level + 1) env e1 in
        let (s2, t2) = w ~level: (level + 1) (substitute_env s1 env) e2 in
        let t = Type.Tpair (t1, t2) in ((substitution_compose s2 s1), t)
    | Application (e1, e2) ->
        let (s1, t1) = w ~level: (level + 1) env e1 in
        let (s2, t2) = w ~level: (level + 1) (substitute_env s1 env) e2 in
        let i = ftv_create () in
        let beta = Type.TV i in
        let ta = substitute s2 t1 in
        let tb = Type.TA (t2, TypeSchemeSet.empty, beta) in
        let v = mgu env true ta tb in
        let s = substitution_compose (substitution_compose v s2) s1 in
        let sigma = substitute v beta in (s, sigma)
    | Lambda (x, e1) ->
        (****** save old type-scheme freevars and type ******)
        let x_fv = fst x.Calast.d_type in
        let x_t = snd x.Calast.d_type in (****** creates new variable ******)
        let i = ftv_create () in
        let beta = Type.TV i in
        (************** sets type-scheme **************)
        let () = x.Calast.d_type <- (IS.empty, beta) in
        let env = add_decl env x in
        let (s1, t1) = w ~level: (level + 1) env e1 in
        let new_type = substitute_type_scheme s1 x.Calast.d_type in
        (* hack to allow user types. When there are no quantified          *)
        (* variables, we try to unify user type and inferred type (to      *)
        (* check that user type is consistent with inferred type), and use *)
        (* the user type                                                   *)
        let beta =
          if IS.is_empty x_fv || is_list_with_size x_t
          then
            (let _ = mgu env true x_t (snd new_type) in
             let () = x.Calast.d_type <- (IS.empty, x_t) in x_t)
          else (let () = x.Calast.d_type <- new_type in beta) in
        let (s, t) =
          (s1, (substitute s1 (Type.TA (beta, TypeSchemeSet.empty, t1))))
        in (s, t)
    | Let (x, e1, e2) ->
        let (s1, t1) = w ~level: (level + 1) env e1 in
        let env = substitute_env s1 env
        in
          (x.Calast.d_type <- generalize env t1;
           let env = add_decl env x in
           let (s2, t2) = w ~level: (level + 1) env e2
           in ((substitution_compose s2 s1), t2))
  with
  | Not_unifiable (depth, env, t1, t2) ->
      let slambda = string_of_lambda env "\t   " e
      in
        if depth = 0
        then
          (print_endline (string_of_unification_error t1 t2);
           print_endline
             "Typing history: (<list of (variable: inferred type)> statements)";
           print_endline ("\t * " ^ slambda);
           raise (Not_unifiable (depth + 1, env, t1, t2)))
        else
          (let () = print_endline ("\t * " ^ slambda)
           in
             if (level = 0) || (depth = 5)
             then (print_endline "\t..."; failwith "Type error")
             else raise (Not_unifiable (depth + 1, env, t1, t2)))
  | Unbound (depth, x) ->
      let slambda = string_of_lambda env "\t   " e
      in
        if depth = 0
        then
          (printf "I do not know of any %s identifier!\n" x;
           print_endline
             "Typing history: (<list of (variable: inferred type)> statements)";
           print_endline ("\t * " ^ slambda);
           raise (Unbound (depth + 1, x)))
        else
          (let () = print_endline ("\t * " ^ slambda)
           in
             if (level = 0) || (depth = 5)
             then (print_endline "\t..."; failwith "Type error")
             else raise (Unbound (depth + 1, x)))
  
(**
  [lambda_of_expr expr] transforms expr to its equivalent lambda calculus
   representation.

   Details:
     Var x -> if x is a builtin function => Term (Function x)
              else => Term (Var x)
     Literal l -> Term (Literal l)
     UnaryOp (uop, e) -> App (uop, e)
     BinaryOp (e1, bop, e2) -> App(App(bop, e1), e2)
     Application (call, args) -> App(...App(App(e, arg1), arg2), ...argn)
     If(ec, et, ee) -> App(App(App(if, ec), et), ee)
		 Indexer (e, [e1; e2]) -> App(App("List.nth", App(App("List.nth", e), e1)), e2)
     List
*)
let rec lambda_of_expr env =
  function
  | Calast.Application (call, args) ->
      let (env, e) = lambda_of_expr env call
      in
        List.fold_left
          (fun (env, e) arg ->
             let (env, arg) = lambda_of_expr env arg
             in (env, (Application (e, arg))))
          (env, e) args
  | Calast.Assign (e1, e2) ->
      let (env, l1) = lambda_of_expr env e1 in
      let (env, l2) = lambda_of_expr env e2
      in (env, (Application (Application (Term (Var ":="), l1), l2)))
  | Calast.BinaryOp (e1, bop, e2) ->
      let (env, l1) = lambda_of_expr env e1 in
      let (env, l2) = lambda_of_expr env e2
      in (env, (Application (Application (Term (BinaryOp bop), l1), l2)))
  | Calast.Function (params, locals, e) ->
      (* adds local variables as let bindings *)
      let (env, e) =
        List.fold_right
          (fun decl (env, e2) ->
             match decl.Calast.d_value with
             | None ->
                 let i = ftv_create () in
                 let beta = Type.TV i
                 in
                   (decl.Calast.d_type <- (IS.empty, beta);
                    let env = add_decl env decl in (env, e2))
             | Some e1 ->
                 let (env, e1) = lambda_of_expr env e1
                 in (env, (Let (decl, e1, e2))))
          locals (lambda_of_expr env e) in
      (* adds parameters : f (a, b) = e => fun a -> b -> e *)
      let e = List.fold_right (fun decl e -> Lambda (decl, e)) params e
      in (env, e)
  | Calast.If (ec, et, ee) ->
      let (env, lc) = lambda_of_expr env ec in
      let (env, lt) = lambda_of_expr env et in
      let (env, le) = lambda_of_expr env ee
      in
        (env,
         (Application (Application (Application (Term (Var "if"), lc), lt),
            le)))
  | Calast.Indexer (e1, e2) ->
      let (env, l1) = lambda_of_expr env e1 in
      let (env, l2) = lambda_of_expr env e2
      in (env, (Application (Application (Term (Var "List_nth"), l1), l2)))
  | Calast.List collection ->
      (match collection with
       | Calast.Generator (el, dl) ->
           let (env, e) =
             lambda_of_expr env (Calast.List (Calast.Comprehension el)) in
           let (env, e) =
             List.fold_left
               (fun (env, e) d ->
                  let eg =
                    match d.Calast.d_value with
                    | None -> failwith "No value in the generator"
                    | Some eg -> eg in
                  let (env, eg) = lambda_of_expr env eg
                  in
                    (env,
                     (Application
                        (Application (Term (Var "List_map"), Lambda (d, e)),
                        eg))))
               (env, e) dl in
           let res = ref e
           in
             (for i = 1 to List.length dl do
                res := Application (Term (Var "List_flatten"), !res)
              done;
              (env, (!res)))
       | Calast.Comprehension el ->
           List.fold_right
             (fun e (env, l) ->
                let (env, e) = lambda_of_expr env e
                in (env, (Application (Application (Term (Var "::"), e), l))))
             el (env, (Term (Var "[]"))))
  | Calast.Literal l -> (env, (Term (Literal l)))
  | Calast.Statements (e1, e2) ->
      let (env, l1) = lambda_of_expr env e1 in
      let (env, l2) = lambda_of_expr env e2
      in (env, (Application (Application (Term (Var ";"), l1), l2)))
  | Calast.Switch _ ->
      failwith
        "Trying to convert a Switch statement to Lambda-calculus:\
this is a fatal error and should never ever happen, as switches do not\
exist in CAL."
  | Calast.UnaryOp (uop, e) ->
      let (env, e) = lambda_of_expr env e
      in (env, (Application (Term (UnaryOp uop), e)))
  | Calast.Unit -> (env, (Term (Var "()")))
  | Calast.Var x -> (env, (Term (Var x)))
  | Calast.While (e1, e2) ->
      let (env, l1) = lambda_of_expr env e1 in
      let (env, l2) = lambda_of_expr env e2
      in (env, (Application (Application (Term (Var "while"), l1), l2)))
  
let type_of_expr debug_enabled env e =
  (debug := debug_enabled;
   ftv_counter_reset ();
   let (env, l) = lambda_of_expr env e
   in
     try
       let (_s, t) = w env l
       in (* let _ = substitute_env s env in *) ((freevars_of_type t), t)
     with
     | e ->
         (printf "Type error while typing %s\n%!" (string_of_lambda env "" l);
          raise e))
  
