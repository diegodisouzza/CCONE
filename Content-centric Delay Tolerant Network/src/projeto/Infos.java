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

public class Infos {

	public String id; 
	public String name;
	
	public Map<DTNHost, Double> relayers;
	
	public double timeSentOut; 
	public double timeDelivered; 
	public double timeCreated; 
	public double timeSatisfied;
	
	public boolean isDelivered;
	public boolean isSatisfied;
	public boolean isSentOut;

	
	public Infos()
	{
		id = new String();
		timeSentOut = 0;
		timeDelivered = 0;
		isDelivered = false;
		isSatisfied = false;
		isSentOut = false;
		relayers = new HashMap<DTNHost,Double>();
	}

}
