
/**
 *  
 *
 */

network Top ==> OutA, OutB :

entities

	init = InitialTokens(tokens = [0, 0]);

	notA = Not();
	na = NA();

	notB = Not();
	nb = NB();
	
structure

	init.X --> na.A;
	na.X --> notA.A;
	notA.X --> na.B;
	na.X --> OutA;

	init.X --> nb.A;
	nb.X --> notB.A;
	notB.X --> nb.B;
	nb.X --> OutB;

end
	