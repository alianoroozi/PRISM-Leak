dtmc

// number of grades
const int g = 2;

// number of students
const int s = 3;


// state and preference of each student
global secret h1 : [0..g-1]; // secret variable
global secret h2 : [0..g-1]; // secret variable
global secret h3 : [0..g-1]; // secret variable

global observable sum : [0..g*s-s]; // observable (public) variable


module student1
	s1 : [0..s]; // its status 

    [] s1=0 & sum<=g*s-s-h1 -> (sum'=sum+h1) & (s1'=1);
    [] s1=1 & sum<=g*s-s-h2 -> (sum'=sum+h2) & (s1'=2);
    [] s1=2 & sum<=g*s-s-h3 -> (sum'=sum+h3) & (s1'=3);

endmodule

// set of initial states
init  s1=0 & sum=0 endinit

