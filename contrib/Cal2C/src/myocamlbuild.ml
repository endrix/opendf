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

module C_tools_win32 = struct
  let link_C_library clib a env build =
    let clib = env clib and a = env a in
    (* List of object files in the .clib *)
    let objs = string_list_of_file clib in
    let include_dirs = Pathname.include_dirs_of (Pathname.dirname a) in
    let obj_of_o x =
      if Filename.check_suffix x ".o" && !Options.ext_obj <> "o" then
        Pathname.update_extension !Options.ext_obj x
      else x in
    let resluts = build (List.map (fun o -> List.map (fun dir -> dir / obj_of_o o) include_dirs) objs) in
    let objs = List.map begin function
      | Outcome.Good o -> o
      | Outcome.Bad exn -> raise exn
    end resluts in
    Cmd(S[A "ar"; A"rcs"; Px a; T(tags_of_pathname a++"c"++"ocamlmklib"); atomize objs]);;
end

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

         (* Internal library: uuid. *)
         ocaml_lib "Uuid/uuid";

         if Sys.os_type = "Win32" then (
           (* Using the optimized versions. *)
           Options.ocamlc := A "ocamlc.opt";
           Options.ocamlopt := A "ocamlopt.opt";

           (* Creation of a C library for Windows. *)
           rule "[Win32] ocaml C stubs: clib & (o|obj)* -> (a|lib) & (so|dll)"
             ~prods:["%(path:<**/>)lib%(libname:<*> and not <*.*>)"-.-(!Options.ext_lib);
                     "%(path:<**/>)dll%(libname:<*> and not <*.*>)"-.-(!Options.ext_dll)]
             ~dep:"%(path)lib%(libname).clib"
             ~insert:(`before "ocaml C stubs: clib & (o|obj)* -> (a|lib) & (so|dll)")
             (C_tools_win32.link_C_library
                "%(path)lib%(libname).clib" ("%(path)lib%(libname)"-.-(!Options.ext_lib)))
         );

         (* Allow use of C bindings with bytecode. *)
         flag [ "ocaml"; "link"; "byte"; "use_uuid" ] (A "-custom");

         (* When building uuid.cm{,x}a, adds dependencies. *)
         if Sys.os_type = "Win32" then
           flag [ "ocaml"; "link"; "library" ] (S [ A "Uuid/libmluuid.a"; A "-cclib"; A "-lrpcrt4" ])
         else
           flag [ "ocaml"; "link"; "library" ] (S [ A "-cclib"; A "-luuid" ]);

         (* Adds a dependency on Uuid/libmluuid.a. *)
         dep [ "ocaml"; "link"; "use_uuid" ] [ "Uuid/libmluuid.a" ];

         (* Documentation: colorize code and include CIL. *)
         flag [ "doc" ]
           (S
              [ A "-colorize-code"; A "-t"; A "CAL2C documentation"; 
                A "-I"; A "/usr/local/lib/cil" ])
     | _ -> ())

