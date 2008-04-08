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
  
open Printf
  
let print_makefile out includes network instances =
  let inc_dir =
    List.find
      (fun inc_dir ->
         let filename = Filename.concat inc_dir "libcal.c"
         in Sys.file_exists filename)
      includes in
  let libcal_o = Filename.concat inc_dir "libcal.o" in
  let files =
    SM.fold
      (fun name child files ->
         match child with
         | (Calast.Actor _, _) ->
             (sprintf "%s.o sched_%s.o" name name) :: files
         | (Calast.Network _, _) -> files)
      instances [] in
  let files = List.rev files in
  let files = String.concat " " files in
  let name = network.Calast.n_name
  in
    fprintf out
      "%s: %s.o %s %s
\t$(CXX) -o $@ $^ $(LDFLAGS) `sdl-config --libs`
" name
      name libcal_o files
  
let create_makefile od includes network instances =
  let filename = Filename.concat od "Makefile" in
  let out = open_out filename
  in
    (fprintf out
       "LD = g++
CFLAGS = -Wall `sdl-config --cflags`
CXXFLAGS = -Wall -I /usr/local/systemc-2.2/include %s
LDFLAGS = -L /usr/local/systemc-2.2/lib-linux -lsystemc

"
       (String.concat " " (List.map (fun inc -> "-I " ^ inc) includes));
     print_makefile out includes network instances;
     fprintf out "
clean:
\trm -f *.o
";
     close_out out)
  
(** [print_sln out name guid] prints the contents of a MSVC solution to the
 output channel [out]. [name] is the name of the project (main network), and
 [guid] the UUID assigned to it. It is noteworthy that the solution itself
 does not have an UUID, but uses SystemC's instead (because it depends on
 SystemC). *)
let print_sln out includes name guid_project =
  let include_systemc =
    find_file includes "msvc71\\SystemC\\SystemC.vcproj" in
  let path_sysc =
    Filename.concat include_systemc "msvc71\\SystemC\\SystemC.vcproj" in
  let guid_sysc_sln = "8BC9CEB8-8B4A-11D0-8D11-00A0C91BC942" in
  let guid_sysc_project = "86DF4B8C-CF94-4EA8-B529-78997F0F30A7"
  in
    fprintf out
      "Microsoft Visual Studio Solution File, Format Version 9.00
# Visual Studio 2005
Project(\"{%s}\") = \"%s\", \"%s.vcproj\", \"{%s}\"
\tProjectSection(ProjectDependencies) = postProject
\t\t{%s} = {%s}
\tEndProjectSection
EndProject
Project(\"{%s}\") = \"SystemC\", \"%s\", \"{%s}\"
EndProject
Global
\tGlobalSection(SolutionConfigurationPlatforms) = preSolution
\t\tDebug|Win32 = Debug|Win32
\t\tRelease|Win32 = Release|Win32
\tEndGlobalSection
\tGlobalSection(ProjectConfigurationPlatforms) = postSolution
\t\t{%s}.Debug|Win32.ActiveCfg = Debug|Win32
\t\t{%s}.Debug|Win32.Build.0 = Debug|Win32
\t\t{%s}.Release|Win32.ActiveCfg = Release|Win32
\t\t{%s}.Release|Win32.Build.0 = Release|Win32
\t\t{%s}.Debug|Win32.ActiveCfg = Debug|Win32
\t\t{%s}.Debug|Win32.Build.0 = Debug|Win32
\t\t{%s}.Release|Win32.ActiveCfg = Release|Win32
\t\t{%s}.Release|Win32.Build.0 = Release|Win32
\tEndGlobalSection
\tGlobalSection(SolutionProperties) = preSolution
\t\tHideSolutionNode = FALSE
\tEndGlobalSection
EndGlobal\n"
      guid_sysc_sln name name guid_project guid_sysc_project
      guid_sysc_project
      guid_sysc_sln
      path_sysc guid_sysc_project guid_project guid_project guid_project guid_project
      guid_sysc_project guid_sysc_project guid_sysc_project guid_sysc_project
  
let files_of_network includes network instances =
  let main_cpp =
    sprintf
      "\t\t\t<File
\t\t\t\tRelativePath=\".\\%s.cpp\"
\t\t\t\t>
\t\t\t</File>\n"
      network.Calast.n_name in
  let main_h =
    sprintf
      "\t\t\t<File
\t\t\t\tRelativePath=\".\\sched_%s.h\"
\t\t\t\t>
\t\t\t</File>\n"
      network.Calast.n_name in
  let dir_libcal = find_file includes "libcal.c" in
  let libcal_c =
    sprintf
      "\t\t\t<File
\t\t\t\tRelativePath=\"%s\"
\t\t\t\t>
\t\t\t</File>\n"
      (Filename.concat dir_libcal "libcal.c") in
  let (header_files, source_files) =
    SM.fold
      (fun name child (header_files, source_files) ->
         match child with
         | (Calast.Actor _, _) ->
             let h_file =
               sprintf
                 "\t\t\t<File
\t\t\t\tRelativePath=\".\\sched_%s.h\"
\t\t\t\t>
\t\t\t</File>\n"
                 name in
             let c_file =
               sprintf
                 "\t\t\t<File
\t\t\t\tRelativePath=\".\\%s.c\"
\t\t\t\t>
\t\t\t</File>\n"
                 name in
             let cpp_file =
               sprintf
                 "\t\t\t<File
\t\t\t\tRelativePath=\".\\sched_%s.cpp\"
\t\t\t\t>
\t\t\t</File>\n"
                 name
             in
               ((h_file :: header_files),
                (c_file :: cpp_file :: source_files))
         | (Calast.Network _, _) ->
             let h_file =
               sprintf
                 "\t\t\t<File
\t\t\t\tRelativePath=\".\\sched_%s.h\"
\t\t\t\t>
\t\t\t</File>\n"
                 name
             in ((h_file :: header_files), source_files))
      instances ([ main_h ], [ main_cpp; libcal_c ]) in
  let header_files = String.concat "" (List.rev header_files) in
  let source_files = String.concat "" (List.rev source_files)
  in (header_files, source_files)
  
let include_directories includes =
  let include_libcal = find_file includes "libcal.h" in
  let include_sdl = find_file includes "include\\SDL.h" in
  let include_sdl = Filename.concat include_sdl "include" in
  let include_systemc = find_file includes "src\\systemc.h" in
  let include_systemc = Filename.concat include_systemc "src" in
  let include_tlm = find_file includes "include\\tlm.h" in
  let include_tlm = Filename.concat include_tlm "include"
  in
    String.concat ";"
      (List.map (fun dir -> "&quot;" ^ (dir ^ "&quot;"))
         [ include_libcal; include_sdl; include_systemc; include_tlm ])

let libraries includes =
  let lib_sdl = find_file includes "lib\\SDL.lib" in  
  String.concat " "
    (List.map (fun file -> "&quot;" ^ file ^ "&quot;")
       [ Filename.concat lib_sdl "lib\\SDL.lib";
         Filename.concat lib_sdl "lib\\SDLmain.lib" ])

(* AdditionalIncludeDirectories="..\libcal;&quot;blablabla&quot;" *)
let print_vcproj out guid includes network instances =
  let (header_files, source_files) = files_of_network includes network instances in
  let libs = libraries includes in
  let includes = include_directories includes
  in
    fprintf out
      "<?xml version=\"1.0\" encoding=\"Windows-1252\"?>
<VisualStudioProject
\tProjectType=\"Visual C++\"
\tVersion=\"8,00\"
\tName=\"%s\"
\tProjectGUID=\"{%s}\"
\tRootNamespace=\"%s\"
\tKeyword=\"Win32Proj\"
\t>
\t<Platforms>
\t\t<Platform
\t\t\tName=\"Win32\"
\t\t/>
\t</Platforms>
\t<ToolFiles>
\t</ToolFiles>
\t<Configurations>
\t\t<Configuration
\t\t\tName=\"Debug|Win32\"
\t\t\tOutputDirectory=\"$(SolutionDir)$(ConfigurationName)\"
\t\t\tIntermediateDirectory=\"$(ConfigurationName)\"
\t\t\tConfigurationType=\"1\"
\t\t\tCharacterSet=\"1\"
\t\t\t>
\t\t\t<Tool
\t\t\t\tName=\"VCPreBuildEventTool\"
\t\t\t/>
\t\t\t<Tool
\t\t\t\tName=\"VCCustomBuildTool\"
\t\t\t/>
\t\t\t<Tool
\t\t\t\tName=\"VCXMLDataGeneratorTool\"
\t\t\t/>
\t\t\t<Tool
\t\t\t\tName=\"VCWebServiceProxyGeneratorTool\"
\t\t\t/>
\t\t\t<Tool
\t\t\t\tName=\"VCMIDLTool\"
\t\t\t/>
\t\t\t<Tool
\t\t\t\tName=\"VCCLCompilerTool\"
\t\t\t\tAdditionalOptions=\"/vmg /wd4273\"
\t\t\t\tOptimization=\"0\"
\t\t\t\tAdditionalIncludeDirectories=\"%s\"
\t\t\t\tPreprocessorDefinitions=\"WIN32;_DEBUG;_CONSOLE;NOGDI;_CRT_SECURE_NO_DEPRECATE\"
\t\t\t\tMinimalRebuild=\"true\"
\t\t\t\tBasicRuntimeChecks=\"3\"
\t\t\t\tRuntimeLibrary=\"3\"
\t\t\t\tUsePrecompiledHeader=\"0\"
\t\t\t\tWarningLevel=\"3\"
\t\t\t\tDetect64BitPortabilityProblems=\"false\"
\t\t\t\tDebugInformationFormat=\"4\"
\t\t\t/>
\t\t\t<Tool
\t\t\t\tName=\"VCManagedResourceCompilerTool\"
\t\t\t/>
\t\t\t<Tool
\t\t\t\tName=\"VCResourceCompilerTool\"
\t\t\t/>
\t\t\t<Tool
\t\t\t\tName=\"VCPreLinkEventTool\"
\t\t\t/>
\t\t\t<Tool
\t\t\t\tName=\"VCLinkerTool\"
\t\t\t\tAdditionalDependencies=\"%s\"
\t\t\t\tLinkIncremental=\"2\"
\t\t\t\tGenerateDebugInformation=\"true\"
\t\t\t\tSubSystem=\"1\"
\t\t\t\tTargetMachine=\"1\"
\t\t\t/>
\t\t\t<Tool
\t\t\t\tName=\"VCALinkTool\"
\t\t\t/>
\t\t\t<Tool
\t\t\t\tName=\"VCManifestTool\"
\t\t\t/>
\t\t\t<Tool
\t\t\t\tName=\"VCXDCMakeTool\"
\t\t\t/>
\t\t\t<Tool
\t\t\t\tName=\"VCBscMakeTool\"
\t\t\t/>
\t\t\t<Tool
\t\t\t\tName=\"VCFxCopTool\"
\t\t\t/>
\t\t\t<Tool
\t\t\t\tName=\"VCAppVerifierTool\"
\t\t\t/>
\t\t\t<Tool
\t\t\t\tName=\"VCWebDeploymentTool\"
\t\t\t/>
\t\t\t<Tool
\t\t\t\tName=\"VCPostBuildEventTool\"
\t\t\t/>
\t\t</Configuration>
\t\t<Configuration
\t\t\tName=\"Release|Win32\"
\t\t\tOutputDirectory=\"$(SolutionDir)$(ConfigurationName)\"
\t\t\tIntermediateDirectory=\"$(ConfigurationName)\"
\t\t\tConfigurationType=\"1\"
\t\t\tCharacterSet=\"1\"
\t\t\tWholeProgramOptimization=\"1\"
\t\t\t>
\t\t\t<Tool
\t\t\t\tName=\"VCPreBuildEventTool\"
\t\t\t/>
\t\t\t<Tool
\t\t\t\tName=\"VCCustomBuildTool\"
\t\t\t/>
\t\t\t<Tool
\t\t\t\tName=\"VCXMLDataGeneratorTool\"
\t\t\t/>
\t\t\t<Tool
\t\t\t\tName=\"VCWebServiceProxyGeneratorTool\"
\t\t\t/>
\t\t\t<Tool
\t\t\t\tName=\"VCMIDLTool\"
\t\t\t/>
\t\t\t<Tool
\t\t\t\tName=\"VCCLCompilerTool\"
\t\t\t\tAdditionalOptions=\"/vmg /wd4273\"
\t\t\t\tAdditionalIncludeDirectories=\"%s\"
\t\t\t\tPreprocessorDefinitions=\"WIN32;NDEBUG;_CONSOLE;NOGDI;_CRT_SECURE_NO_DEPRECATE\"
\t\t\t\tRuntimeLibrary=\"2\"
\t\t\t\tUsePrecompiledHeader=\"0\"
\t\t\t\tWarningLevel=\"3\"
\t\t\t\tDetect64BitPortabilityProblems=\"false\"
\t\t\t\tDebugInformationFormat=\"3\"
\t\t\t/>
\t\t\t<Tool
\t\t\t\tName=\"VCManagedResourceCompilerTool\"
\t\t\t/>
\t\t\t<Tool
\t\t\t\tName=\"VCResourceCompilerTool\"
\t\t\t/>
\t\t\t<Tool
\t\t\t\tName=\"VCPreLinkEventTool\"
\t\t\t/>
\t\t\t<Tool
\t\t\t\tName=\"VCLinkerTool\"
\t\t\t\tAdditionalDependencies=\"%s\"
\t\t\t\tLinkIncremental=\"1\"
\t\t\t\tGenerateDebugInformation=\"true\"
\t\t\t\tSubSystem=\"1\"
\t\t\t\tOptimizeReferences=\"2\"
\t\t\t\tEnableCOMDATFolding=\"2\"
\t\t\t\tTargetMachine=\"1\"
\t\t\t/>
\t\t\t<Tool
\t\t\t\tName=\"VCALinkTool\"
\t\t\t/>
\t\t\t<Tool
\t\t\t\tName=\"VCManifestTool\"
\t\t\t/>
\t\t\t<Tool
\t\t\t\tName=\"VCXDCMakeTool\"
\t\t\t/>
\t\t\t<Tool
\t\t\t\tName=\"VCBscMakeTool\"
\t\t\t/>
\t\t\t<Tool
\t\t\t\tName=\"VCFxCopTool\"
\t\t\t/>
\t\t\t<Tool
\t\t\t\tName=\"VCAppVerifierTool\"
\t\t\t/>
\t\t\t<Tool
\t\t\t\tName=\"VCWebDeploymentTool\"
\t\t\t/>
\t\t\t<Tool
\t\t\t\tName=\"VCPostBuildEventTool\"
\t\t\t/>
\t\t</Configuration>
\t</Configurations>
\t<References>
\t</References>
\t<Files>
\t\t<Filter
\t\t\tName=\"Source Files\"
\t\t\tFilter=\"cpp;c;cc;cxx;def;odl;idl;hpj;bat;asm;asmx\"
\t\t\tUniqueIdentifier=\"{4FC737F1-C7A5-4376-A066-2A32D752A2FF}\"
\t\t\t>
%s\t\t</Filter>
\t\t<Filter
\t\t\tName=\"Header Files\"
\t\t\tFilter=\"h;hpp;hxx;hm;inl;inc;xsd\"
\t\t\tUniqueIdentifier=\"{93995380-89BD-4b04-88EB-625FBE52EBFB}\"
\t\t\t>
%s\t\t</Filter>
\t\t<Filter
\t\t\tName=\"Resource Files\"
\t\t\tFilter=\"rc;ico;cur;bmp;dlg;rc2;rct;bin;rgs;gif;jpg;jpeg;jpe;resx;tiff;tif;png;wav\"
\t\t\tUniqueIdentifier=\"{67DA6AB6-F800-4c08-8B7A-83BB121AAD01}\"
\t\t\t>
\t\t</Filter>
\t</Files>
\t<Globals>
\t</Globals>
</VisualStudioProject>\n"
      network.Calast.n_name guid network.Calast.n_name includes libs includes libs
      source_files header_files
  
let create_visualc_solution od includes network instances =
  if Sys.os_type = "Win32"
  then
    (let guid_project = "{9AC2FEFB-8204-4635-ABE7-429A33CAFAFB}" in
     let name = network.Calast.n_name in
     let filename = Filename.concat od (name ^ ".sln") in
     let out = open_out filename
     in
       (print_sln out includes name guid_project;
        close_out out;
        let filename = Filename.concat od (name ^ ".vcproj") in
        let out = open_out filename
        in
          (print_vcproj out guid_project includes network instances;
           close_out out)))
  else ()
  
