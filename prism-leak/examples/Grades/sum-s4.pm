dtmc

// number of grades
const int g = 4;

// number of students
const int s = 4;

// state and preference of each student
global s1 : [0..1];
global secret h1 : [0..g-1]; // secret variable
global s2 : [0..1];
global secret h2 : [0..g-1]; // secret variable
global s3 : [0..1];
global secret h3 : [0..g-1]; // secret variable
global s4 : [0..1];
global secret h4 : [0..g-1]; // secret variable

global observable sum : [0..g*s-s]; // observable (public) variable


module student1	

    // grade of student1 is h1
    [] s1=0 & sum<=g*s-s-h1 -> (sum'=sum+h1) & (s1'=1);

endmodule

// construct further students with renaming
module student2 = student1 [ h1=h2, s1=s2 ] endmodule
module student3 = student1 [ h1=h3, s1=s3 ] endmodule
module student4 = student1 [ h1=h4, s1=s4 ] endmodule

// set of initial states
init  s1=0 & s2=0 & s3=0 & s4=0 & sum=0 endinit

