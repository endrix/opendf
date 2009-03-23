network idct2d () \in\, signed ==> out :

var
	INP_SZ = 12;
	MEM_SZ = 16;
	OUT_SZ = 10;
	PIX_SZ = 9;
	
entities

	scale = Scale(SIN_SZ = 32, SOUT_SZ = 32);
	
	row  = scaled_1d_idct(IN_SZ = 32, OUT_SZ = 32);

	column  = scaled_1d_idct(IN_SZ = 32, OUT_SZ = 32);
	
	transpose = Transpose();

	retranspose = Transpose();

	shift = rightshift();

	clip = Clip(isz = OUT_SZ, osz = PIX_SZ);

structure

	\in\ --> scale.Sin;
	signed --> clip.SIGNED;
	scale.Sout --> transpose.In;
	transpose.Out --> row.In;
	row.Out --> retranspose.In;
	retranspose.Out --> column.In;
	column.Out --> shift.In;
 	shift.Out --> clip.I;
	clip.O --> out;

end
