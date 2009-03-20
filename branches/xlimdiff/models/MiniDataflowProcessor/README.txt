
The MiniDataflowProcessor model illustrates the use of dataflow for building 
a simple processor.

MDP.nl is the basic processor, which needs to be connected to RAM and ROM memory
to be able to function.

CPU.nl composes an MDP with the corresponding memory components, given a program
(which will be placed into ROM), and the size and (some) initial content of the 
RAM.

TopFibonacci.nl instantiates a CPU with a simple program for computing a few
Fibonacci numbers.

