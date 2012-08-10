
network NA () A, B ==> X :


entities

	dup1 = Duplicate();
	dup2 = Duplicate();
	merge = Merge();
	a = A();

structure

	A --> dup1.A;
	B --> dup2.A;
	
	dup1.X --> merge.A;
	dup2.X --> merge.B;
	
	merge.X --> a.S;
	
	a.R --> X;
end