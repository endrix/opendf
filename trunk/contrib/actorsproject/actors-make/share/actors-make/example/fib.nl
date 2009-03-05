network fib () ==> :

entities
	d1   = SingleDelay(initialToken=1);
	d2   = SingleDelay(initialToken=0);
	add  = AddUntilOverflow();
	sink = art_Sink_txt();

structure
	add.Sum --> d1.In;
	d1.Out -->  d2.In;
	d1.Out -->  add.X;
	d2.Out -->  add.Y;
	d2.Out  --> sink.In;
end
