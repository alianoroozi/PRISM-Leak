dtmc

// value of the variable l
global observable l: [0..3];

// value of the variable h
global secret h: [0..3];

//observable l;

module thread1
	counter1 : [0..1];

	[] counter1=0 & l=1 -> (counter1'=1) & (l'=h);
    //[] counter1=1 & l=h -> true;
	[] counter1=0 & l=0 -> (counter1'=1);

endmodule

module thread2
	counter2 : [0..1];

	[] counter2=0 -> (counter2'=1) & (l'=1);

endmodule

module mainthread
    counter : [0..1];

    [] counter=0 & counter1=1 & counter2=1 -> (l'=2) & (counter'=1);
    [] counter=1 -> true;

endmodule

init 
    l=0 & counter1=0 & counter2=0 & counter=0 
endinit
