FIRv2

An FIR filter that is built from simple components (constant generator, adder, 
multiplier, delay), and whose structure depends on the length of the list of
coefficients. This example demonstrates a simple parametric structure.

Notes on individual components:

FIR.nl
	Demonstrates the creation of parametric network structures. Note that the 
	number of actors created in the entities section depends on the parameter
	taps (specifically, on its length). The structure section then contains a
	loop that creates the connections between the instantiated actors, including
	handling of the boundary conditions (the if..then..else construct).