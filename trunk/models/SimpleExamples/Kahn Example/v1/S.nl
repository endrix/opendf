

network S () ==> :

entities

	h0 = h(INIT = 0);
	h1 = h(INIT = 1);
	f = f();
	g = g(); 

structure

	h0.V --> f.U;
	h1.V --> f.V;
	f.W --> g.U;
	g.V --> h0.U;
	g.W --> h1.U;

end