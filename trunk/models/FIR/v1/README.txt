
FIRv1

The basic setup of a FIR filter, used as a golden reference in the other 
examples.

Notes on individual components:

Top.nl
	All components in the top-level model (named Top) are basic actors. This 
	network demonstrates the basic use of NL for instantiating and composing 
	components.

Clock.cal
	Demonstrates a technique for input-bounding an actor network by only feeding 
	it "clocked" input, i.e. simulation time expires between inputs. If the
	rest of the model is untimed, this results in running the model to 
	quiescence for each new input. 

FIR.cal
	Showcases the use of first-class function objects (the lambda construct), 
	as well as a simple use of invariants.