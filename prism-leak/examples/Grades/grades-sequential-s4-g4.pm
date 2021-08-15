dtmc

// number of grades
const int g = 4;

// number of students
const int s = 4;

// number of possible random numbers generated
const int n = (g-1)*s + 1;

// probability of random numbers
const double pr = 1/n;

// random numbers 
global num1 : [-1..n-1];
global num2 : [-1..n-1];
global num3 : [-1..n-1];
global num4 : [-1..n-1];

// state and preference of each student
global secret h1 : [0..g-1]; // secret variable
global secret h2 : [0..g-1]; // secret variable
global secret h3 : [0..g-1]; // secret variable
global secret h4 : [0..g-1]; // secret variable

global observable sum : [0..g*s-s]; // observable (public) variable

// public announcements of each student
global observable ann1 : [0..n-1];
global observable ann2 : [0..n-1];
global observable ann3 : [0..n-1];
global observable ann4 : [0..n-1];

module student1	

	s1 : [-s..s*2];

	// generate the random numbers
	[] s1=-4 -> pr:(num4'=0)&(s1'=-3) + pr:(num4'=1)&(s1'=-3) + 
					pr:(num4'=2)&(s1'=-3) + pr:(num4'=3)&(s1'=-3) + 
					pr:(num4'=4)&(s1'=-3) + pr:(num4'=5)&(s1'=-3) + 
					pr:(num4'=6)&(s1'=-3) + pr:(num4'=7)&(s1'=-3) + 
					pr:(num4'=8)&(s1'=-3) + pr:(num4'=9)&(s1'=-3) + 
					pr:(num4'=10)&(s1'=-3) + pr:(num4'=11)&(s1'=-3) + 
					1.0-(n-1)*pr:(num4'=12)&(s1'=-3);
	[] s1=-3 -> pr:(num1'=0)&(s1'=-2) + pr:(num1'=1)&(s1'=-2) + 
					pr:(num1'=2)&(s1'=-2) + pr:(num1'=3)&(s1'=-2) + 
					pr:(num1'=4)&(s1'=-2) + pr:(num1'=5)&(s1'=-2) + 
					pr:(num1'=6)&(s1'=-2) + pr:(num1'=7)&(s1'=-2) + 
					pr:(num1'=8)&(s1'=-2) + pr:(num1'=9)&(s1'=-2) + 
					pr:(num1'=10)&(s1'=-2) + pr:(num1'=11)&(s1'=-2) + 
					1.0-(n-1)*pr:(num1'=12)&(s1'=-2);
	[] s1=-2 -> pr:(num2'=0)&(s1'=-1) + pr:(num2'=1)&(s1'=-1) + 
					pr:(num2'=2)&(s1'=-1) + pr:(num2'=3)&(s1'=-1) + 
					pr:(num2'=4)&(s1'=-1) + pr:(num2'=5)&(s1'=-1) + 
					pr:(num2'=6)&(s1'=-1) + pr:(num2'=7)&(s1'=-2) + 
					pr:(num2'=8)&(s1'=-2) + pr:(num2'=9)&(s1'=-2) +
					pr:(num2'=10)&(s1'=-1) + pr:(num2'=11)&(s1'=-1) + 
					1.0-(n-1)*pr:(num2'=12)&(s1'=-1);
	[] s1=-1 -> pr:(num3'=0)&(s1'=0) + pr:(num3'=1)&(s1'=0) + 
					pr:(num3'=2)&(s1'=0) + pr:(num3'=3)&(s1'=0) + 
					pr:(num3'=4)&(s1'=0) + pr:(num3'=5)&(s1'=0) + 
					pr:(num3'=6)&(s1'=0) + pr:(num3'=7)&(s1'=-2) + 
					pr:(num3'=8)&(s1'=-2) + pr:(num3'=9)&(s1'=-2) +
					pr:(num3'=10)&(s1'=0) + pr:(num3'=11)&(s1'=0) + 
					1.0-(n-1)*pr:(num3'=12)&(s1'=0);

    // grade of student1 is h1
    [] s1=0 -> (ann1'=mod(h1+num1-num2,n)) & (s1'=1);
    [] s1=1 -> (ann2'=mod(h2+num2-num3,n)) & (s1'=2);
    [] s1=2 -> (ann3'=mod(h3+num3-num1,n)) & (s1'=3);
    [] s1=3 -> (ann3'=mod(h3+num3-num1,n)) & (s1'=4);

    // compute sum
    [] s1=4 -> (sum'=mod(sum+ann1,n)) & (s1'=5);
    [] s1=5 -> (sum'=mod(sum+ann2,n)) & (s1'=6);
    [] s1=6 -> (sum'=mod(sum+ann3,n)) & (s1'=7);
    [] s1=7 -> (sum'=mod(sum+ann4,n)) & (s1'=8);

endmodule

// set of initial states
init  s1=-3 & sum=0 & ann1=0&ann2=0&ann3=0&ann4=0 & num1=-1&num2=-1&num3=-1&num4=-1 endinit

