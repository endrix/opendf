/*****************************************************************************/
/* CAL2C                                                                     */
/* Copyright (c) 2007-2008, IETR/INSA of Rennes.                             */
/* All rights reserved.                                                      */
/*                                                                           */
/* This software is governed by the CeCILL-B license under French law and    */
/* abiding by the rules of distribution of free software. You can  use,      */
/* modify and/ or redistribute the software under the terms of the CeCILL-B  */
/* license as circulated by CEA, CNRS and INRIA at the following URL         */
/* "http://www.cecill.info".                                                 */
/*                                                                           */
/* Matthieu WIPLIEZ <Matthieu.Wipliez@insa-rennes.fr                         */
/*****************************************************************************/

#ifdef _WIN32
#include <rpc.h>
#else
#include <uuid/uuid.h>
#endif
#include <caml/alloc.h>
#include <caml/memory.h>

value uuid_stubs_generate(value unit) {
  CAMLparam1(unit);
  value res;
  uuid_t uuid;

  // Generate a UUID in uuid, and transforms it to a string in sz_uuid.
#ifdef _WIN32
  unsigned char *sz_uuid;

  UuidCreate(&uuid);
  UuidToString(&uuid, &sz_uuid);
#else
  char sz_uuid[37];
  
  uuid_generate(uuid);
  uuid_unparse_upper(uuid, sz_uuid);
#endif

  // copy the string to res
  res = caml_copy_string(sz_uuid);

#ifdef _WIN32
  // Free the uuid string
  RpcStringFree(&sz_uuid);
#endif

  CAMLreturn(res);
}
