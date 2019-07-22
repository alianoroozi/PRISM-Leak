package prismod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import parser.VarList;
import parser.ast.Declaration;
import prism.PrismException;
import prism.PrismLog;
import prism.ProbModel;
import prismintertrace.ExplicitState;

public class ODChecker {

	
	ProbModel probModel;
	ProbModelExplicitExplorerOD expModel;
	int numObservableVars = 0;
	
	// logs
	private PrismLog mainLog = null;
	
	public ODChecker(ProbModel probModel, PrismLog mainLog) throws PrismException {
		
		this.mainLog = mainLog;
		this.probModel = probModel;
		this.numObservableVars = determineNumObservableVars();
		if(numObservableVars == 0)
			throw new PrismException("There should be at least one observable variable!");
		
		// explore traces and compute trace probabilities and final state secret distributions
		this.expModel = new ProbModelExplicitExplorerOD(probModel, numObservableVars);

		mainLog.println("\nChecking Observational Determinism condition 2 ...");
		boolean result2 = checkODCondition2();
		mainLog.println("Observational Determinism condition 2: "+result2);
    	
    	if (result2) {	
    		mainLog.println("\nChecking Observational Determinism condition 1 ...");
	    	boolean result1 = checkODCondition1();
	    	mainLog.println("Observational Determinism condition 1: "+result1);
	   	} else {
	  		mainLog.println("\nObservational Determinism condition 2 does not hold, so there is "
	   				+ "no need to check Observational Determinism condition 1.");
	  	}
		
		
	}
	
    public boolean checkODCondition1() throws PrismException {
		
		
		Stack<Long> path  = new Stack<Long>();   // the current path
	    
	    boolean result = true;
	    long state_num;
	    String publicData = "";
	    
	    // one list of witnesses for each observable variable and one witness for each initial value of the observable variable
	    List<List<String>> witnesses = new ArrayList<>();
	    for(int i=0; i < numObservableVars; i++) {
//	    	witnesses.add(new HashMap<>());
	    	witnesses.add(new ArrayList<String>());
//	    	for(ExplicitState s: expModel.startStates) {
//	    		publicData = s.getPublicData(i);
//	    		witnesses.get(i).put(publicData, new ArrayList<String>());
//	    	}
	    }
	    	
	    
		for (ExplicitState s : expModel.startStates) {
			state_num = s.getStateNumber();
			result = allPathsCondition1(state_num, path, witnesses);
			if(!result)
				return false;
		}
		
    	return true;
	}
	
	public boolean checkODCondition2() throws PrismException {
	    
	    long state_num, s1, s2;
	    String s1_public, s2_public;
		
		// set of all traces (stutter steps removed) for each initial state
		Map<Long, Set<List<String>>> allTraces = new HashMap<>();
				
		Stack<Long> path  = new Stack<Long>();   // the current path
		for (ExplicitState s : expModel.startStates) {
			state_num = s.getStateNumber();	
			allTraces.put(state_num, new HashSet<>());
			allPathsCondition2(state_num, path, allTraces);
		}
		
		// check condition 2
		for (int i=0; i<expModel.startStates.size(); i++) { // create all pairs of initial states
			s1 = expModel.startStates.get(i).getStateNumber();	
			for (int j=i+1; j<expModel.startStates.size(); j++) {
				s2 = expModel.startStates.get(j).getStateNumber();
				
				if (s1 != s2) {
					s1_public = expModel.startStates.get(i).getPublicData(-1);
					s2_public = expModel.startStates.get(j).getPublicData(-1); 
					if (s1_public.equals(s2_public) && !allTraces.get(s1).equals(allTraces.get(s2)))
						return false;
				}
			}
		}
    	return true;
	} 
		
	// use DFS to find all paths starting from v
    private boolean allPathsCondition1(long v, Stack<Long> path, List<List<String>> witnesses) {

    	boolean result;
    	long iniState;
    	String iniStatePublicData;
    	List<String> trace, witness;
    	
        // add state number v to current path from the initial state
        path.push(v);

        // found a path
        if (expModel.isFinalState(v)) {
        	
        	for(int i=0; i < numObservableVars; i++) { 
	        	trace = expModel.trace(path, i);
	        	iniState = path.get(0);
	        	iniStatePublicData = expModel.getPublicData(iniState, i);
	        	witness = witnesses.get(i);//.get(iniStatePublicData);
	        	// Remove stuttering steps from trace
	        	trace = removeStutterData(trace);
	        	if (trace.size() <= witness.size()) {
	        		if (!isPrefix(trace, witness))
	        			return false;
		        	}
	        	else {
	        		if (!isPrefix(witness, trace))
	        			return false;
	        		else
		        		{
//		        			witnesses.get(i).clear();
		        			witnesses.set(i, trace);
		        		}
	        	}  
        	}	
        }
        
        // consider all successors that would continue path with repeating a state
        else {
            for (long w : expModel.post(v)) {
//                if (!path.contains(w)) { 
                	result = allPathsCondition1(w, path, witnesses);
	               	if (!result)
	               		return false;
//                }
            }
        }

        // done exploring from v, so remove from path
        path.pop();
        
        return true;
    }
    
    // use DFS to find all paths starting from v
    private void allPathsCondition2(long v, Stack<Long> path, Map<Long, Set<List<String>>> allTraces) {
    	
        // add state number v to current path from the initial state
        path.push(v);

        // found a path
        if (expModel.isFinalState(v)) {
        	
        	List<String> trace = expModel.trace(path, -1);
        	long iniState = path.get(0);
        	// Remove stuttering steps from trace
        	trace = removeStutterData(trace);
        	allTraces.get(iniState).add(trace);
    	
        }
        
        // consider all successors that would continue path with repeating a state
        else {
            for (long w : expModel.post(v)) {
//                if (!path.contains(w))  
                	allPathsCondition2(w, path, allTraces);
            }
        }

        // done exploring from v, so remove from path
        path.pop();
        
        return;
    }

    /**
     * 
     * @param trace_a
     * @param trace_b
     * @return true if trace_a is prefix of trace_b
     */
	public boolean isPrefix(List<String> trace_a, List<String> trace_b) {
		
		if(trace_a.size() <= trace_b.size()) {
			
			for(int i=0; i < trace_a.size(); i++) 
				if(!trace_a.get(i).equals(trace_b.get(i)))
					return false;
			
			return true;
		}
		
		return false;
	}
	
	/**
	 * Removes stutter data of the trace 
	 * 
	 * @param trace
	 * @return
	 */
	public List<String> removeStutterData(List<String> trace){
		List<String> new_trace = new ArrayList<>();
		String data = trace.get(0);
		new_trace.add(data);
		for(String d: trace) {
			if(!d.equals(data)) {
				data = d;
				new_trace.add(data);
			}
		}
		return new_trace;
	}
	
	public int determineNumObservableVars() {
		
		int num = 0;
		VarList varList = probModel.getVarList();
		int j = varList.getNumVars();
		int observabilityType;
		
		for (int i = 0; i < j; i++) {
			
			observabilityType = varList.getDeclaration(i).getObservabilityType();
			
			if(observabilityType == Declaration.OBSERVABILITY_OBSERVABLE) {
				num ++;
			}
		}
		return num;
	}
	

}
