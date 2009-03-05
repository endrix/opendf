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

open Calast
  
open Cal2c_util
  
(* -> is RIGHT-associative *)
let initial_environment =
  let types =
    [ (":=",
       ((IS.singleton 0),
        (Type.TA (Type.TV 0, TypeSchemeSet.empty,
           Type.TA (Type.TV 0, TypeSchemeSet.empty, Type.TP Type.Unit)))),
       None);
      ("::",
       ((IS.singleton 0),
        (Type.TA (Type.TV 0, TypeSchemeSet.empty,
           Type.TA (Type.TL (None, Type.TV 0), TypeSchemeSet.empty,
             Type.TL (None, Type.TV 0))))),
       None);
      ("[]", ((IS.singleton 0), (Type.TL (None, Type.TV 0))), None);
      ("()", (IS.empty, (Type.TP Type.Unit)), None);
      (";",
       (IS.empty,
        (Type.TA (Type.TP Type.Unit, TypeSchemeSet.empty,
           Type.TA (Type.TP Type.Unit, TypeSchemeSet.empty,
             Type.TP Type.Unit)))),
       None);
      ("abs",
       ((IS.singleton 0),
        (Type.TA (Type.TV 0, TypeSchemeSet.empty, Type.TV 0))),
       None);
      ("bitand",
       (IS.empty,
        (Type.TA (Type.TP Type.Int, TypeSchemeSet.empty,
           Type.TA (Type.TP Type.Int, TypeSchemeSet.empty, Type.TP Type.Int)))),
       None);
      ("bitnot",
       (IS.empty,
        (Type.TA (Type.TP Type.Int, TypeSchemeSet.empty, Type.TP Type.Int))),
       None);
      ("bitor",
       (IS.empty,
        (Type.TA (Type.TP Type.Int, TypeSchemeSet.empty,
           Type.TA (Type.TP Type.Int, TypeSchemeSet.empty, Type.TP Type.Int)))),
       None);
      ("bitxor",
       (IS.empty,
        (Type.TA (Type.TP Type.Int, TypeSchemeSet.empty,
           Type.TA (Type.TP Type.Int, TypeSchemeSet.empty, Type.TP Type.Int)))),
       None);
      ("currentSystemTime",
       (IS.empty,
        (Type.TA (Type.TP Type.Unit, TypeSchemeSet.empty, Type.TP Type.Int))),
       (Some
          (Calast.Function
             ([ {
                  Calast.d_name = "()";
                  d_type = (IS.empty, (Calast.Type.TP Calast.Type.Unit));
                  d_value = None;
                } ],
             [], Calast.Literal (Calast.Integer 0)))));
      ("if",
       ((IS.singleton 0),
        (Type.TA (Type.TP Type.Bool, TypeSchemeSet.empty,
           Type.TA (Type.TV 0, TypeSchemeSet.empty,
             Type.TA (Type.TV 0, TypeSchemeSet.empty, Type.TV 0))))),
       None);
      ("Integers",
       (IS.empty,
        (Type.TA (Type.TP Type.Int, TypeSchemeSet.empty,
           Type.TA (Type.TP Type.Int, TypeSchemeSet.empty,
             Type.TL (None, Type.TP Type.Int))))),
       None);
      ("JFrame",
       ((IS.singleton 0),
        (Type.TA (Type.TP Type.String, TypeSchemeSet.empty, Type.TV 0))),
       (Some
          (Calast.Function
             ([ {
                  Calast.d_name = "title";
                  d_type = (IS.empty, (Calast.Type.TP Calast.Type.String));
                  d_value = None;
                } ],
             [], Calast.Unit))));
      ("lshift",
       (IS.empty,
        (Type.TA (Type.TP Type.Int, TypeSchemeSet.empty,
           Type.TA (Type.TP Type.Int, TypeSchemeSet.empty, Type.TP Type.Int)))),
       None);
      ("List_flatten",
       ((IS.singleton 0),
        (Type.TA (Type.TL (None, Type.TL (None, Type.TV 0)), TypeSchemeSet.
           empty, Type.TL (None, Type.TV 0)))),
       None);
      ("List_map",
       ((IS.add 1 (IS.singleton 0)),
        (Type.TA (Type.TA (Type.TV 0, TypeSchemeSet.empty, Type.TV 1),
           TypeSchemeSet.empty,
           Type.TA (Type.TL (None, Type.TV 0), TypeSchemeSet.empty,
             Type.TL (None, Type.TV 1))))),
       None);
      ("List_nth",
       ((IS.singleton 0),
        (Type.TA (Type.TL (None, Type.TV 0), TypeSchemeSet.empty,
           Type.TA (Type.TP Type.Int, TypeSchemeSet.empty, Type.TV 0)))),
       None);
      ("Math_ceil",
       (IS.empty,
        (Type.TA (Type.TP Type.Float, TypeSchemeSet.empty,
           Type.TP Type.Float))),
       None);
      ("Math_log",
       (IS.empty,
        (Type.TA (Type.TP Type.Float, TypeSchemeSet.empty,
           Type.TP Type.Float))),
       None);
      ("Math_round",
       (IS.empty,
        (Type.TA (Type.TP Type.Float, TypeSchemeSet.empty,
           Type.TP Type.Float))),
       None);
      ("openFile",
       (IS.empty,
        (Type.TA (Type.TP Type.String, TypeSchemeSet.empty, Type.TP Type.Int))),
       (Some
          (Calast.Function
             ([ {
                  Calast.d_name = "file_name";
                  d_type = (IS.empty, (Calast.Type.TP Calast.Type.String));
                  d_value = None;
                } ],
             [], Calast.Literal (Calast.Integer 0)))));
      ("picture_displayImage",
       (IS.empty,
        (Type.TA (Type.TP Type.Unit, TypeSchemeSet.empty, Type.TP Type.Unit))),
       (Some
          (Calast.Function
             ([ {
                  Calast.d_name = "()";
                  d_type = (IS.empty, (Calast.Type.TP Calast.Type.Unit));
                  d_value = None;
                } ],
             [], Calast.Unit))));
      ("picture_setPixel",
       (IS.empty,
        (Type.TA (Type.TP Type.Int, TypeSchemeSet.empty,
           Type.TA (Type.TP Type.Int, TypeSchemeSet.empty,
             Type.TA (Type.TP Type.Int, TypeSchemeSet.empty,
               Type.TA (Type.TP Type.Int, TypeSchemeSet.empty,
                 Type.TA (Type.TP Type.Int, TypeSchemeSet.empty,
                   Type.TP Type.Unit))))))),
       (Some
          (Calast.Function
             ([ {
                  Calast.d_name = "x";
                  d_type = (IS.empty, (Calast.Type.TP Calast.Type.Int));
                  d_value = None;
                };
                {
                  Calast.d_name = "y";
                  d_type = (IS.empty, (Calast.Type.TP Calast.Type.Int));
                  d_value = None;
                };
                {
                  Calast.d_name = "r";
                  d_type = (IS.empty, (Calast.Type.TP Calast.Type.Int));
                  d_value = None;
                };
                {
                  Calast.d_name = "g";
                  d_type = (IS.empty, (Calast.Type.TP Calast.Type.Int));
                  d_value = None;
                };
                {
                  Calast.d_name = "b";
                  d_type = (IS.empty, (Calast.Type.TP Calast.Type.Int));
                  d_value = None;
                } ],
             [], Calast.Unit))));
      ("Picture",
       ((IS.singleton 0),
        (Type.TA (Type.TP Type.Int, TypeSchemeSet.empty,
           Type.TA (Type.TP Type.Int, TypeSchemeSet.empty, Type.TV 0)))),
       (Some
          (Calast.Function
             ([ {
                  Calast.d_name = "width";
                  d_type = (IS.empty, (Calast.Type.TP Calast.Type.Int));
                  d_value = None;
                };
                {
                  Calast.d_name = "height";
                  d_type = (IS.empty, (Calast.Type.TP Calast.Type.Int));
                  d_value = None;
                } ],
             [], Calast.Unit))));
      ("println",
       (IS.empty,
        (Type.TA (Type.TP Type.String, TypeSchemeSet.empty,
           Type.TP Type.Unit))),
       None);
      ("readByte",
       (IS.empty,
        (Type.TA (Type.TP Type.Int, TypeSchemeSet.empty, Type.TP Type.Int))),
       (Some
          (Calast.Function
             ([ {
                  Calast.d_name = "fd";
                  d_type = (IS.empty, (Calast.Type.TP Calast.Type.Int));
                  d_value = None;
                } ],
             [], Calast.Literal (Calast.Integer 0)))));
      ("rshift",
       (IS.empty,
        (Type.TA (Type.TP Type.Int, TypeSchemeSet.empty,
           Type.TA (Type.TP Type.Int, TypeSchemeSet.empty, Type.TP Type.Int)))),
       None);
      ("toString",
       ((IS.singleton 0),
        (Type.TA (Type.TV 0, TypeSchemeSet.empty, Type.TP Type.String))),
       None);
      ("while",
       (IS.empty,
        (Type.TA (Type.TP Type.Bool, TypeSchemeSet.empty,
           Type.TA (Type.TP Type.Unit, TypeSchemeSet.empty,
             Type.TP Type.Unit)))),
       None) ]
  in
    List.fold_left
      (fun m (x, t, v) ->
         SM.add x { Calast.d_name = x; d_type = t; d_value = v; } m)
      SM.empty types
  
