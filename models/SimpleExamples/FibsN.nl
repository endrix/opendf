network FibsN (N) ==> Out:

entities
	init = [InitialTokens(tokens = [1]) : for i in 1 .. N];
	add = [Add() : for i in 2 .. N];

structure
	for i in 1 .. N-1 do
		init[i-1].Out --> init[i].In;
	end
	
	for i in 0 .. N-2 do
		if i = 0 then
			init[0].Out --> add[i].A;
		else
			add[i-1].Result --> add[i].A;
		end
		init[i+1].Out --> add[i].B;
	end

	add[N-2].Result --> init[0].In;
	add[N-2].Result --> Out;
end