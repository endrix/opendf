
network GoL (w, h, init) ==> :

entities

	a = [
			[Cell(init = init[i][j]) : for j in 0 .. w-1]
			: for i in 0 .. h-1
		];
		
	edgeTop = [Edge() : for i in 0 .. w-1];
	edgeBottom = [Edge() : for i in 0 .. w-1];
	edgeLeft = [Edge() : for i in 0 .. h-1];
	edgeRight = [Edge() : for i in 0 .. h-1];
	
	cornerNW = Edge();
	cornerNE = Edge();
	cornerSW = Edge();
	cornerSE = Edge();

structure

	foreach j in 1..h-2, foreach i in 1..w-2 do
		a[i][j].Out --> a[i-1][j-1].SE;
		a[i][j].Out --> a[i-1][j].S;
		a[i][j].Out --> a[i-1][j+1].SW;
		a[i][j].Out --> a[i][j-1].E;
		a[i][j].Out --> a[i][j+1].W;
		a[i][j].Out --> a[i+1][j-1].NE;
		a[i][j].Out --> a[i+1][j].N;
		a[i][j].Out --> a[i+1][j+1].NW;
	end
	
	foreach i in 0 .. w-1 do
		a[0][i].Out --> edgeTop[i].In;
		edgeTop[i].Out --> a[0][i].N;
		a[h-1][i].Out --> edgeBottom[i].In;
		edgeBottom[i].Out --> a[h-1][i].S;
	end
		
	foreach i in 0 .. h-1 do
		a[i][0].Out --> edgeLeft[i].In;
		edgeLeft[i].Out --> a[i][0].W;
		a[i][w-1].Out --> edgeRight[i].In;
		edgeRight[i].Out --> a[i][w-1].S;
	end
	
	cornerNW.Out --> a[0][0].NW;
	a[0][0].Out --> cornerNW.In;
	
	cornerNE.Out --> a[0][w-1].NE;
	a[0][w-1].Out --> cornerNE.In;
	
	cornerSW.Out --> a[h-1][0].SW;
	a[h-1][0].Out --> cornerSW.In;
	
	cornerSE.Out --> a[h-1][w-1].SE;
	a[h-1][w-1].Out --> cornerSE.In;
	
end


