package prismfinalleak;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import prism.PrismException;
import prism.PrismLog;
import prism.ProbModel;
import prismintertrace.InterLeakComputerExp;

/**
 * A class for computing final leakage
 *
 * @author Ali A. Noroozi
 */

public class FinalLeakComputerExp {
	ProbModelOutputExplorer expModel;
	ProbModel probModel;
	
	// logs
	private PrismLog mainLog = null;
	
	public static boolean MIN_ENTROPY = false;
	public static boolean SHANNON_ENTROPY = true;
	private boolean entropyType = SHANNON_ENTROPY; 
		
	private Map<String, Map<String, Double>> secretOutCondProbs; // the distribution Pr(h|o) 
    private Map<String, Double> outProbs; // output probabilities Pr(o)
    
    
    public FinalLeakComputerExp(ProbModel probModel, boolean bounded, int boundedStep, 
            boolean entropyType, String initDistFileName, PrismLog mainLog) throws PrismException {
        
        this.entropyType = entropyType;
        this.mainLog = mainLog;
        
        if(!bounded)
            mainLog.println("\nExploring outputs ...\n");
        // explore outputs and compute output-secret probabilities 
        expModel = new ProbModelOutputExplorer(probModel, initDistFileName);
        expModel.exploreModel(bounded, boundedStep);
        
        computeSecretOutCondProbs();
        
        if(!bounded)
            mainLog.println(outProbs.size() + " outputs found");
        
    }
    
    /**
     * 
     * @param outSecretDist contains output-secret probabilities: Pr(o=\bar{o}, h=\bar{h})
     */
    public void computeSecretOutCondProbs(){
        
        String out;
        Map<String, Double> outSecretDist;
        outProbs = new HashMap<>(); // Pr(o)
        
        secretOutCondProbs = expModel.getOutSecretDist(); // Pr(h,o)
        for(Map.Entry<String, Map<String, Double>> entry: secretOutCondProbs.entrySet()){
            
            out = entry.getKey();
            outSecretDist = entry.getValue(); // pr(h|o=out)
            
            double outProb = outSecretDist.values().stream().mapToDouble(d->d).sum(); // Pr(o=out)
            outSecretDist.replaceAll((k, v) -> v/outProb);
            
            outProbs.put(out, outProb);
        }
    }
    
    /**
     * Compute final leakage. 
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
     * Compute remaining uncertainty H(h|o)
     * 
     * @return remaining uncertainty H(h|o)
     */
    public double remainingUncertainty() throws PrismException {
        
        String out;
        Map<String, Double> outSecretDist;
        double outProb, outFinalEntropy;
        
        double remaining_uncertainty = 0; // H(h|o)
        for(Map.Entry<String, Double> entry: outProbs.entrySet()){
            
        	out = entry.getKey();
        	outProb = entry.getValue(); // Pr(o=out)
        	outSecretDist = secretOutCondProbs.get(out); // Pr(h|o=out)
        	outFinalEntropy = entropy(outSecretDist); // H(h|o=out)
            
            remaining_uncertainty += outProb * outFinalEntropy;     
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
     * Compute maximum leakage. 
     * 
     * @return maximum leakage
     */
    public double maxLeakage(){
        
        // compute minimum posterior entropy: min(H(h|o=out)) for all outputs out
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
        
        // compute maximum posterior entropy: max(H(h|o=out)) for all outputs out
        double maximumEntropy = maximumEntropy();
        double initialUncertainty = initialUncertainty();
        double leakage = initialUncertainty - maximumEntropy;
        return leakage;
    }
    
    /**
     * Compute probability of maximum leakage of state_machine. 
     * 
     * @return probability of maximum leakage
     */
    public double probMaxLeakage(){
        
        // compute minimum posterior entropy 
        double minimumEntropy = minimumEntropy();
        
        // compute probability of maximum leakage
        String out;
        Map<String, Double> outSecretDistribution;
        double outFinalEntropy;
        
        double probMaxLeakage = 0;
        for(Map.Entry<String, Double> entry: outProbs.entrySet()){
            
        	out = entry.getKey();
        	outSecretDistribution = secretOutCondProbs.get(out);
            
        	outFinalEntropy = entropy(outSecretDistribution);
            if(minimumEntropy == outFinalEntropy)
                probMaxLeakage += outProbs.get(out);
        }

        return probMaxLeakage;
    }
    
    /**
     * Compute probability of minimum leakage of state_machine. 
     * 
     * @return probability of minimum leakage
     */
    public double probMinLeakage(){
        
        // compute maximum posterior entropy of outputs
        double maximumEntropy = maximumEntropy();
        
        // compute probability of minimum leakage
        String out;
        Map<String, Double> outSecretDistribution;
        double outFinalEntropy;
        
        double probMinLeakage = 0;
        for(Map.Entry<String, Double> entry: outProbs.entrySet()){
            
        	out = entry.getKey();
        	outSecretDistribution = secretOutCondProbs.get(out);
            
        	outFinalEntropy = entropy(outSecretDistribution);
            if(maximumEntropy == outFinalEntropy)
                probMinLeakage += outProbs.get(out);
        }

        return probMinLeakage;
    }
    
    /**
     * 
     * @return maximum posterior entropy (Shannon or min-entropy): max(H(h|o=out)) for all outputs out
     */
    public double maximumEntropy() {
        
        String out;
        Map<String, Double> outSecretDistribution;
        double outFinalEntropy;
        
        double maximumEntropy = -1;
        for(Map.Entry<String, Double> entry: outProbs.entrySet()){
            
        	out = entry.getKey();
        	outSecretDistribution = secretOutCondProbs.get(out);
        
        	outFinalEntropy = entropy(outSecretDistribution);
            if(maximumEntropy < outFinalEntropy)
                maximumEntropy = outFinalEntropy;
        }
        
        return maximumEntropy;
    }
    
    /**
     * 
     * @return minimum posterior entropy (Shannon or min-entropy): min(H(h|o=out)) for all outputs out
     */
    public double minimumEntropy() {
        
        // compute minimum posterior entropy
        String out;
        Map<String, Double> outSecretDistribution;
        double outFinalEntropy;
        
        double minimumEntropy = Double.MAX_VALUE;
        for(Map.Entry<String, Double> entry: outProbs.entrySet()){
            
        	out = entry.getKey();
            outSecretDistribution = secretOutCondProbs.get(out);
        
            outFinalEntropy = entropy(outSecretDistribution);
            if(minimumEntropy > outFinalEntropy)
                minimumEntropy = outFinalEntropy;
        }
        return minimumEntropy;
    }
    
    /**
     * 
     * @return Shannon or min-entropy of distribution
     */
    public double entropy(Map<String, Double> distribution) {
        
        if(entropyType == MIN_ENTROPY)
            return InterLeakComputerExp.minEntropy(distribution);
        else // SHANNON_ENTROPY
            return InterLeakComputerExp.shannonEntropy(distribution);
    }
    
    public void printModelInfo(String modelFilename) {
        String out;
        Map<String, Double> dist;
        String modelInfoFileName = modelFilename.substring(0, modelFilename.lastIndexOf('.'));

        try (PrintWriter outFile = new PrintWriter(modelInfoFileName)) {
            outFile.println(secretOutCondProbs.size());
            for(Map.Entry<String, Map<String, Double>> entry: secretOutCondProbs.entrySet()){
                
                out = entry.getKey();
                dist = entry.getValue();
                outFile.println(out + ":" + dist);
      
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

