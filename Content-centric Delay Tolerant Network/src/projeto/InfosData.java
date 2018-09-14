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

public class InfosData {
		
	public static Map<String, Infos> contentsList = new HashMap<String, Infos>();
    
	/** Total number of data delivered*/
	public static int nrDatasDelivered = 0;
	
	/** Total number of data cached*/
	public static int nrDatasCached = 0;
	
	public static Map<String, Infos> getContentsList() {
		return contentsList;
	}
	public static void setContentsList(String name, DTNHost host) {
		String id = host.getAddress()+"_"+name+"_"+SimClock.getTime();
		Infos info = new Infos();
		info.id = id;
		info.timeCreated = SimClock.getTime();
		info.isSentOut = true;
		info.timeSentOut = SimClock.getTime();
		contentsList.put(id, info);
	}
	public static int getNrDatasDelivered() {
		return nrDatasDelivered;
	}
	public static void setNrDatasDelivered() {
		nrDatasDelivered++;
	}
	public static int getNrDatasCached() {
		return nrDatasCached;
	}
	public static void setNrDatasCached() {
		nrDatasCached++;
	}
}
