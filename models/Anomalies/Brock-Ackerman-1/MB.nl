
network MA () X ==> Z :

entities

	consinv = ConsInv();
	fanout1 = Fanout();
	fanout2 = Fanout();
	merge = Merge();
	wait = Wait();
	cons = Cons();
	
structure

	X --> consinv.S;

	consinv.H --> fanout1.A;
	consinv.T --> merge.B;
	
	fanout1.X --> wait.S;
	fanout1.Y --> merge.A;
	
	merge.X --> fanout2.A;
	
	fanout2.X --> wait.W;
	fanout2.Y --> cons.T;
	
	wait.R --> cons.H;
	
	cons.R --> Z;
end
	