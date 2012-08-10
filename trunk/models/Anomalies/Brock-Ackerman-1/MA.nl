
network MA () X ==> Z :

entities

	consinv = ConsInv();
	fanout = Fanout();
	merge = Merge();
	cons = Cons();
	
structure

	X --> consinv.S;

	consinv.H --> fanout.A;
	consinv.T --> merge.B;
	
	fanout.X --> cons.H;
	fanout.Y --> merge.A;
	
	merge.X --> cons.T;
	
	cons.R --> Z;
end
	