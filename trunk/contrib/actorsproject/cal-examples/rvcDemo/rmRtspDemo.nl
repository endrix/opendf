network rmRtspClock () ==> :

entities
	decoder=orcc_decoder(ACCODED=2,
                         ACPRED=1,
                         ADDR_SZ=24,
                         BTYPE_SZ=12,
                         FCODE_MASK=448,
                         FCODE_SHIFT=6,
                         FOURMV=4,
                         INTER=512,
                         INTRA=1024,
                         MAXH_IN_MB=69,
                         MAXW_IN_MB=121,
                         MB_COORD_SZ=8,
                         MOTION=8,
                         MV_SZ=9,
                         NEWVOP=2048,
                         QUANT_MASK=31,
                         ROUND_TYPE=32,
                         SAMPLE_COUNT_SZ=8,
                         SAMPLE_SZ=13);

	sourceBits = art_Rtsp(activeMode=1,url="$URL");
    display = art_Display_yuv_width_height(title="Display",width=720,height=576);
	dbus = art_DBus_test();

structure
	sourceBits.Out --> decoder.bits;
	decoder.VID --> display.In;
	decoder.WIDTH --> display.WIDTH;
	decoder.HEIGHT --> display.HEIGHT;
	dbus.Out --> sourceBits.In;
	sourceBits.Out2 --> dbus.In;
end
