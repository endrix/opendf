network test2() ==> :

entities
        
        producer0 = art_producer(affinity=0, N="$N", chunk="$chunk");
	process0_0 = process(affinity=1);
	process0_1 = process(affinity=2);
        consumer0 = art_consumer(affinity=3);

        producer1 = art_producer(affinity=1, N="$N", chunk="$chunk");
	process1_0 = process(affinity=2);
	process1_1 = process(affinity=3);
        consumer1 = art_consumer(affinity=0);

        producer2 = art_producer(affinity=2, N="$N", chunk="$chunk");
	process2_0 = process(affinity=3);
	process2_1 = process(affinity=0);
        consumer2 = art_consumer(affinity=1);

        producer3 = art_producer(affinity=3, N="$N", chunk="$chunk");
	process3_0 = process(affinity=0);
	process3_1 = process(affinity=1);
        consumer3 = art_consumer(affinity=2);

structure
        producer0.Out --> process0_0.In;
       	process0_0.Out --> process0_1.In;
       	process0_1.Out --> consumer0.In;

        producer1.Out --> process1_0.In;
       	process1_0.Out --> process1_1.In;
       	process1_1.Out --> consumer1.In;

        producer2.Out --> process2_0.In;
       	process2_0.Out --> process2_1.In;
       	process2_1.Out --> consumer2.In;

        producer3.Out --> process3_0.In;
       	process3_0.Out --> process3_1.In;
       	process3_1.Out --> consumer3.In;
end
