package projeto;

import java.util.HashMap;
import java.util.Map;

import core.DTNHost;
import core.SimClock;

public class InfosAd {
	
	/** List of interests*/
	public static Map<Double, DTNHost> adsList = new HashMap<Double, DTNHost>();

	public static void setAdsList(DTNHost advertiser) {
		adsList.put(SimClock.getTime(), advertiser);
	}
	
	public static Map<Double, DTNHost> getAdsList() {
		return adsList;
	}

}
