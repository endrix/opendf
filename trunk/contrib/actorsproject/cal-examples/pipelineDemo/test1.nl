network test1() ==> :

entities
        
        producer0 = producer(affinity=0);
	    process0_0 = processblock(affinity=0);
	    process0_1 = process(affinity=0);
        consumer0 = consumer(affinity=0);

        producer1 = producer(affinity=1);
	    process1_0 = processblock(affinity=1);
	    process1_1 = process(affinity=1);
        consumer1 = consumer(affinity=1);

        producer2 = producer(affinity=2);
	    process2_0 = processblock(affinity=2);
	    process2_1 = process(affinity=2);
        consumer2 = consumer(affinity=2);

        producer3 = producer(affinity=3);
	    process3_0 = processblock(affinity=3);
	    process3_1 = process(affinity=3);
        consumer3 = consumer(affinity=3);

        service_level = servicelevel(affinity=0);
        dbus = art_DBus_test(affinity=0);
        happy = art_happiness(affinity=0);

structure
        producer0.Out --> process0_0.In;
       	process0_0.Out --> process0_1.In;
       	process0_1.Out --> consumer0.In;
        service_level.Out1 --> process0_0.Toggle;

        producer1.Out --> process1_0.In;
       	process1_0.Out --> process1_1.In;
       	process1_1.Out --> consumer1.In;
        service_level.Out2 --> process1_0.Toggle;

        producer2.Out --> process2_0.In;
       	process2_0.Out --> process2_1.In;
       	process2_1.Out --> consumer2.In;
        service_level.Out3 --> process2_0.Toggle;

        producer3.Out --> process3_0.In;
       	process3_0.Out --> process3_1.In;
       	process3_1.Out --> consumer3.In;
        service_level.Out4 --> process3_0.Toggle;

        dbus.Out --> service_level.In;
        happy.Out  --> dbus.In;

        happy.Quit --> producer0.Quit;
        happy.Quit --> producer1.Quit;
        happy.Quit --> producer2.Quit;
        happy.Quit --> producer3.Quit;
end
