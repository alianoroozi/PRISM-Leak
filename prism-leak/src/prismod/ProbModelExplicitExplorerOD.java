package prismod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.Stack;
import java.util.TreeMap;

import jdd.JDDNode;
import jdd.JDDVars;
import odd.ODDNode;
import parser.VarList;
import parser.ast.Declaration;
import parser.type.TypeInt;
import prism.PrismException;
import prism.ProbModel;
import prism.StateListMTBDD;
import prismintertrace.ExplicitState;
import sparse.PrismSparse;

/**
*
*	A class for explicit representation and exploration of ProbModel
*
* @author Ali A. Noroozi
*/

public class ProbModelExplicitExplorerOD {
	
	private ProbModel currentModel = null;
	List<ExplicitState> reachStates; // set of reachable states
	List<ExplicitState> startStates; // set of initial states
	int numObservableVars;
	
	JDDNode matrix;
	String name;
	JDDVars rows;
	JDDVars cols;
	ODDNode odd;
	
//	Map<Long, Set<List<String>>> allVarsIniTraces; // all traces of an initial state - temporary variable to print traces
//	List<Set<List<String>>> varsListTraces;
	
	public ProbModelExplicitExplorerOD(ProbModel currentModel, int numObservableVars) throws PrismException {
		
		this.currentModel = currentModel;
		this.reachStates = getStates();
		this.startStates = getInitialStates();
		
		assert numObservableVars > 0: "There should be at least one observable variable!";
		this.numObservableVars = numObservableVars;
		
		matrix = currentModel.getTrans();
		name = currentModel.getTransSymbol();
		rows = currentModel.getAllDDRowVars();
		cols = currentModel.getAllDDColVars();
		odd = currentModel.getODD();
				
		double res = PrismSparse.PS_CreateSparseMatrix(matrix.ptr(), name, rows.array(), rows.n(), cols.array(), cols.n(), odd.ptr());
		if (res == -2) {
			throw new PrismException("Out of memory building transition matrix");
		}
		
//      PrismSparse.PS_FreeSparseMatrix(matrix.ptr(), name, rows.array(), rows.n(), cols.array(), cols.n(), odd.ptr());
	}

	 /**
	  * 
	  * @return the explicit set of reachable states 
	  */
	public List<ExplicitState> getStates() {
		 
		StateListMTBDD states = (StateListMTBDD) currentModel.getReachableStates();
		return states.getExplicitStates();
	}

	 /**
	  * 
	  * @return the explicit set of initial states 
	  */
	public List<ExplicitState> getInitialStates() {
		 
		StateListMTBDD start = (StateListMTBDD) currentModel.getStartStates();
		return start.getExplicitStates();
	}
	 
	 /**
	  * Note: Make sure s is less than reachStates.size()
	  * @return successor states of s. If s has a self-loop, it is included in post(s)
	  */
	public int[] post(long s) {
		
		return PrismSparse.PS_SuccessorStates((int) s, matrix.ptr(), name, rows.array(), rows.n(), cols.array(), cols.n(), odd.ptr());
	}
	 
	 /**
	  * 
	  * @return true if s has no successor or the only successor is itself
	  */
	public boolean isFinalState(long s) {
		 
		return PrismSparse.isFinalState((int) s, matrix, name, rows, cols, odd);
	}
	 
	 /**
	  * 
	  * @return transition probability between states i and j
	  */
	public double getTransitionProb(long i, long j) {
		 
		return PrismSparse.PS_GetTransitionProb((int) i, (int) j, matrix.ptr(), name, rows.array(), rows.n(), cols.array(), cols.n(), odd.ptr());
	}
	 
	 /**
	  * 
	  * @return probability of path. Probability of the initial state is not included.
	  */
	public double prob(List<Long> path) {
		 
        double prob = 1.0;
        for(int i=0; i < path.size()-1; i++)
            prob = prob *  getTransitionProb(path.get(i), path.get(i+1));
        
        return prob;
	}
	 
	 /**
	  * If varIndex is set to -1, return trace of all variables, 
	  * else return trace of variable in varIndex 
	  * 
	  * @return trace of path
	  */
	public List<String> trace(List<Long> path, int varIndex){
		 	 
		String publicData; 
		List<String> trace = new ArrayList<>();
        
		for(long s: path) {
        	publicData = getPublicData(s, varIndex);
        	trace.add(publicData);	
		}
        
		return trace;
	}
	
	/**
	 * If varIndex is set to -1, return public data of all variables, 
	 * else return value (public data) of variable in varIndex 
	 * @param s
	 * @return public data of state s
	 */
	public String getPublicData(long s, int varIndex) {
		
		return reachStates.get((int) s).getPublicData(varIndex);
	}
	
	public int getNumObservableVars() {
		return numObservableVars;
	}

}
