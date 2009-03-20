#ifndef BROADCASTER_H
#define BROADCASTER_H

SC_MODULE(broadcaster_input) {
	// FIFOs
	sc_port<tlm::tlm_fifo_get_if<int> > input;
	sc_port<tlm::tlm_fifo_put_if<int>, 0, SC_ALL_BOUND > output;

	// Parameters

	SC_HAS_PROCESS(broadcaster_input);

	broadcaster_input(sc_module_name N) : sc_module(N)
	{
		SC_THREAD(process);
	}

	void process() {
		int val;

		while (1) {
			val = input->get();
			for (int i = 0; i < output.size(); i++) {
				output[i]->put(val);
			}
		}
	}

};

SC_MODULE(broadcaster_output) {
	// FIFOs
	tlm::tlm_fifo<int> input;
	sc_port<tlm::tlm_fifo_put_if<int>, 0, SC_ALL_BOUND > output;

	// Parameters

	SC_HAS_PROCESS(broadcaster_output);

	broadcaster_output(sc_module_name N) : sc_module(N),
		input(-16)
	{
		SC_THREAD(process);
	}

	void process() {
		int val;

		while (1) {
			val = input.get();
			for (int i = 0; i < output.size(); i++) {
				output[i]->put(val);
			}
		}
	}

};

#endif
