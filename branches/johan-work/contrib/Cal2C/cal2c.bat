set SDL="D:\libraries\SDL-1.2.13"
set SYSC="D:\libraries\systemc-2.2.0"
set TLM="D:\libraries\TLM-2007-11-29"
src\_build\Cal2C\cal2c.native -debug -I %SDL% -I testbench/libcal -I %SYSC% -I %TLM% -mp examples/RVC_MPEG4_SP_Decoder -o testbench/generated testbed
pause
