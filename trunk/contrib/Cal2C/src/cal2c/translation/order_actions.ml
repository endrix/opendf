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
  
module E =
  Cal2c_util.Exception.Make(struct let module_name = "Order_actions"
                                      end)
  
open E
  
module V =
  struct
    type t = string
    
    let equal (a : string) (b : string) = a = b
      
    let hash (s : string) = Hashtbl.hash s
      
    let compare = String.compare
      
  end
  
module G = Graph.Persistent.Digraph.Concrete(V)
  
module T = Graph.Topological.Make(G)
  
module C = Graph.Components.Make(G)

(** [link_subgraphs graph] takes the graph created from the priority list, and
 returned a new graph modified as follows: for each vertex x.y, link
 predecessors of x to x.y and link x.y to successors of x. Warning: x may not
 be concerned by priorities (especially in the case of FSM), hence the test
 "mem_vertex". *)
let link_subgraphs graph =
  G.fold_vertex
    (fun tag graph ->
       if String.contains tag '.'
       then
         (let index = String.rindex tag '.' in
          let base = String.sub tag 0 index
          in
            if G.mem_vertex graph base
            then
              (let graph =
                 G.fold_pred (fun pred graph -> G.add_edge graph pred tag)
                   graph base graph in
               let graph =
                 G.fold_succ (fun succ graph -> G.add_edge graph tag succ)
                   graph base graph
               in graph)
            else graph)
       else graph)
    graph graph

let add_tag_and_children actions graph previous tag =
  let re_tag = Str.regexp (tag ^ "\\..*$") in
  let children =
    List.filter
      (fun action -> Str.string_match re_tag action.Calast.a_name 0) actions in
  let children = List.map (fun action -> action.Calast.a_name) children
  in
    List.fold_left
      (fun (tag, graph) action -> (tag, (G.add_edge graph previous action)))
      (tag, graph) (tag :: children)
  
(** [sort_by_priority priorities action_map] creates a graph from [priorities]
 as follows: a > b > ... > z becomes a -> b -> ... -> z. This graph is often
 made up of unconnected components, and therefore needs to be modified. This
 is done by calling [link_subgraphs]. After that, we simply iterate through
 the graph using a topological order to get the order in which actions should
 be scheduled. The action map [action_map] is used to get the action
 associated with the tag. [None] means the tag represents a set of actions. *)
let sort_by_priority priorities actions =
  let graph =
    List.fold_left
      (fun graph priority ->
         match priority with
         | [] -> failwith "Empty priority list"
         | h :: t ->
             let (_last_tag, graph) =
               List.fold_left
                 (fun (previous_tag, graph) tag ->
                    add_tag_and_children actions graph previous_tag tag)
                 (h, graph) t
             in graph)
      G.empty priorities in
  let graph = link_subgraphs graph in
  let prioritized_actions =
    T.fold
      (fun tag prioritized_actions ->
         try
           (List.find (fun { Calast.a_name = aname } -> aname = tag) actions) ::
             prioritized_actions
         with | Not_found -> prioritized_actions)
      graph []
  in List.rev prioritized_actions
  
(** [order_actions actions priorities] orders actions according to the
 following criteria: untagged actions come first, then actions not
 concerned by priorities, and finally other actions sorted by priorities. *)
let order actions priorities =
  let (untagged, tagged) =
    List.partition
      (fun { Calast.a_name = name } ->
         ((String.length name) >= 16) &&
           ((String.sub name 0 16) = "untagged_action_"))
      actions in
  let untagged =
    List.sort (fun a1 a2 -> String.compare a1.Calast.a_name a2.Calast.a_name)
      untagged in
  let sorted = sort_by_priority priorities actions in
  let actions_prioritized =
    List.fold_left
      (fun action_set action_names ->
         List.fold_left
           (fun action_set action_name -> SS.add action_name action_set)
           action_set action_names)
      SS.empty priorities in
  let actions_prioritized =
    List.fold_left
      (fun action_set action -> SS.add action.Calast.a_name action_set)
      actions_prioritized sorted in
  let unprioritized =
    List.filter
      (fun { Calast.a_name = name } -> not (SS.mem name actions_prioritized))
      tagged
  in untagged @ (unprioritized @ sorted)
  
class orderActionsVisitor =
  object inherit Astvisit.nopVisitor
           
    method visitActor =
      fun actor ->
        let actions =
          try order actor.Calast.ac_actions actor.Calast.ac_priorities
          with
          | e ->
              failwith
                (Printf.sprintf "%s: exception raised %s"
                   actor.Calast.ac_name (Printexc.to_string e))
        in { (actor) with Calast.ac_actions = actions; }
      
  end
  
