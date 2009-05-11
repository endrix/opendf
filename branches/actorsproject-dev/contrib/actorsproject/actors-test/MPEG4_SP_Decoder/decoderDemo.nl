network decoderDemo () ==> :

entities
	decoder = decoder();
	sourceBits = art_Source_bin(fileName="./foreman_qcif_30.bit");
	display = art_Display_yuv(title="Foreman QCIF",
                                  width=176,
			          height=144);
        ddr = art_DDRModel(MAXW_IN_MB = 121, MAXH_IN_MB = 69);

structure
	sourceBits.Out --> decoder.bits;
	decoder.VIDEO --> display.In;
	
	ddr.RD --> decoder.MCD {bufferSize = 100;};
	decoder.MBD --> ddr.WD {bufferSize = 100;};
	decoder.MBA --> ddr.WA {bufferSize = 100;};
	decoder.MCA --> ddr.RA {bufferSize = 100;};

end
