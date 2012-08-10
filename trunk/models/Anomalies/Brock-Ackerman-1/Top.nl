
/**
 *  
 *
 */

network Top ==> OutA, OutB :

entities

	init = InitialTokens(tokens = [0]);

	consA = Cons();
	notA = Not();
	ma = MA();
	
	consB = Cons();
	notB = Not();	
	mb = MB();
	
structure

	init.X --> consA.H;
	consA.R --> ma.X;
	ma.Z --> notA.A;
	notA.X --> consA.T;
	ma.Z --> OutA;

	init.X --> consB.H;
	consB.R --> mb.X;
	mb.Z --> notB.A;
	notB.X --> consB.T;
	mb.Z --> OutB;
end
	