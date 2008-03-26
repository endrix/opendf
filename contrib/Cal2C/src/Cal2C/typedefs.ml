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
  
module E = Exception.Make(struct let module_name = "Typedefs"
                                    end)
  
open E
  
(*** Debug flag ***)
let debug = ref false
  
(** Lambda terms *)
type term =
  | (** Variable lambda term *)
  Var of Calast.var
  | (** Unary operator lambda term *)
  UnaryOp of Calast.uop
  | (** Binary operator lambda term *)
  BinaryOp of Calast.bop
  | (** Literal: int, float... lambda term *)
  Literal of Calast.literal

(** Lambda calculus syntax *)
type lambda =
  | (** Lambda term *)
  Term of term
  | (** Lambda application *)
  Application of lambda * lambda
  | (** Pair *)
  Pair of lambda * lambda
  | (** Lambda abstraction *)
  Lambda of Calast.decl * lambda
  | (** Lambda let *)
  Let of Calast.decl * lambda * lambda

type environment = Calast.decl Cal2c_util.SM.t

(** Raised when two types cannot be unified:
 Not_unifiable (lambda-expr level, environment (at the moment the type error
 occurred), t1, t2). *)
exception
  Not_unifiable of int * environment * Type.definition * Type.definition
  
(** Raised when an identifier is unbound. (lambda-expr level, id) *)
exception Unbound of int * string
  
(*** Printing functions ***)
(** [string_of_primitive p] returns a string description of a primitive type. *)
let string_of_primitive =
  function
  | Type.Bool -> "bool"
  | Type.Float -> "float"
  | Type.Int -> "int"
  | Type.String -> "string"
  | Type.Unit -> "unit"
  
let rec string_of_type =
  function
  | Type.TV i ->
      let rec i2s prefix i =
        let s i =
          let c = char_of_int ((int_of_char 'a') + i) in String.make 1 c
        in
          if i >= 26
          then (let idiv = (i / 26) - 1 in i2s (i2s prefix idiv) (i mod 26))
          else prefix ^ (s i)
      in i2s "'" i
  | Type.TP p -> string_of_primitive p
  | Type.TL (_, t) -> "(" ^ ((string_of_type t) ^ ") list")
  | Type.TR t -> "(" ^ ((string_of_type t) ^ ") ref")
  | Type.Tpair (t1, t2) -> (string_of_type t1) ^ (", " ^ (string_of_type t2))
  | Type.TA (t1, _, t2) ->
      (string_of_type t1) ^ (" -> " ^ (string_of_type t2))
  
(** [string_of_substitution s] returns a string representation of [s]. *)
let string_of_substitution s =
  sprintf "[%s]"
    (String.concat ", "
       (List.rev
          (Cal2c_util.IM.fold
             (fun i t list ->
                (sprintf "(%s => %s)" (string_of_type (Type.TV i))
                   (string_of_type t)) ::
                  list)
             s [])))
  
(** [string_of_type_scheme ts] returns a string description of the type scheme
 [ts], composed of quantified variables and a type. *)
let string_of_type_scheme (vars, t) =
  let t = string_of_type t
  in
    if vars = IS.empty
    then t
    else
      (let qvars =
         String.concat ", "
           (List.map (fun i -> string_of_type (Type.TV i)) (IS.elements vars))
       in sprintf "forall %s.%s" qvars t)
  
let string_of_env env =
  sprintf "{%s}"
    (String.concat ", "
       (List.rev
          (SM.fold
             (fun x decl list ->
                (sprintf "%s: %s" x
                   (string_of_type_scheme decl.Calast.d_type)) ::
                  list)
             env [])))
  
(** [string_of_lambda l] returns a string representation of [l]. *)
let rec string_of_list_cons env indent e =
  match e with
  | Application ((Application ((Term (Var "::")), ehead)), (Term (Var "[]")))
      -> string_of_lambda env indent ehead
  | Application ((Application ((Term (Var "::")), ehead)), etail) ->
      let (vars1, str1) = string_of_lambda env indent ehead in
      let (vars2, str2) = string_of_list_cons env indent etail
      in ((DS.union vars1 vars2), (str1 ^ ("; " ^ str2)))
  | e -> string_of_lambda env indent e

and string_of_fundec env indent e =
  match e with
  | Lambda (x, ((Lambda _ as e1))) ->
      let (vars, str) = string_of_fundec env indent e1
      in (vars, (x.Calast.d_name ^ (", " ^ str)))
  | Lambda (x, e1) ->
      let indent = indent ^ "  " in
      let (vars, str) = string_of_lambda env indent e1
      in (vars, (x.Calast.d_name ^ (") -> \n" ^ (indent ^ str))))
  | e -> string_of_lambda env indent e

and string_of_lambda env indent e =
  match e with
  | Term x ->
      (match x with
       | Var var ->
           let ds =
             (try DS.singleton (SM.find var env) with | Not_found -> DS.empty)
           in (ds, var)
       | Literal literal -> (DS.empty, (Calast.string_of_literal literal))
       | UnaryOp uop -> (DS.empty, (Calast.string_of_uop uop))
       | BinaryOp bop -> (DS.empty, (Calast.string_of_bop bop)))
  | Pair (e1, e2) ->
      let (vars1, str1) = string_of_lambda env indent e1 in
      let (vars2, str2) = string_of_lambda env indent e2 in
      let str = "(" ^ (str1 ^ (", " ^ (str2 ^ ")")))
      in ((DS.union vars1 vars2), str)
  | Application ((Term (UnaryOp Contents)), e) ->
      string_of_lambda env indent e
  | Application ((Application ((Term (BinaryOp op)), e1)), e2) ->
      let (vars1, str1) = string_of_lambda env indent e1 in
      let (vars2, str2) = string_of_lambda env indent e2 in
      let str = str1 ^ (" " ^ ((Calast.string_of_bop op) ^ (" " ^ str2)))
      in ((DS.union vars1 vars2), str)
  | Application ((Term (Var "List_nth")), elist) ->
      let (vars, str) = string_of_lambda env indent elist
      in (vars, (str ^ "[...]"))
  | Application ((Application ((Term (Var "List_nth")), elist)), eindex) ->
      let (vars1, str1) = string_of_lambda env indent elist in
      let (vars2, str2) = string_of_lambda env indent eindex
      in ((DS.union vars1 vars2), (str1 ^ ("[" ^ (str2 ^ "]"))))
  | Application ((Term (Var "::")), e) ->
      let (vars, str) = string_of_lambda env indent e
      in (vars, ("[" ^ (str ^ "; ...]")))
  | Application ((Application ((Term (Var "::")), _)), _) ->
      let (vars, str) = string_of_list_cons env indent e
      in (vars, ("[" ^ (str ^ "]")))
  | Application ((Application ((Term (Var ":=")), e1)), e2) ->
      let (vars1, str1) = string_of_lambda env indent e1 in
      let (vars2, str2) = string_of_lambda env indent e2
      in ((DS.union vars1 vars2), (str1 ^ (" := " ^ str2)))
  | Application ((Term (Var ";")), e) ->
      let (vars, str) = string_of_lambda env indent e in
      let s2 = indent ^ "..." in (vars, (str ^ (";\n" ^ s2)))
  | Application ((Application ((Term (Var ";")), e1)), e2) ->
      let (vars1, str1) = string_of_lambda env indent e1 in
      let str1 = str1 ^ ";\n" in
      let (vars2, str2) = string_of_lambda env indent e2 in
      let str2 = indent ^ str2 in ((DS.union vars1 vars2), (str1 ^ str2))
  | Application (e1, e2) ->
      let (vars1, str1) = string_of_lambda env indent e1 in
      let (vars2, str2) = string_of_lambda env indent e2
      in ((DS.union vars1 vars2), ("(" ^ (str1 ^ (" " ^ (str2 ^ ")")))))
  | Lambda (_, _) ->
      let (vars, str) = string_of_fundec env indent e
      in (vars, ("function (" ^ str))
  | Let (x, e1, e2) ->
      let (vars1, str1) = string_of_lambda env indent e1 in
      let (vars2, str2) = string_of_lambda env indent e2
      in
        ((DS.union vars1 vars2),
         ("let " ^ (x.Calast.d_name ^ (" = " ^ (str1 ^ (" in " ^ str2))))))
  
let string_of_lambda env indent e =
  let (vars, str) = string_of_lambda env indent e in
  let vars =
    String.concat ", "
      (List.map
         (fun decl ->
            sprintf "%s: %s" decl.Calast.d_name
              (string_of_type_scheme decl.Calast.d_type))
         (DS.elements vars))
  in ("<" ^ (vars ^ "> ")) ^ str
  
(*** freevars implementation ***)
let freevars_of_type =
  let rec aux vars =
    function
    | Type.TP _ -> vars
    | Type.TV i -> IS.add i vars
    | Type.TL (_, t) | Type.TR t -> aux vars t
    | Type.TA (t1, _, t2) | Type.Tpair (t1, t2) -> aux (aux vars t1) t2
  in aux IS.empty
  
let freevars_of_type_scheme (vars, t) = IS.diff (freevars_of_type t) vars
  
let freevars_of_env env =
  SM.fold
    (fun _ { Calast.d_type = ts } vars ->
       IS.union vars (freevars_of_type_scheme ts))
    env IS.empty
  
(*** Substitution ***)
let substitution_empty = Cal2c_util.IM.empty
  
let substitution_add s i t = Cal2c_util.IM.add i t s
  
(** [substitute s t] returns a new type definition where free variables
 in [t] have been replaced by their type in [s]. If there is no substitution
 for [t], it is kept generic. *)
let rec substitute s =
  function
  | Type.TP p -> Type.TP p
  | Type.TV i -> (try Cal2c_util.IM.find i s with | Not_found -> Type.TV i)
  | Type.TL (l, t) -> Type.TL (l, substitute s t)
  | Type.TR t -> Type.TR (substitute s t)
  | Type.TA (t1, v, t2) -> Type.TA (substitute s t1, v, substitute s t2)
  | Type.Tpair (t1, t2) -> Type.Tpair (substitute s t1, substitute s t2)
  
(*** Substitution for type-schemes and environment ***)
let substitute_type_scheme s (vars, t) =
  let freevars = freevars_of_type_scheme (vars, t)
  in
    (if !debug
     then
       (printf "substitute_type_scheme: %s\n%!"
          (string_of_type_scheme (vars, t));
        printf "freevars: %s\n%!" (string_of_type_scheme (freevars, t)))
     else ();
     let rec substitute s =
       function
       | Type.TP p -> Type.TP p
       | Type.TV i -> (* substitution only acts on free type variables *)
           if IS.mem i freevars
           then (try Cal2c_util.IM.find i s with | Not_found -> Type.TV i)
           else Type.TV i
       | Type.TL (l, t) -> Type.TL (l, substitute s t)
       | Type.TR t -> Type.TR (substitute s t)
       | Type.TA (t1, v, t2) -> Type.TA (substitute s t1, v, substitute s t2)
       | Type.Tpair (t1, t2) -> Type.Tpair (substitute s t1, substitute s t2) in
     let t = substitute s t in (vars, t))
  
let substitute_env s env =
  (if !debug
   then
     printf "substitute_env: %s %s\n%!" (string_of_substitution s)
       (string_of_env env)
   else ();
   SM.iter
     (fun _ decl ->
        let ts = substitute_type_scheme s decl.Calast.d_type
        in decl.Calast.d_type <- ts)
     env;
   env)
  
let substitution_fold s f v = Cal2c_util.IM.fold f s v
  
let substitution_is_bound s i = Cal2c_util.IM.mem i s
  
(** [compose s1 s2] applies the [s1] substitution to every type
    in [s2], then adds substitutions of s1 whose type variable are not bound in
    s2, and returns the new substitution. *)
let substitution_compose s1 s2 =
  let s =
    substitution_fold s2
      (fun i2 t2 s -> substitution_add s i2 (substitute s1 t2))
      substitution_empty
  in
    substitution_fold s1
      (fun i1 t1 s ->
         if substitution_is_bound s2 i1 then s else substitution_add s i1 t1)
      s
  
(*** Term types ***)
(** [type_of_uop s uop] returns a couple [s, t] where [s] is the new
 substitution obtained by adding the type [t] of [uop] to the
 original substitution. *)
let type_of_uop =
  function
  | Calast.Contents ->
      ((IS.singleton 0),
       (Type.TA (Type.TR (Type.TV 0), TypeSchemeSet.empty, Type.TV 0)))
  | Calast.Not ->
      (IS.empty,
       (Type.TA (Type.TP Type.Bool, TypeSchemeSet.empty, Type.TP Type.Bool)))
  | Calast.Reference ->
      ((IS.singleton 0),
       (Type.TA (Type.TV 0, TypeSchemeSet.empty, Type.TR (Type.TV 0))))
  | Calast.UMinus ->
      ((IS.singleton 0),
       (Type.TA (Type.TV 0, TypeSchemeSet.empty, Type.TV 0)))
  
(** [type_of_bop s bop] returns a couple [s, t] where [s] is the new
 substitution obtained by adding the type [t] of [bop] to the
 original substitution. *)
let type_of_bop =
  function
  | Calast.Plus | Calast.Minus | Calast.Times | Calast.Div |
      Calast.Mod ->
      ((IS.singleton 0),
       (Type.TA (Type.TV 0, TypeSchemeSet.empty,
          Type.TA (Type.TV 0, TypeSchemeSet.empty, Type.TV 0))))
  | Calast.Equal | Calast.NotEqual | Calast.LessThan | Calast.
      LessThanOrEqual | Calast.GreaterThan | Calast.GreaterThanOrEqual ->
      ((IS.singleton 0),
       (Type.TA (Type.TV 0, TypeSchemeSet.empty,
          Type.TA (Type.TV 0, TypeSchemeSet.empty, Type.TP Type.Bool))))
  | Calast.Or | Calast.And ->
      (IS.empty,
       (Type.TA (Type.TP Type.Bool, TypeSchemeSet.empty,
          Type.TA (Type.TP Type.Bool, TypeSchemeSet.empty, Type.TP Type.Bool))))
  
let type_of_term env term =
  match term with
  | BinaryOp bop -> type_of_bop bop
  | Literal literal ->
      let p =
        (match literal with
         | Calast.Boolean _ -> Type.Bool
         | Calast.Integer _ -> Type.Int
         | Calast.Null -> Type.Unit
         | Calast.Real _ -> Type.Float
         | Calast.String _ -> Type.String)
      in (IS.empty, (Type.TP p))
  | UnaryOp uop -> type_of_uop uop
  | Var x ->
      (try let decl = SM.find x env in decl.Calast.d_type
       with | Not_found -> raise (Unbound (0, x)))
  
let string_of_unification_error t1 t2 =
  let s =
    match (t1, t2) with
    | (Type.TP _, Type.TP _) ->
        sprintf "primitive types %s and %s are NOT interchangeable"
          (string_of_type t1) (string_of_type t2)
    | (Type.TP _, Type.TL _) ->
        sprintf
          "we expected a primitive type (something like \"%s\"), and all we \
					 got was this lousy list type: %s."
          (string_of_type t1) (string_of_type t2)
    | (Type.TL _, Type.TP _) ->
        sprintf
          "we expected a list type (something like \"%s\"), and all we got \
					 was this lousy primitive type: %s"
          (string_of_type t1) (string_of_type t2)
    | _ -> "unknown"
  in "Typing failed because " ^ s
  
(*** Unification ***)
let mgu env relax t1 t2 =
  let rec mgu t1 t2 =
    match (t1, t2) with
    | (Type.TP p1, Type.TP p2) ->
        if
          (p1 = p2) ||
            (relax &&
               (((p1 = Type.Int) && (p2 = Type.Float)) ||
                  ((p1 = Type.Float) && (p2 = Type.Int))))
        then substitution_empty
        else raise (Not_unifiable (0, env, t1, t2))
    | (t, Type.TV i) | (Type.TV i, t) ->
        substitution_add substitution_empty i t
    | (Type.TA (t11, _, t12), Type.TA (t21, _, t22)) ->
        let s1 = mgu t11 t21 in
        let s2 = mgu t12 t22 in substitution_compose s1 s2
    | (Type.TL (_, t1), Type.TL (_, t2)) | (Type.TR t1, Type.TR t2) ->
        mgu t1 t2
    | (Type.Tpair (t11, t12), Type.Tpair (t21, t22)) ->
        let s1 = mgu t11 t21 in
        let s2 = mgu t12 t22 in substitution_compose s1 s2
    | _ -> raise (Not_unifiable (0, env, t1, t2))
  in mgu t1 t2
  
