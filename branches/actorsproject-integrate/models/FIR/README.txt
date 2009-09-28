
The FIRv1-FIRv4 directories contain a number of models implementing and 
running a finite impulse response (FIR) filter. The filters are parametric in
the sense that they depend on a list of filter coefficients of arbitrary length.

All top-level models instantiate the actor net.sf.caltrop.actors.Plotter, which
is used to plot the input and output waveform of the filters. The input waveform
is generated as a sine wave to which some random noise is added. 

All waveforms are "tagged", i.e. the samples are paired with a label in the 
"Tag" actor, so they can be distinguished and properly labeled by the plotter.

 