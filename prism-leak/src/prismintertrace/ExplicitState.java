package prismintertrace;

/**
*
* @author Ali A. Noroozi
*/

public class ExplicitState {
	
	protected long stateNum; // state number
	protected String publicData; // values of observable variables in this state   
    private String secretData; // values of secret variables in this state
    protected double reachabilityProbability; // reachability probability of this state
   
    public ExplicitState(long stateNum, String publicData, String secretData) {
    	
    	this(stateNum, publicData, secretData, -1);
    }
    
    public ExplicitState(long stateNum, String publicData, String secretData, double reachabilityProbability) {
    	this.stateNum = stateNum;
		this.publicData = publicData;
		this.secretData = secretData;
		this.reachabilityProbability = reachabilityProbability;
	}
    
    /**
     * 
     * @return state number
     */
   public long getStateNumber() {
        return stateNum;
    }
    
    /**
     * If varIndex is set to -1, return public data of all variables, 
	 * else return value (public data) of variable in varIndex 
     * 
     * @return public data of this state
     */
    public String getPublicData(int varIndex) {
    	if (varIndex > -1)
    		return publicData.split("-")[varIndex];
    	
        return publicData;
    }    
    
    /**
     * 
     * @return public data of this state
     */
    public String getSecretData() {
        return secretData;
    }
    
    /**
     * 
     * @return reachability probability of this state
     */
    public double getReachabilityProbability() {
		return reachabilityProbability;
	}

    public String toString() {
        return Long.toString(stateNum);
    }

}
