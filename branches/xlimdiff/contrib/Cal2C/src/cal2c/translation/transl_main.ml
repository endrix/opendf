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
  
module E = Exception.Make(struct let module_name = "Transl_main"
                                    end)
  
open E
  
let exceptions = ref []
  
let print_main od includes network =
  let n = network.Calast.n_name in
  let filename = Filename.concat od (n ^ ".cpp")
  in
    m4 includes filename
      (fun out ->
         (Printf.fprintf out
            "include(%s)dnl
__INCLUDE__
#include \"sched_%s.h\"
#include \"libcal.h\"

int sc_main (int argc , char *argv[]) {
	sched_%s %s(\"%s\");
	sc_start();
	return 0;
}

int main(int argc, char *argv[]) {
	int res;
	char buf[2];
	libcal_init();
	res = sc_core::sc_elab_and_sim( argc, argv );
	printf(\"End of simulation! Press a key to continue\\n\");
	fgets(buf, 2, stdin);
	return res;
}
"
            "tlm.m4" n n n n;
          close_out out))
  
(** [translate debug od network] first solves parameters, retrieving all
 [instances] at the same time. It then translates code and action for each
 actor instance, before creating SystemC code at the network level. *)
let translate debug od includes network =
  let instances = Solveparams.categorize_parameters SM.empty network in
  let instances = Solveparams.propagate_parameters instances network in
  let instances =
    if debug.d_general
    then
      SM.map
        (fun (child, _) ->
           match child with
           | Calast.Network _ -> (child, [])
           | Calast.Actor actor ->
               (try
                  let (actor, globals) =
                    Transl_action.translate debug od actor
                  in ((Calast.Actor actor), globals)
                with
                | ex ->
                    (print_endline
                       ("Exception in actor \"" ^
                          (actor.Calast.ac_name ^ "\""));
                     raise ex)))
        instances
    else
      SM.map
        (fun (child, _) ->
           match child with
           | Calast.Network _ -> (child, [])
           | Calast.Actor actor ->
               (try
                  let (actor, globals) =
                    Transl_action.translate debug od actor
                  in ((Calast.Actor actor), globals)
                with
                | ex ->
                    (exceptions :=
                       (actor.Calast.ac_name ^
                          (": " ^ (Printexc.to_string ex))) ::
                         !exceptions;
                     (child, []))))
        instances
  in
    (print_endline "Printing schedule...";
     SM.iter
       (fun _ child ->
          match child with
          | (Calast.Network network, _) ->
              Schednetwork.translate od includes network instances
          | (Calast.Actor actor, globals) ->
              Schedactor.translate od includes actor globals)
       instances;
     print_main od includes network;
     Schednetwork.translate od includes network instances;
     print_endline "Schedule printed";
     Transl_makefile.create_makefile od includes network instances;
     Transl_makefile.create_visualc_solution od includes network instances;
     List.iter (Printf.fprintf stderr "Exception: %s\n%!")
       (List.rev !exceptions))
  
