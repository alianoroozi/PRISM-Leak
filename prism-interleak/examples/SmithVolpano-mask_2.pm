dtmc

const int n = 2; // num of bits of the pin variable

global turn : [1..4];
global c1 : [0..5];
global c2 : [0..5];
global c3 : [0..7];
global maintrigger : [0..2];
global trigger0 : [0..1];
global trigger1 : [0..1];
global mask : [0..pow(2, n)-1];
global observable result : [0..pow(2, n)-1];
global secret pin : [0..pow(2, n)-1];

module Alpha

    [] turn=1 & c1=0 & mask!=0 -> (c1'=1);
    [] turn=1 & c1=1 & trigger0=0 & trigger1=1 -> (turn'=2);
    [] turn=1 & c1=1 & trigger0=0 & trigger1!=1 -> (turn'=3);
    [] turn=1 & c1=1 & trigger0!=0 -> (c1'=2);
    [] turn=1 & c1=2 & mod(floor(result/mask),2)=0 & 
              result+mask<=pow(2,n)-1 -> 
              (result'=result+mask) & (c1'=3);
    [] turn=1 & c1=2 & mod(floor(result/mask),2)=1 -> 
                                      (c1'=3);
    [] turn=1 & c1=3 -> (trigger0'=0) & (c1'=4);
    [] turn=1 & c1=4 & maintrigger<2 -> 
       (maintrigger'=maintrigger+1) & (c1'=5); 
    [] turn=1 & c1=5 & maintrigger=1 -> 
                      (trigger1'=1) & (c1'=0);
    [] turn=1 & c1=5 & maintrigger!=1 -> (c1'=0);
endmodule

module Beta

    [] turn=2 & c2=0 & mask!=0 -> (c2'=1);
    [] turn=2 & c2=1 & trigger1=0 & trigger0=1 -> (turn'=1);
    [] turn=2 & c2=1 & trigger1=0 & trigger0!=1 -> (turn'=3);
    [] turn=2 & c2=1 & trigger1!=0 -> (c2'=2);
    [] turn=2 & c2=2 & mod(floor(result/mask),2)=1 -> 
              (result'=result-mask) & (c2'=3);
    [] turn=2 & c2=2 & mod(floor(result/mask),2)=0 -> 
                                      (c2'=3);
    [] turn=2 & c2=3 -> (trigger1'=0) & (c2'=4);
    [] turn=2 & c2=4 & maintrigger<2 -> 
       (maintrigger'=maintrigger+1) & (c2'=5); 
    [] turn=2 & c2=5 & maintrigger=1 -> 
                      (trigger0'=1) & (c2'=0);
    [] turn=2 & c2=5 & maintrigger!=1 -> (c2'=0);
endmodule

module Gamma

    [] turn=3 & mask!=0 & c3=0 -> (c3'=1);
    [] turn=3 & mask=0 & c3=0 -> (c3'=5);
    [] turn=3 & c3=1 -> (maintrigger'=0) & (c3'=2);
    [] turn=3 & c3=2 & mod(floor(pin/mask),2)=0 -> 
                      (trigger0'=1) & (c3'=3);
    [] turn=3 & c3=2 & mod(floor(pin/mask),2)=1 -> 
                      (trigger1'=1) & (c3'=3);
    [] turn=3 & c3=3 & maintrigger=2 -> (c3'=4);
    [] turn=3 & c3=3 & maintrigger!=2 -> (turn'=2);
    [] turn=3 & c3=3 & maintrigger!=2 -> (turn'=1);
    [] turn=3 & c3=4 -> (mask'=floor(mask/2)) & (c3'=0);
    [] turn=3 & c3=5 -> (trigger0'=1) & (c3'=6);
    [] turn=3 & c3=6 -> (trigger1'=1) ;
endmodule

init
    mask=2 & result=0 & maintrigger=0 & trigger0=0 & 
    trigger1=0 & c1=0 & c2=0 & c3=0 & turn=3
endinit
