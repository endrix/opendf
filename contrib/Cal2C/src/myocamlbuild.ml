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
    let system = output_of_command "uname -s" in
      if system = "CYGWIN_NT-5.1" then (
        print_endline "Found Cygwin.";
        try
          output_of_command "cygpath -w -p /"
        with Not_found ->
          Printf.fprintf stderr "The \"cygpath\" command could not be \
            executed, and the path of your Cygwin installation could not be \
            determined. Assuming C:\\Cygwin.\n";
          "C:\\Cygwin"
      ) else (
        print_endline "Assuming MinGW.";
        "C:\\MinGW"
      )
  with Not_found ->
    failwith "The uname command could not be executed. \
      Please check that it is present in your environment"

let _ =
  dispatch
    (function
     | After_rules ->
         (* External libraries: graph, cil. *)
         ocaml_lib ~extern:true "graph";
         let cil = "/usr/local/lib/cil" in
         let cil =
           if Sys.os_type = "Win32" then
             get_root () ^ cil
           else
             cil
         in
         ocaml_lib ~extern:true ~dir:cil "cil";

         (* Internal library: json. *)
         ocaml_lib "Json/json";

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

