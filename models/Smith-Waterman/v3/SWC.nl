
/**
	Smith-Waterman Cell
	
	The basic computational unit of the Smith-Waterman algorithm.
*/


network SWC (match, mismatch, gap) A, B, W, NW, N ==> V :

entities

   compare = Compare(match = match, mismatch = mismatch);
   addConst1 = AddConst(c = gap);
   addConst2 = AddConst(c = gap);
   add = Add();
   maxs = [Max() : for i in 1..3];

structure
	
	action [a], [b], [w], [nw], [n] ==> [v]
	var
		v1 = nw + if a = b then match else mismatch,
		v2 = w + gap,
		v3 = n + gap,
		v = max(max(v1, 0), max(v2, v3))
	end
	
end
