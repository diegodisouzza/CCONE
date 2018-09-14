/* 
 /* 
 * Copyright 2016 Universidade Federal do Estado do Rio de Janeiro, RJ, Brasil
 * Modified by Claudio Diego T. de Souza.
 * Released under GPLv3. See LICENSE.txt for details. 
 */

package projeto;

import java.util.HashMap;
import java.util.Map;

import core.DTNHost;
import core.SimClock;

public class InfosInterest {

	/** List of interests*/
	public static Map<String, Infos> interestsList = new HashMap<String, Infos>();

	/** Total number of interest delivered*/
	public static int nrInterestsDelivered = 0;
	
	/** Total number of interests send out*/
	public static int nrInterestsSentOut = 0;
    
	/** List of interests satisfied on PIT*/
	public static Map<Double, String> interestsSatisfiedOnPit = new HashMap<Double, String>();
	
	/** List of interests satisfied in Nodes*/
	public static Map<Double, String> interestsSatisfiedInNodes = new HashMap<Double, String>();
    
    /** Total number of interests satisfied locally*/
	public  static int nrInterestsSatisfiedinNode = 0;
    
    /** Total number of interest messages created*/
	public  static int nrInterestsCreated = 0;
    
	public static Map<String, Infos> getInterestsList() {
		return interestsList;
	}

	public static void setInterestsList(String id) {
		if(!interestsList.containsKey(id))
		{
			Infos info = new Infos();
			info.id = id; 
			info.timeCreated = SimClock.getTime();
			interestsList.put(id, info);
			setNrInterestsCreated();
		}
	}
	
	public static void setSatisfied(String id, boolean isSatisfied) {
		if(interestsList.containsKey(id)){
			interestsList.get(id).isSatisfied = isSatisfied;
			interestsList.get(id).timeSatisfied = SimClock.getTime();
		}
	}

	public static void setInterestRelayed(String id, DTNHost host) {
		if(interestsList.containsKey(id))
			interestsList.get(id).relayers.put(host,SimClock.getTime());
	}

	public static int getNrInterestsDelivered() {
		return nrInterestsDelivered;
	}

	public static void setNrInterestsDelivered(String id) {
		if(!interestsList.containsKey(id))
			setInterestsList(id);
		if(!interestsList.get(id).isDelivered)
		{
			interestsList.get(id).isDelivered = true;
			interestsList.get(id).timeDelivered = SimClock.getTime();
			nrInterestsDelivered++;
		}
	}

	public static int getNrInterestsSentOut() {
		return nrInterestsSentOut;
	}

	public static void setNrInterestsSentOut(String id) {
		if(!interestsList.containsKey(id))
			setInterestsList(id);
		if(!interestsList.get(id).isSentOut)
		{
			interestsList.get(id).isSentOut = true;
			interestsList.get(id).timeSentOut = SimClock.getTime();
			nrInterestsSentOut++;
		}
	}
	
	public static Map<Double, String> getInterestsSatisfiedOnPit(String id) {
		return interestsSatisfiedOnPit;
	}
	
	public static void setInterestsSatisfiedOnPit(String id) {
		interestsSatisfiedOnPit.put(SimClock.getTime(), id);
	}
	

	public static Map<Double, String> getInterestsSatisfiedInNodes(String id) {
		return interestsSatisfiedInNodes;
	}
	
	public static void setInterestsSatisfiedInNodes(String id) {
		interestsSatisfiedInNodes.put(SimClock.getTime(), id);
	}

	public static void setNrInterestsSatisfiedinNode() {
		nrInterestsSatisfiedinNode++;
	}

	public static int getNrInterestsCreated() {
		return nrInterestsCreated;
	}

	public static void setNrInterestsCreated() {
		nrInterestsCreated++;
	}

	public static Map<Double, String> getInterestsRelayed() {
		return interestsSatisfiedOnPit;
	}

}
