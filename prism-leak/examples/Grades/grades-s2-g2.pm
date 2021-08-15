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
global num3 : [-1..n-1];

// state and preference of each student
global s1 : [0..2];
global secret h1 : [0..g-1]; // secret variable
global s2 : [0..2];
global secret h2 : [0..g-1]; // secret variable
global s3 : [0..2];
global secret h3 : [0..g-1]; // secret variable

global observable sum : [0..g*s-s]; // observable (public) variable

// public announcements of each student
global observable ann1 : [0..n-1];
global observable ann2 : [0..n-1];
global observable ann3 : [0..n-1];

module student1	

	// generate the random numbers
	[] num1=-1 -> pr:(num1'=0) + pr:(num1'=1) + 1.0-(n-1)*pr:(num1'=2);

    // grade of student1 is h1
    [] s1=0 & num1>-1 & num2>-1 & num3>-1  -> (ann1'=mod(h1+num1-num2,n)) & (s1'=1);

    // compute sum
    [] s1=1 -> (sum'=mod(sum+ann1,n)) & (s1'=2);

endmodule

// construct further students with renaming
module student2 = student1 [ h1=h2, s1=s2, ann1=ann2, num1=num2, num2=num3, num3=num1 ] endmodule
module student3 = student1 [ h1=h3, s1=s3, ann1=ann3, num1=num3, num2=num1, num3=num2 ] endmodule

// set of initial states
init  s1=0 & s2=0 & s3=0 & sum=0 & ann1=0&ann2=0&ann3=0 & num1=-1&num2=-1&num3=-1 endinit

