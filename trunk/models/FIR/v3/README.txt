FIRv3

This version of the FIR filter folds the computation for n taps onto a
smaller number of "processing units".

Notes on individual components:

FIRcell.cal
	This is a partial FIR unit that is getting reused during the computation of
	one sample.
	
FIR.nl
	Shows how the folding happens based on the list of coefficients and the
	parameter nUnits, which specifies how many FIRcell units are to be 
	instantiated. Note how local functions are defined in the var section, and 
	how they are used to compute local constants (tapSegments, nSegs). These
	are the used during the instantiation of entities in the entities section,
	and also in the structure section, where these entities are connected into
	a graph structure.
	