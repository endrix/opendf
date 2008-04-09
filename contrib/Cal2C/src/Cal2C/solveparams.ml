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
  
module E = Exception.Make(struct let module_name = "Solveparams"
                                    end)
  
open E
  
(** [check_parameters parent name parameters params] checks that the
 actor/network named [name] is instantiated with the same parameters [params]
 that are declared in its [parameters]. If not, an error is signaled, which
 indicates in which network (called [parent]) the error occurs. *)
let check_parameters parent name parameters params =
  let parset_decl =
    List.fold_left (fun set decl -> SS.add decl.Calast.d_name set) SS.empty
      parameters in
  let parset_inst =
    List.fold_left (fun set (pn, _) -> SS.add pn set) SS.empty params in
  let diff = SS.diff parset_decl parset_inst
  in
    if SS.is_empty diff
    then
      (let diff = SS.diff parset_inst parset_decl
       in
         if SS.is_empty diff
         then ()
         else
           failwith
             (Printf.sprintf
                "network \"%s\": \"%s\" is instantiated with too many \
								 parameters. The following parameters appear in the \
								 instantiation and not in the declaration: %s"
                parent name (String.concat ", " (SS.elements diff))))
    else
      failwith
        (Printf.sprintf
           "network \"%s\": \"%s\" is instantiated with too few parameters. \
					  The following parameters appear in the declaration and not in \
						the instantiation: %s"
           parent name (String.concat ", " (SS.elements diff)))
  
(** [update_parameters parent name parameters decls fixed variables]
 sets the values of the declaration list [parameters] of the (network or
 actor) called [name], whose parent name is [parent], of every parameter
 that has a [fixed] value. Their value can be found in the [decls] string map.
 Other parameters are supposedly [variables], and the function checks that it
 is so. If not, there is a missing parameter, and cal2c stops. *)
let update_parameters parent name parameters decls fixed variables =
  List.fold_left
    (fun fixed decl ->
       if SM.mem decl.Calast.d_name fixed
       then
         (try
            let v = SM.find decl.Calast.d_name fixed in
            let v =
              try (new Visitconstfold.constFoldVisitor decls)#visitExpr v
              with
              | _ ->
                  failwith
                    (Printf.sprintf
                       "network \"%s\": instanciation of \"%s\": \
														   could not solve. Missing parameter?"
                       parent name) in
            let () = decl.Calast.d_value <- Some v
            in SM.add decl.Calast.d_name v fixed
          with
          | Not_found ->
              failwith
                (Printf.sprintf
                   "network \"%s\": gginstanciation of %s: \
														   could not solve %s"
                   parent name decl.Calast.d_name))
       else
         if SM.mem decl.Calast.d_name variables
         then fixed
         else
           failwith
             (Printf.sprintf
                "network \"%s\": %s is not instanciated with the parameter \
								\"%s\". Either remove this parameter from the declaration, \
								or add it at the instantiation."
                parent name decl.Calast.d_name))
    fixed parameters
  
(** [decls_of_network network] returns a string map that associates the
 parameters and local declarations of [network] with their (Calast.expr)
 value. It is assumed at this point that a parameter or local with no value
 is a variable parameter. *)
let decls_of_network network =
  let decls =
    List.fold_left
      (fun decls decl ->
         match decl.Calast.d_value with
         | None -> decls
         | Some e -> SM.add decl.Calast.d_name e decls)
      SM.empty network.Calast.n_parameters
  in
    List.fold_left
      (fun decls decl ->
         match decl.Calast.d_value with
         | None -> decls
         | Some e -> SM.add decl.Calast.d_name e decls)
      decls network.Calast.n_locals
  
(** [propagate_parameters instances network] propagates the values of
 [network] parameters to its children. It starts by getting the
 parameters and locals of [network]. For each entity of the network, we
 get the fixed and variables associated with it (using the [instances] table),
 check parameters and update them. Finally, if the entity is itself a network,
 we recursively apply this treatment to its children. *)
let rec propagate_parameters instances network =
  let decls = decls_of_network network
  in
    List.fold_left
      (fun instances entity ->
         let (name, parameters) =
           match entity.Calast.e_child with
           | Calast.Network network ->
               ((network.Calast.n_name), (network.Calast.n_parameters))
           | Calast.Actor actor ->
               ((actor.Calast.ac_name), (actor.Calast.ac_parameters)) in
         let (child, (fixed, variables)) = SM.find name instances
         in
           match entity.Calast.e_expr with
           | Calast.DirectInst (_, params) ->
               (check_parameters network.Calast.n_name name parameters
                  (List.map
                     (fun decl ->
                        match decl.Calast.d_value with
                        | None -> failwith "parameter has no value"
                        | Some v -> ((decl.Calast.d_name), v))
                     params);
                let fixed =
                  update_parameters network.Calast.n_name name parameters
                    decls fixed variables in
                let instances =
                  SM.add name (child, (fixed, variables)) instances
                in
                  (match entity.Calast.e_child with
                   | Calast.Network network ->
                       propagate_parameters instances network
                   | _ -> instances))
           | _ ->
               failwith
                 "visitEntity: conditional instantiation not supported")
      instances network.Calast.n_entities
  
(** [update_instances parent name params instances] updates the instances
 map with fixed and variables parameters associated with the instantiation
 of [name]. If there already is a binding, the function iterates over the
 fixed parameters to verify their value is the same as the value in the
 [params] associated with this instantiation. If it is, they are still fixed,
 otherwise they become variable parameters.
 When there is no existing binding, we first retrieve the parameters of
 the parent network (whose name is [parent]). The instantiation parameters
 inherit the fixed/variable status of their parent thanks to a simple
 [List.filter]. *)
let update_instances parent child name (params : (string * Calast.expr) list)
                     instances =
  let (child, (fixed, variables)) =
    if SM.mem name instances
    then
      (let (child, (fixed, variables)) = SM.find name instances in
       let (fixed, variables) =
         SM.fold
           (fun pn pv (fixed, variables) ->
              let param_value = List.assoc pn params
              in
                if pv = param_value
                then ((SM.add pn pv fixed), variables)
                else (fixed, (SM.add pn pv variables)))
           fixed (SM.empty, variables)
       in (child, (fixed, variables)))
    else
      (let variables =
         try snd (snd (SM.find parent instances))
         with | Not_found -> SM.empty in
       let fixed =
         List.fold_left
           (fun fixed (pn, pv) ->
              if SM.mem pn variables then fixed else SM.add pn pv fixed)
           SM.empty params
       in (child, (fixed, variables)))
  in SM.add name (child, (fixed, variables)) instances
  
(** [update_actor_parameters actor instances] updates the values of the field
 actor.ac_parameters with None for variable parameters, and Some thing for
 fixed parameters. *)
let update_actor_parameters actor instances =
  let (_, (fixed, variables)) = SM.find actor.Calast.ac_name instances
  in
    (List.iter
       (fun decl ->
          let dval =
            try Some (SM.find decl.Calast.d_name fixed)
            with
            | Not_found ->
                if SM.mem decl.Calast.d_name variables
                then None
                else
                  (Printf.fprintf stderr
                     "Warning: actor %s: value of %s not found\n%!"
                     actor.Calast.ac_name decl.Calast.d_name;
                   None)
          in decl.Calast.d_value <- dval)
       actor.Calast.ac_parameters;
     instances)
  
(** [categorize_parameters instances network] separates parameters into fixed
 ones and variables ones, for each actor/network. Fixed parameters have a
 value which is constant among all instanciations, and variable parameters
 do not. This function calls update_instances on each instantiation in the
 current [network]. It then either updates the parameters values of actor
 instantiations, or recursively calls itself on network instantiations. *)
let rec categorize_parameters instances network =
  let instances =
    List.fold_left
      (fun instances entity ->
         match entity.Calast.e_expr with
         | Calast.DirectInst (name, params) ->
             update_instances network.Calast.n_name entity.Calast.e_child
               name
               (List.map
                  (fun decl ->
                     match decl.Calast.d_value with
                     | None -> failwith "parameter has no value"
                     | Some v -> ((decl.Calast.d_name), v))
                  params)
               instances
         | _ ->
             failwith
               "solve_parameters: conditional instantiation not supported")
      instances network.Calast.n_entities
  in
    List.fold_left
      (fun instances entity ->
         match entity.Calast.e_child with
         | Calast.Actor actor -> update_actor_parameters actor instances
         | Calast.Network network -> categorize_parameters instances network)
      instances network.Calast.n_entities
  
