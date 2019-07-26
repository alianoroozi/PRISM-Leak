package prisminterleakexp;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import prism.PrismException;
import prism.PrismLog;
import prism.ProbModel;

/**
 * A class for computing intermediate leakage
 *
 * @author Ali A. Noroozi
 */

public class InterLeakComputerExp {
	
	ProbModelExplicitExplorer expModel;
	ProbModel probModel;
	
	// logs
	private PrismLog mainLog = null;
	
	public static boolean MIN_ENTROPY = false;
	public static boolean SHANNON_ENTROPY = true;
	private boolean entropyType = SHANNON_ENTROPY; 
		
	private Map<List<String>, Map<String, Double>> secretTraceCondProbs; // the distribution Pr(h|T) 
	private Map<List<String>, Double> traceProbs; // trace probabilities Pr(T)
	
	
    public InterLeakComputerExp(ProbModel probModel, boolean bounded, int boundedStep, 
    		boolean entropyType, String initDistFileName, PrismLog mainLog) throws PrismException {
    	
    	this.entropyType = entropyType;
    	this.mainLog = mainLog;
    	
    	if(!bounded)
    		mainLog.println("\nExploring traces ...\n");
    	// explore traces and compute trace-secret probabilities 
    	expModel = new ProbModelExplicitExplorer(probModel, initDistFileName);
    	expModel.exploreModel(bounded, boundedStep);
    	
    	computeSecretTraceCondProbs();
    	
    	if(!bounded)
    		mainLog.println(traceProbs.size() + " traces found");
    	
    }
    
    /**
     * 
     * @param traceSecretDist contains trace-secret probabilities: Pr(T=\bar{T}, h=\bar{h})
     */
    public void computeSecretTraceCondProbs(){
    	
    	List<String> tr;
    	Map<String, Double> trSecretDist;
    	traceProbs = new HashMap<>(); // Pr(T)
    	
    	secretTraceCondProbs = expModel.getTraceSecretDist(); // Pr(h,T)
    	for(Map.Entry<List<String>, Map<String, Double>> entry: secretTraceCondProbs.entrySet()){
        	
            tr = entry.getKey();
            trSecretDist = entry.getValue(); // pr(h|T=tr)
            
            double trProb = trSecretDist.values().stream().mapToDouble(d->d).sum(); // Pr(T=tr)
            trSecretDist.replaceAll((k, v) -> v/trProb);
            
            traceProbs.put(tr, trProb);
    	}
    }
    
    /**
     * Compute leakage using trace-exploration-based method. 
     * 
     * @return expected leakage
     */
    public double expectedLeakage() throws PrismException {

    	double initialUncertainty = initialUncertainty();
    	double remainingUncertainty = remainingUncertainty();
        double leakage = initialUncertainty - remainingUncertainty;
        return leakage;
    }
    
    /**
     * Compute remaining uncertainty H(h|T)
     * 
     * @return remaining uncertainty H(h|T)
     */
    public double remainingUncertainty() throws PrismException {
    	
    	List<String> tr;
    	Map<String, Double> trSecretDist;
    	double trProb, trFinalEntropy;
    	
        double remaining_uncertainty = 0; // H(h|T)
        for(Map.Entry<List<String>, Double> entry: traceProbs.entrySet()){
        	
            tr = entry.getKey();
            trProb = entry.getValue(); // Pr(T=tr)
            trSecretDist = secretTraceCondProbs.get(tr); // Pr(h|T=tr)
            trFinalEntropy = entropy(trSecretDist); // H(h|T=tr)
            
            remaining_uncertainty += trProb * trFinalEntropy;     
        }
        
        return remaining_uncertainty;
    }
    
    /**
     * Compute initial uncertainty H(h)
     * 
     * @return 
     */
    public double initialUncertainty(){
    	
    	double initialUncertainty = 0;
	    initialUncertainty = entropy(expModel.getPriorKnowledge()); // H(h)
    	return initialUncertainty;
    }
    
    /**
     * Compute maximum leakage using trace-exploration-based method. 
     * 
     * @return maximum leakage
     */
    public double maxLeakage(){
        
        // compute minimum posterior entropy: min(H(h|T=tr)) for all traces tr
        double minimumEntropy = minimumEntropy();
        double initialUncertainty = initialUncertainty();
        double maxLeakage = initialUncertainty - minimumEntropy;
        return maxLeakage;
    }
    
    /**
     * Compute minimum leakage. 
     * 
     * @return minimum leakage
     */
    public double minLeakage(){
        
        // compute maximum posterior entropy: max(H(h|T=tr)) for all traces tr
        double maximumEntropy = maximumEntropy();
        double initialUncertainty = initialUncertainty();
        double leakage = initialUncertainty - maximumEntropy;
        return leakage;
    }
    
    /**
     * Compute probability of maximum leakage of state_machine using trace-exploration-based method. 
     * 
     * @return probability of maximum leakage
     */
    public double probMaxLeakage(){
        
        // compute minimum posterior entropy 
        double minimumEntropy = minimumEntropy();
        
        // compute probability of maximum leakage
        List<String> tr;
    	Map<String, Double> trSecretDistribution;
    	double trFinalEntropy;
    	
    	double probMaxLeakage = 0;
        for(Map.Entry<List<String>, Double> entry: traceProbs.entrySet()){
        	
            tr = entry.getKey();
            trSecretDistribution = secretTraceCondProbs.get(tr);
            
        	trFinalEntropy = entropy(trSecretDistribution);
        	if(minimumEntropy == trFinalEntropy)
        		probMaxLeakage += traceProbs.get(tr);
        }

        return probMaxLeakage;
    }
    
    /**
     * Compute probability of minimum leakage of state_machine using trace-exploration-based method. 
     * 
     * @return probability of minimum leakage
     */
    public double probMinLeakage(){
        
        // compute maximum posterior entropy of traces
        double maximumEntropy = maximumEntropy();
        
        // compute probability of minimum leakage
        List<String> tr;
    	Map<String, Double> trSecretDistribution;
    	double trFinalEntropy;
    	
    	double probMinLeakage = 0;
        for(Map.Entry<List<String>, Double> entry: traceProbs.entrySet()){
        	
            tr = entry.getKey();
            trSecretDistribution = secretTraceCondProbs.get(tr);
            
        	trFinalEntropy = entropy(trSecretDistribution);
        	if(maximumEntropy == trFinalEntropy)
        		probMinLeakage += traceProbs.get(tr);
        }

        return probMinLeakage;
    }
    
    /**
     * 
     * @return maximum posterior entropy (Shannon or min-entropy): max(H(h|T=tr)) for all traces tr
     */
    public double maximumEntropy() {
    	
        List<String> tr;
    	Map<String, Double> trSecretDistribution;
    	double trFinalEntropy;
    	
    	double maximumEntropy = -1;
        for(Map.Entry<List<String>, Double> entry: traceProbs.entrySet()){
        	
            tr = entry.getKey();
            trSecretDistribution = secretTraceCondProbs.get(tr);
        
        	trFinalEntropy = entropy(trSecretDistribution);
        	if(maximumEntropy < trFinalEntropy)
        		maximumEntropy = trFinalEntropy;
        }
        
        return maximumEntropy;
    }
    
    /**
     * 
     * @return minimum posterior entropy (Shannon or min-entropy): min(H(h|T=tr)) for all traces tr
     */
    public double minimumEntropy() {
    	
    	// compute minimum posterior entropy
        List<String> tr;
    	Map<String, Double> trSecretDistribution;
    	double trFinalEntropy;
    	
    	double minimumEntropy = Double.MAX_VALUE;
        for(Map.Entry<List<String>, Double> entry: traceProbs.entrySet()){
        	
            tr = entry.getKey();
            trSecretDistribution = secretTraceCondProbs.get(tr);
        
            trFinalEntropy = entropy(trSecretDistribution);
        	if(minimumEntropy > trFinalEntropy)
        		minimumEntropy = trFinalEntropy;
        }
        return minimumEntropy;
    }
    
    
    /**
     * 
     * @return list of traces with maximum probability
     */
    public List<List<String>> tracesMaxProbability(){
    	
    	// compute max probability of all trace probabilities
    	List<String> tr;
    	double trProb;
    	
    	double maxTraceProb = -1;
    	for(Map.Entry<List<String>, Double> entry: traceProbs.entrySet()){
        	
            tr = entry.getKey();
    		trProb = entry.getValue();
    		
    		if(trProb > maxTraceProb)
    			maxTraceProb = trProb;
    	}
    	// find traces with maximum probability
    	List<List<String>> tracesMaxProb = new ArrayList<>();
    	
    	for(Map.Entry<List<String>, Double> entry: traceProbs.entrySet()){
        	
            tr = entry.getKey();
    		trProb = entry.getValue();
    		
    		if(trProb == maxTraceProb)
    			tracesMaxProb.add(tr);
    	}
    	return tracesMaxProb;
    }
    
    
    
    /**
     * Complete leakage occurs in traces that result in min-entropy of 0.
     * 
     * @return probability of complete leakage
     */
    public double probCompleteLeakage() {
    	
    	List<String> tr;
    	Map<String, Double> trSecretDistribution;
    	double trFinalEntropy, trProb;
    	
    	double probCompleteLeakage = 0;
        for(Map.Entry<List<String>, Double> entry: traceProbs.entrySet()){
        	
            tr = entry.getKey();
            trSecretDistribution = secretTraceCondProbs.get(tr);
    	
            trFinalEntropy = entropy(trSecretDistribution);
	        if(trFinalEntropy == 0.0) { // complete leakage
	        	trProb = traceProbs.get(tr);
	        	probCompleteLeakage += trProb;
	        }
		}
    	return probCompleteLeakage;
    }
    
    /**
     * 
     * @return Shannon or min-entropy of distribution
     */
    public double entropy(Map<String, Double> distribution) {
    	
    	if(entropyType == MIN_ENTROPY)
        	return minEntropy(distribution);
        else // SHANNON_ENTROPY
        	return shannonEntropy(distribution);
    }
    
    /**
     * 
     * @param distribution contains elements as String and their probabilities as Double
     * @return min-entropy of the distribution
     * 
     */
    public double minEntropy(Map<String, Double> distribution){
        double max_prob = -1.0;
        for(Map.Entry<String, Double> entry: distribution.entrySet()){
            double prob = entry.getValue();
            max_prob = Math.max(prob, max_prob);
        }
        double d =  - Logarithm.log2(max_prob);
        return d;
    }
    
    /**
     * 
     * @param distribution contains elements as String and their probabilities as Double
     * @return Shannon entropy of the distribution
     * 
     */
    public double shannonEntropy(Map<String, Double> distribution){
        double shannon = 0;
        for(Map.Entry<String, Double> entry: distribution.entrySet()){
            double p = entry.getValue();
            if(p != 0) {
            	double log_p =  Logarithm.log2(p);
            	shannon += p * log_p;
            }
        }
        return -shannon;
    }
    
    public void printModelInfo(String modelFilename) {
    	List<String> tr;
    	Map<String, Double> dist;
    	String modelInfoFileName = modelFilename.substring(0, modelFilename.lastIndexOf('.'));

        try (PrintWriter outFile = new PrintWriter(modelInfoFileName)) {
        	outFile.println(secretTraceCondProbs.size());
            for(Map.Entry<List<String>, Map<String, Double>> entry: secretTraceCondProbs.entrySet()){
            	
                tr = entry.getKey();
                dist = entry.getValue();
                outFile.println(tr + ":" + dist);
      
            }
        } catch (FileNotFoundException e) {
			e.printStackTrace();
		}
                
        return;
    }
    
}


class Logarithm
{
    public static double log2( double a ){
        return Math.log(a) / Math.log(2);
    }
}