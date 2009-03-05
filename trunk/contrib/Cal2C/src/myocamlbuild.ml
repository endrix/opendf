open Ocamlbuild_plugin

open Command

let output_of_command cmd =
	try
		let inch = Unix.open_process_in cmd in
		try
			let output = input_line inch in
			(try
				ignore (Unix.close_process_in inch)
			with Unix.Unix_error _ -> ());
			output
		with End_of_file ->
			ignore (Unix.close_process_in inch);
			raise Not_found
	with Unix.Unix_error _ -> raise Not_found

let get_root () =
	try
		output_of_command "cygpath -w -p /"
	with Not_found ->
		failwith "The \"cygpath\" command could not be executed. \
		  Are you sure Cygwin is present?"

let _ =
	dispatch
		(function
			| After_rules ->
			(* External libraries: graph, cil. *)
					ocaml_lib ~extern: true "graph";
					let cil = "/usr/local/lib/cil" in
					let cil =
						if Sys.os_type = "Win32" then
							get_root () ^ cil
						else
							cil
					in
					ocaml_lib ~extern: true ~dir: cil "cil";
					
					if Sys.os_type = "Win32" then (
						(* Using the optimized versions. *)
						Options.ocamlc := A "ocamlc.opt";
						Options.ocamlopt := A "ocamlopt.opt"
					);
					
					(* Documentation: colorize code and include CIL. *)
					flag [ "doc" ]
						(S
							[ A "-colorize-code"; A "-t"; A "CAL2C documentation";
							A "-I"; A "/usr/local/lib/cil" ])
			| _ -> ())

