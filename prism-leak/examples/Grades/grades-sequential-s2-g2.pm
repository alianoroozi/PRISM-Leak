dtmc

// number of grades
const int g = 2;

// number of students
const int s = 2;

// number of possible random numbers generated
const int n = (g-1)*s + 1;

// probability of random numbers
const double pr = 1/n;

// random numbers 
global num1 : [-1..n-1];
global num2 : [-1..n-1];

// state and preference of each student
global secret h1 : [0..g-1]; // secret variable
global secret h2 : [0..g-1]; // secret variable

global observable sum : [0..g*s-s]; // observable (public) variable

// public announcements of each student
global observable ann1 : [0..n-1];
global observable ann2 : [0..n-1];

module student1	

	s1 : [-s..s*2];

	// generate the random numbers
	[] s1=-2 -> pr:(num2'=0)&(s1'=-1) + pr:(num2'=1)&(s1'=-1) + 
						  1.0-(n-1)*pr:(num2'=2)&(s1'=-1);
	[] s1=-1 -> pr:(num1'=0)&(s1'=0) + pr:(num1'=1)&(s1'=0) + 
						  1.0-(n-1)*pr:(num1'=2)&(s1'=0);

    // grade of student1 is h1
    [] s1=0 -> (ann1'=mod(h1+num1-num2,n)) & (s1'=1);
    [] s1=1 -> (ann2'=mod(h2+num2-num1,n)) & (s1'=2);

    // compute sum
    [] s1=2 -> (sum'=mod(sum+ann1,n)) & (s1'=3);
    [] s1=3 -> (sum'=mod(sum+ann2,n)) & (s1'=4);

endmodule

// set of initial states
init  s1=-2 & sum=0 & ann1=0&ann2=0 & num1=-1&num2=-1 endinit

