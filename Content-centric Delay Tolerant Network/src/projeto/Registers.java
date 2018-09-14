/* 
 /* 
 * Copyright 2016 Universidade Federal do Estado do Rio de Janeiro, RJ, Brasil
 * Modified by Claudio Diego T. de Souza.
 * Released under GPLv3. See LICENSE.txt for details. 
 */

package projeto;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import core.DTNHost;
import core.SimClock;

public class Registers
{	
	private static double interval = 3600;
	
	private static int nrofHosts = 100;

	private static double nextimeRegist = interval;
	
	public static String register = new String(); 
	
	public static void print()
	{
		double time = SimClock.getTime();
		if(time>=nextimeRegist)
		{
			DecimalFormat formato = new DecimalFormat("#.##");
			int nrAdsMade = adsMadeInterval();
			int nrIntCreated = interestsCreatedInterval();		//ok
			int nrIntSentOut = interestsSentInterval();			//ok
			int nrIntDelivered = interestsDeliveredInterval();	//ok
			int nrIntRelayed = nrInterestsRelayedInterval();	//ok
			int nrSatisfiedPIT = interestsSatisfiedOnPitInterval();//ok
			int nrSatisfiedOnReq = nrSatisfiedOnRequesterInterval();//ok
			int nrDataSentOut = dataSentInterval();				//ok
			int nrDataDelivered = InfosData.nrDatasDelivered;	//ok
			int nrDataCached = InfosData.nrDatasCached;			//ok
			int nrRelayersInterval = nrofRelayersInterval();				//ok
			double avgIntRelayedPerNode = Double.valueOf(formato.format(avgIntRelayedPerNode())); //ok
			double dpIntRelayedPerNode = Double.valueOf(formato.format(dpIntRelayedPerNode(avgIntRelayedPerNode))); //ok
			double avgDelaySatisfaction = Double.valueOf(formato.format(avgDelaySatisfaction())); //ok
			double satisfactionRate = Double.valueOf(formato.format(satisfactionRate()));//ok
					
			String line = nextimeRegist+"|"
					+nrAdsMade+"|"
					+nrIntCreated+"|"
					+nrIntSentOut+"|"
					+nrIntDelivered+"|"
					+nrIntRelayed+"|"
					+nrSatisfiedPIT+"|"
					+nrSatisfiedOnReq+"|"
					+nrDataSentOut+"|"
					+nrDataDelivered+"|"
					+nrDataCached+"|"
					+nrRelayersInterval+"|"
					+avgIntRelayedPerNode+"|"
					+dpIntRelayedPerNode+"|"
					+avgDelaySatisfaction+"|"
					+satisfactionRate+"\n";
			
			register += line;
			
			String line2 = "|interval: "+(int)(nextimeRegist/interval)+
					"|ads made: "+nrAdsMade+
					"|interests created: "+nrIntCreated+
					"|interests sentout: "+nrIntSentOut+
					"|interests delivered: "+nrIntDelivered+
					"|interests relayed: "+nrIntRelayed+
					"\n|satisfied on PIT: "+nrSatisfiedPIT+
					"|satisfied on requester: "+nrSatisfiedOnReq+
					"|contents sentout: "+nrDataSentOut+
					"|contents delivered: "+nrDataDelivered+
					"|contents cached: "+nrDataCached+
					"\n|relayers: "+nrRelayersInterval+
					"|avg interest relayed per node: "+avgIntRelayedPerNode+
					"|dp interest relayed per node: "+dpIntRelayedPerNode+
					"|avg delay: "+avgDelaySatisfaction+
					"|satisfaction rate: "+satisfactionRate+"%\n";
			
			System.out.println(line2);
			nextimeRegist = nextimeRegist+interval;
		}
	}
	
	private static int adsMadeInterval()
	{
		int adMadeInterval = 0;
		for(double time : InfosAd.adsList.keySet())
		{
			if(time>=nextimeRegist-interval && time <= nextimeRegist)
				adMadeInterval++;
		}
		return adMadeInterval;
	}
	
	private static int interestsCreatedInterval()
	{
		int interestCreatedInterval = 0;
		for(Infos info : InfosInterest.interestsList.values())
		{
			if(info.timeCreated>=nextimeRegist-interval && info.timeCreated <= nextimeRegist)
				interestCreatedInterval++;
		}
		return interestCreatedInterval;
	}
	
	private static int interestsSentInterval()
	{
		int interestSentInterval = 0;
		for(Infos info : InfosInterest.interestsList.values())
		{
			if(info.isSentOut && info.timeSentOut>=nextimeRegist-interval && info.timeSentOut <= nextimeRegist)
				interestSentInterval++;
		}
		return interestSentInterval;
	}
	
	private static int interestsDeliveredInterval()
	{
		int interestDeliveredInterval = 0;
		for(Infos info : InfosInterest.interestsList.values())
		{
			if(info.isDelivered && info.timeDelivered>=nextimeRegist-interval && info.timeDelivered <= nextimeRegist)
				interestDeliveredInterval++;
		}
		return interestDeliveredInterval;
	}
	
	private static int dataSentInterval()
	{
		int dataSentInterval = 0;
		for(Infos info : InfosData.contentsList.values())
		{
			if(info.timeSentOut>=nextimeRegist-interval && info.timeSentOut <= nextimeRegist)
				dataSentInterval++;
		}
		return dataSentInterval;
	}
	
	//nr of relays of interests in interval
	private static int nrInterestsRelayedInterval()
	{
		int nrofRelays = 0;
		for(Infos info : InfosInterest.interestsList.values())
		{
			for(Double timeRelayed : info.relayers.values())
			{
				if(timeRelayed>=nextimeRegist-interval && timeRelayed<=nextimeRegist)
					nrofRelays++;
			}
		}
		return nrofRelays;
	}
	
	private static int interestsSatisfiedOnPitInterval()
	{
		int interestSatisfiedOnPitInterval = 0;
		for(Double timeSatisfied : InfosInterest.interestsSatisfiedOnPit.keySet())
		{
			if(timeSatisfied>=nextimeRegist-interval && timeSatisfied <= nextimeRegist)
				interestSatisfiedOnPitInterval++;
		}
		return interestSatisfiedOnPitInterval;
	}
	
	private static int nrSatisfiedOnRequesterInterval()
	{
		int nrSatisfiedOnReq = 0;
		for(Infos info : InfosInterest.interestsList.values())
		{
			if(info.isSatisfied && info.timeSatisfied>=nextimeRegist-interval && info.timeSatisfied <= nextimeRegist)
				nrSatisfiedOnReq++;
		}
		return nrSatisfiedOnReq;
	}
	
	//nr of relayers in interval
	private static int nrofRelayersInterval()
	{
		ArrayList<DTNHost> relayers = new ArrayList<DTNHost>();
		
		for(Infos info : InfosInterest.interestsList.values())
		{
			for(DTNHost host : info.relayers.keySet())
			{
				if(!relayers.contains(host) && info.relayers.get(host)>=nextimeRegist-interval 
						&& info.relayers.get(host)<=nextimeRegist)
					relayers.add(host);
			}
		}
		
		return relayers.size();
	}
	
	//avg of interests relayed per node in interval
	private static double avgIntRelayedPerNode()
	{
		int totalRelays = 0;
		double avg = 0;
		Map<DTNHost,Integer> relayers = new HashMap<DTNHost, Integer>();
		
		for(Infos info : InfosInterest.interestsList.values())
		{
			for(DTNHost host : info.relayers.keySet())
			{
				if(info.relayers.get(host)>=nextimeRegist-interval && info.relayers.get(host)<=nextimeRegist)
				{
					if(!relayers.containsKey(host))
						relayers.put(host, 1);
					else
						relayers.put(host, relayers.get(host)+1);
					
					totalRelays++;
				}
			}
		}
	
		if(totalRelays>0)
			avg = (double)totalRelays/nrofHosts;
		
		return avg;
	}
	
	//dp of interests relayed per node in interval
	private static double dpIntRelayedPerNode(double avg)
	{
		double sum = 0;
		double dp = 0;
		int total = 0;
		Map<DTNHost,Integer> relayers = new HashMap<DTNHost, Integer>();
		
		for(Infos info : InfosInterest.interestsList.values())
		{
			for(DTNHost host : info.relayers.keySet())
			{
				if(info.relayers.get(host)>=nextimeRegist-interval && info.relayers.get(host)<=nextimeRegist)
				{
					if(!relayers.containsKey(host))
						relayers.put(host, 1);
					else
						relayers.put(host, relayers.get(host)+1);
				}
			}
		}
		
		for(int i = relayers.size(); i<=100; i++)
		{
			relayers.put(null, 0);
		}
	
		for(int totalPerNode : relayers.values())
		{
			sum+=Math.pow((totalPerNode - avg), 2);
			total++;
		}
		
		if(sum>0)
			dp = Math.sqrt((double)sum / total);
		return dp;
	}
	
	//avg of delay satisfaction from sending of interests to receiving of data 
	private static double avgDelaySatisfaction()
	{
		double avgDelay = 0;
		double sumDelay = 0;
		int nrInterestsSatisfied = 0;
		double timeSatisfied, timeSentOut, difference;
		
		for(Infos info : InfosInterest.interestsList.values())
		{
			if(info.isSatisfied && info.timeSatisfied>=nextimeRegist-interval && info.timeSatisfied<=nextimeRegist)
			{
				timeSatisfied = info.timeSatisfied;
				timeSentOut = info.timeSentOut;
				difference = timeSatisfied - timeSentOut;
				sumDelay += difference;
				nrInterestsSatisfied++;
			}
		}
		if(sumDelay>0 && nrInterestsSatisfied>0)
			avgDelay = sumDelay / nrInterestsSatisfied;
		else 
			avgDelay = 0;
		
		return avgDelay;
	}
	
	private static double satisfactionRate()
	{
		double interestsSentOut = 0;
		double interestsSatisfied = 0;
		
		for(Infos info : InfosInterest.interestsList.values())
		{
			if(info.isSentOut){
				interestsSentOut++;
			}
			if(info.isSatisfied){
				interestsSatisfied++;
			}
		}
		
		if(interestsSentOut > 0 && interestsSatisfied > 0)
			return (interestsSatisfied / interestsSentOut) * 100;
		else
			return 0;
	}

}
