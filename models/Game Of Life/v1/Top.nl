
network Top (w = 20, h = 20) ==> :

entities 
	gol = GoL(w = w, h = h, init = a, nSteps = 1);
	m = Mapper();
	d = Display(title = "Game of Life", width = w, height = h, autoUpdate = 1);
	
structure
	gol.Display --> m.In;
	m.X --> d.X;
	m.Y --> d.Y;
	m.R --> d.R;
	m.G --> d.G;
	m.B --> d.B;

var
	a = randomize(w, h);
	
	function randomize (w, h) :
		[
			[randomInt(2) : for j in 1..w] :
			for i in 1..h
		]
	end

end	