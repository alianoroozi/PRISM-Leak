dtmc

// s is the number of students
const int s = 3;

// g is the number of grades (from 0 to g-1)
const int g = 2;

// n is the number of possible random numbers generated: n=((g-1)*s)+1
formula n = ((g-1)*s)+1;

// this is the sum that will be printed
global output: [0..n];

// this is an internal counter for the sum
global observable sum: [0..100];

// there are s secrets, each one with g possible values:
global secret h0 : [0..g-1];
global secret h1 : [0..g-1];
global secret h2 : [0..g-1];


module gradesum
	i : [0..s]; 
	j : [0..g-1];
	
	[] i=0 & j=0 & h0=j -> (sum'=0) & (j'=mod(j+1,g));
	[] i=0 & j=0 & h0!=j -> (j'=mod(j+1,g));
	[] i=0 & j=1 & h0=j -> (sum'=1) & (j'=mod(j+1,g)) & (i'=i+1);
	[] i=0 & j=1 & h0!=j -> (j'=mod(j+1,g)) & (i'=i+1);
	
	[] i=1 & j=0 & h1=j -> (sum'=sum) & (j'=mod(j+1,g));
	[] i=1 & j=0 & h1!=j -> (j'=mod(j+1,g));
	[] i=1 & j=1 & h1=j -> (sum'=sum+1) & (j'=mod(j+1,g)) & (i'=i+1);
	[] i=1 & j=1 & h1!=j -> (j'=mod(j+1,g)) & (i'=i+1);
	
	[] i=2 & j=0 & h2=j -> (sum'=2) & (j'=mod(j+1,g));
	[] i=2 & j=0 & h2!=j -> (j'=mod(j+1,g));
	[] i=2 & j=1 & h2=j -> (sum'=3) & (j'=mod(j+1,g)) & (i'=i+1) ;
	[] i=2 & j=1 & h2!=j -> (j'=mod(j+1,g)) & (i'=i+1) ;

	//[] i=3 -> (output'=sum);
	
endmodule

init
i=0 & j=0 & sum=0 & output=0
endinit


