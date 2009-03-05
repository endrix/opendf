divert(`-1')

# This file defines macro translation for TLM implementation.

# __AVAILABLE__($1): the number of tokens available on port $1.
define(`__AVAILABLE__', `$1->used()')

# __EVENT__($1): returns an event when data arrives on port $1.
define(`__EVENT__', `$1->ok_to_get()')

# __GET__($1): takes the first element of port $1, and returns it.
define(`__GET__', `$1->get()')

# __INCLUDE__: include directives
define(`__INCLUDE__', `#include <systemc.h>
#include <tlm.h>')

# __OR__($1,$2): to __WAIT__ on event $1 or event $2
define(`__OR__', `$1 | $2')

# __PEEK__($1): returns the value of the first element on port $1, but without removing it.
define(`__PEEK__', `$1->peek()')

# __PROCESS_BEGIN__($1): actor process main function beginning.
define(`__PROCESS_BEGIN__', `void $1::process() {')

# __PROCESS_END__: actor process main function ending.
define(`__PROCESS_END__', `}')

# __PUT__($1, $2): writes to the port $1 an element $2.
define(`__PUT__', `$1->put($2)')

# __WAIT__($1, $2, ... $n): wait events on the ports $1 ... $n
define(`__WAIT__', `wait($@)')

divert`'dnl
