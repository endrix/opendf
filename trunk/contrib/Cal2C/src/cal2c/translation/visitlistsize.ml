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
  Cal2c_util.Exception.Make(struct let module_name = "Visitlistsize"
                                      end)
  
open E
  
(****************************************************************************)
class solveListParametersLengthVisitor =
  object (self)
    inherit Astvisit.nopVisitor as super
      
    val mutable m_gen_var = SM.empty
      
    (** [registerGeneratorDecls dl] registers the variables declared in [dl]
    in the m_gen_var table. *)
    method private registerGeneratorDecls =
      fun dl ->
        List.iter
          (fun decl ->
             match decl.Calast.d_value with
             | Some
                 (Calast.Application ((Calast.Var "Integers"),
                    ([ Calast.Literal (Calast.Integer es);
                       Calast.Literal (Calast.Integer ee)
                     ])))
                 ->
                 m_gen_var <-
                   SM.add decl.Calast.d_name
                     (Calast.Literal (Calast.Integer (ee - es))) m_gen_var
             | _ -> ())
          dl
      
    val m_unsolvedLists = SH.create 0
      
    (** Updates the list length. *)
    method private updateListLengthFromIndexer =
      fun var i ->
        if SH.mem m_unsolvedLists var
        then
          (let decl = SH.find m_unsolvedLists var in self#updateDecl decl i)
        else ()
      
    (** [updateDecl decl length] updates the length of [decl] is it has the
    list type, and either no previous length, or a previous length that was
		found to be inferior to [length]. *)
    method private updateDecl =
      fun decl length ->
        match decl.Calast.d_type with
        | (is, Calast.Type.TL (None, t)) ->
            let t = (is, (Calast.Type.TL (Some length, t)))
            in decl.Calast.d_type <- t
        | (is, Calast.Type.TR (Calast.Type.TL (None, t))) ->
            let t = (is, (Calast.Type.TR (Calast.Type.TL (Some length, t))))
            in decl.Calast.d_type <- t
        | (is, Calast.Type.TL ((Some i), t)) when i < length ->
            let t = (is, (Calast.Type.TL (Some length, t)))
            in decl.Calast.d_type <- t
        | (is, Calast.Type.TR (Calast.Type.TL ((Some i), t))) when i < length
            ->
            let t = (is, (Calast.Type.TR (Calast.Type.TL (Some length, t))))
            in decl.Calast.d_type <- t
        | _ -> ()
      
    (** We visit a list declaration, at this point only function parameters
    list do not have size information, neither do they have a value. We will
		guess it from its use in visitExpr. *)
    method visitDecl =
      fun decl ->
        match decl.Calast.d_type with
        | (_, Calast.Type.TL (None, _)) |
            (_, Calast.Type.TR (Calast.Type.TL (None, _))) ->
            (match decl.Calast.d_value with
             | None ->
                 let decl = super#visitDecl decl
                 in
                   (SH.replace m_unsolvedLists decl.Calast.d_name decl; decl)
             | _ -> super#visitDecl decl)
        | _ -> super#visitDecl decl
      
    (** We also extend lists (comprehension, generator) to see if we can guess
		some information about unsolved lists:
		  [ unsolved_list[i], for i in Integers(0, 63) ]
		So we can guess the minimum size of unsolved_list here! *)
    method visitExpr =
      fun e ->
        match e with
        | Calast.List (Calast.Generator (_, dl)) ->
            (self#registerGeneratorDecls dl; super#visitExpr e)
        | Calast.Indexer ((Calast.Var var), vi) ->
            ((try
                let vi =
                  (new Visitconstfold.constFoldVisitor m_gen_var)#visitExpr
                    vi
                in
                  match vi with
                  | Calast.Literal (Calast.Integer i) ->
                      self#updateListLengthFromIndexer var (i + 1)
                  | _ -> ()
              with | _ -> ());
             super#visitExpr e)
        | _ -> super#visitExpr e
      
  end
  
let compute_size el dl =
  let iterations =
    List.fold_left
      (fun iterations decl ->
         match decl.Calast.d_value with
         | Some
             (Calast.Application ((Calast.Var "Integers"),
                ([ Calast.Literal (Calast.Integer f);
                   Calast.Literal (Calast.Integer e)
                 ])))
             -> let loop_size = (e + 1) - f in iterations * loop_size
         | _ -> failwith "compute_size: unable to compute size")
      1 dl
  in iterations * (List.length el)
  
(****************************************************************************)
(** This class visits the Calast tree and solve the lengths of all lists. *)
class solveListLengthVisitor =
  object (self)
    inherit Astvisit.nopVisitor as super
      
    val m_functions = SH.create 10
      
    val m_solvedLists = SH.create 10
      
    val m_unsolvedLists = SH.create 10
      
    method private lengthOfList =
      fun name ->
        let decl = SH.find m_solvedLists name
        in
          match snd decl.Calast.d_type with
          | Calast.Type.TL ((Some length), _) |
              Calast.Type.TR (Calast.Type.TL ((Some length), _)) -> length
          | _ -> failwith (name ^ " not a list with a size")
      
    method private getLength =
      function
      | Calast.Application ((Calast.Var var), _) ->
          let decl =
            (try SH.find m_functions var
             with
             | Not_found ->
                 failwith
                   ("solveListLengthVisitor#getLength: \"" ^
                      (var ^ "\" is not known as a function")))
          in
            (match decl.Calast.d_value with
             | Some (Calast.Function (_, _, e)) -> self#getLength e
             | _ ->
                 failwith
                   ("solveListLengthVisitor#getLength: \"" ^
                      (var ^ "\" function has no value")))
      | Calast.List list ->
          (match list with
           | Calast.Comprehension el -> List.length el
           | Calast.Generator (el, dl) -> compute_size el dl)
      | Calast.If (_, e1, e2) ->
          let l1 = self#getLength e1 in
          let l2 = self#getLength e2
          in
            if l1 = l2
            then l1
            else
              failwith
                "solveListLengthVisitor#getLength: the list returned in \
					       the \"then\" branch does not have the same length as the \
						     list returned in the \"else\" branch"
      | Calast.Var var ->
          (try self#lengthOfList var
           with
           | Not_found ->
               failwith
                 ("solveListLengthVisitor#getLength: \"" ^
                    (var ^ "\" does not have size information")))
      | _ -> failwith "solveListLengthVisitor#getLength: no length"
      
    method private updateDecl =
      fun decl length ->
        match decl.Calast.d_type with
        | (is, Calast.Type.TL (None, t)) ->
            let t = (is, (Calast.Type.TL (Some length, t)))
            in
              (decl.Calast.d_type <- t;
               SH.remove m_unsolvedLists decl.Calast.d_name;
               SH.replace m_solvedLists decl.Calast.d_name decl)
        | (is, Calast.Type.TR (Calast.Type.TL (None, t))) ->
            let t = (is, (Calast.Type.TR (Calast.Type.TL (Some length, t))))
            in
              (decl.Calast.d_type <- t;
               SH.remove m_unsolvedLists decl.Calast.d_name;
               SH.replace m_solvedLists decl.Calast.d_name decl)
        | _ -> ()
      
    method visitActor =
      fun actor ->
        let actor = super#visitActor actor in
        let visitor = new solveListParametersLengthVisitor
        in visitor#visitActor actor
      
    (** First case: we visit a function, we use the visitDecl of super, which
    will visits parameters, locals, and the function body. After that, we
		register the function in the m_functions hash table.
		
		Second case: we visit a list declaration, wherever it may be. If it has a
		value, very good! we compute the list length, and saves the list in the
		m_solvedLists hash table. If it has not, we just call the super
		visitDecl method, and hope for the best (check visitExpr below).
		*)
    method visitDecl =
      fun decl ->
        match decl.Calast.d_type with
        | (_, Calast.Type.TA _) ->
            let decl = super#visitDecl decl
            in (SH.replace m_functions decl.Calast.d_name decl; decl)
        | (is, Calast.Type.TL (None, t)) |
            (is, Calast.Type.TR (Calast.Type.TL (None, t))) ->
            (match decl.Calast.d_value with
             | Some e ->
                 let length =
                   (try self#getLength e
                    with | ex -> (print_endline decl.Calast.d_name; raise ex)) in
                 let t = (is, (Calast.Type.TL (Some length, t)))
                 in
                   (decl.Calast.d_type <- t;
                    let decl = super#visitDecl decl
                    in
                      (SH.replace m_solvedLists decl.Calast.d_name decl;
                       decl))
             | None ->
                 let decl = super#visitDecl decl
                 in
                   (SH.replace m_unsolvedLists decl.Calast.d_name decl; decl))
        | _ -> super#visitDecl decl
      
    (** Here we extend assign statements where the left side of it is
    known as an unsolved list. If it is the case, we compute its length, and
		update the type in the declaration. *)
    method visitExpr =
      fun e ->
        match e with
        | Calast.Assign ((Calast.Var var), e2) ->
            if SH.mem m_unsolvedLists var
            then
              (let decl = SH.find m_unsolvedLists var in
               let length = self#getLength e2
               in (self#updateDecl decl length; super#visitExpr e))
            else super#visitExpr e
        | e -> super#visitExpr e
      
  end
  
