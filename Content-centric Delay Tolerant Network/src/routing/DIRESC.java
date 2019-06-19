/* 
/* 
* Copyright 2016 Universidade Federal do Estado do Rio de Janeiro, RJ, Brasil
* DIRESC: Protocolo de Descoberta e Recuperacao de Conteudo baseado em Cooperacao Social
* DIRESC: Content DIscovery and REtrieval protocol based on Social Cooperation
* Modified by Claudio Diego T. de Souza.
* Released under GPLv3. See LICENSE.txt for details. 
*/
package routing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import core.Connection;
import core.DTNHost;
import core.Message;
import core.Settings;
import core.SimClock;
import core.Tuple;
import projeto.SocialTie;

public class DIRESC extends CCNArchitecture {
	
	/**The table that keeps the time stamp of encounter between nodes e.g. <host, time stamps>*/
	protected Map<DTNHost, ArrayList<Double>> encounterTable = new HashMap<DTNHost, ArrayList<Double>>();
	
	/**The table that keeps the social tie information between nodes e.g. <host, social tie information>*/
	protected Map<DTNHost, SocialTie> socialTieTable = new HashMap<DTNHost, SocialTie>();
	
	/**The priority stack to classify the message sending*/
	private final int priority[] = {1,2,3,4,5}; // 1-CS; 2-FIB; 3-this.PIT; 4-other.PIT; 5-ST(social tie)

	/**Social relationship value average of a node to check friendship*/
	protected double avgRelationship;
	protected double densityFactor = 1/3;

	/**The list of interest messages to remove from buffer when data packages are received*/
	protected ArrayList<String> interestsToRemove = new ArrayList<String>();

	/**Constructor for DIRESC
	 * @param s Settings*/
	public DIRESC(Settings s) {
		super(s);
	}
	
	/**Constructor for DIRESC
	 * @param obj DIRESC*/
	public DIRESC(DIRESC obj) {
		super(obj);
	}
	
	/**When a connection is up, it records the encounter time stamp, 
	 * computes the social relationship and the social relationship value average
	 * @param con Connection*/
	@Override
	public void changedConnection(Connection con)
	{
		if(con.isUp()) 
		{
			DTNHost otherHost = con.getOtherNode(this.getHost());
			recordTimestamp(otherHost);
			computeSocialRelationship(otherHost);
			computeAvgRelationship();
			
			announcement(otherHost);
		}
	}
	
	/**It records the encounter time stamp of two nodes
	 * @param otherHost encountered host
	 * */
	public void recordTimestamp(DTNHost otherHost)
	{		
		if(encounterTable.containsKey(otherHost))
			encounterTable.get(otherHost).add(SimClock.getTime());
		else
		{
			ArrayList<Double> times = new ArrayList<Double>();
			times.add(SimClock.getTime());
			encounterTable.put(otherHost, times);
		}

	}

	/**It computes the social relationship value between two nodes
	 * @param otherHost social relationship host
	 * */
	public void computeSocialRelationship(DTNHost otherHost)
	{
		double RiJ = 0.0;
		double x;
		double tBase = SimClock.getTime(); 
		
		if(this.encounterTable.containsKey(otherHost))
		{
			ArrayList<Double> times = this.encounterTable.get(otherHost);
			
			for(Double pastTime : times)
			{
				x =  (tBase - pastTime);
				RiJ = RiJ + F(x);
			}
		}
		
		if(this.socialTieTable.containsKey(otherHost)){
			this.socialTieTable.get(otherHost).setValue(RiJ);
			this.socialTieTable.get(otherHost).settBase(tBase);
		}else{
			SocialTie st= new SocialTie(RiJ, tBase, this.getHost(), otherHost);
			this.socialTieTable.put(otherHost, st);
		}		
	}
	
	/**Weighting function to compute social relationship
	 * @param x value of this time stamp minus encounter time stamp in the past
	 * */
	private double F(double x)
	{
		double lamb = 0.0001; // lamb = 1/(e^4) = 0.018315
		return Math.pow(0.5, (lamb*x)); //(1/2)^(lamb*x)
	}
	
	/**It computes the social relationship value average of some node*/
	public void computeAvgRelationship()
	{
		double sum = 0, total = 0;
		
		for(SocialTie st : this.socialTieTable.values())
		{
			sum += st.getValue();
			total++;
		}
		
		if(total > 0)
			this.avgRelationship = densityFactor * (sum/total);
		else
			this.avgRelationship = 0;
	}
	
	public void announcement(DTNHost otherHost) {
		double socialValue = this.socialTieTable.containsKey(otherHost) ? this.socialTieTable.get(otherHost).getValue() : 0.0;
		
		if (socialValue >= avgRelationship) {
			
		}
		
	}
	
	/**Attempts to send interest packages to other hosts*/
	public void sendInterest()
	{
		ArrayList<Tuple<Message, Connection>> outgoingMessages = new ArrayList<Tuple<Message, Connection>>();
		TreeMap<Integer, Tuple<Message, Connection>> outgoingToOrder = new TreeMap<Integer, Tuple<Message, Connection>>();
		
		for(Connection con : this.getConnections())
		{
			DTNHost otherHost = con.getOtherNode(getHost());
			DIRESC otherRouter = (DIRESC)otherHost.getRouter();
			double socialValue = this.socialTieTable.containsKey(otherHost) ? this.socialTieTable.get(otherHost).getValue() : 0.0;
			
			for(Message m : this.getMessageCollection())
			{
				CCNMessage message = this.messageFields(m);
				
				if(message.getType().equals(INTEREST_PACKAGE))
				{
					if(m.getFrom().equals(this.getHost()) && !this.PIT.containsKey(message.getName()))
						updatePIT(m, m.getFrom());
					
					if(this.PIT.containsKey(message.getName()))
					{
						if(otherRouter.hasMessage(message.getName()) 
								|| otherRouter.deliveredMessages.containsKey(message.getName()))
						{
							outgoingToOrder.put(this.priority[0], new Tuple<Message, Connection>(m,con));
						}
						else if(!this.PIT.get(message.getName()).sentToCluster() 
								&& ((this.FIB.containsKey(message.getName()) 
								&& this.FIB.get(message.getName()).contains(otherHost)) 
								|| otherRouter.FIB.containsKey(message.getName())))
						{
							outgoingToOrder.put(this.priority[1], new Tuple<Message, Connection>(m,con));
						}
						else if(this.avgRelationship > 0.0 && socialValue >= this.avgRelationship 
								&& !this.PIT.get(message.getName()).sentToFriends())
						{
							outgoingToOrder.put(this.priority[4], new Tuple<Message, Connection>(m,con));
						}
					}
				}
			}
			
			if(!outgoingToOrder.isEmpty())
			{
				for(Tuple<Message, Connection> t : outgoingToOrder.values())
					outgoingMessages.add(t);
				
				Tuple<Message, Connection> tupleSent = tryMessagesForConnected(outgoingMessages);
				
				if(tupleSent != null)
				{
					Message m = tupleSent.getKey();
					CCNMessage message = this.messageFields(m);
					
					if(!this.PIT.get(message.getName()).sentToCluster() 
							&& ((this.FIB.containsKey(message.getName()) 
							&& this.FIB.get(message.getName()).contains(otherHost)) 
							|| otherRouter.FIB.containsKey(message.getName())))
					{
						this.PIT.get(message.getName()).setSentToCluster(true);
					}
					else if(this.avgRelationship > 0.0 && socialValue >= this.avgRelationship 
							&& !this.PIT.get(message.getName()).sentToFriends())
					{
						this.PIT.get(message.getName()).setSentToFriends(true);
					}
					
					if(m.getFrom().equals(this.getHost()))
						this.PIT.get(message.getName()).setSent(true);
					else if(this.nonces.contains(m.getId()) && this.PIT.get(message.getName()).sentToFriends() 
							&& this.PIT.get(message.getName()).sentToCluster())
					{
						this.deleteMessage(m.getId(), true);
					}
				}
			}
		}
	}
	
	/**Attempts to send data packages to other host*/
	private void sendData()
	{
		List<Tuple<Message, Connection>> outgoingMessages = new ArrayList<Tuple<Message, Connection>>();
		TreeMap<Integer, Tuple<Message, Connection>> outgoingToOrder = new TreeMap<Integer, Tuple<Message, Connection>>();
		
		ArrayList<String> entriesToRemove = new ArrayList<>();
		
		for(Connection con : this.getConnections())
		{
			DTNHost otherHost = con.getOtherNode(getHost());
			DIRESC otherRouter = (DIRESC)otherHost.getRouter();
			
			for(PITEntry entry : this.PIT.values())
			{
				if(this.hasMessage(entry.getName()))
				{
					Message m = this.getMessage(entry.getName());
					CCNMessage message = this.messageFields(m);
					
					if(message.getType().equals(CONTENT_PACKAGE))
					{
						if(!otherRouter.PIT.containsKey(message.getName()))
						{
							this.PIT.get(message.getName()).removeHost(otherHost);
							if(!this.PIT.get(message.getName()).moreRequesters())
								entriesToRemove.add(message.getName());
						}
													
						if(this.PIT.containsKey(message.getName()) && this.PIT.get(message.getName()).containsHost(otherHost))
						{
							outgoingToOrder.put(this.priority[2], new Tuple<Message, Connection>(m,con));
						}
						else if(otherRouter.PIT.containsKey(message.getName()) && otherRouter.PIT.get(message.getName()).isSent())
						{
							outgoingToOrder.put(this.priority[3], new Tuple<Message, Connection>(m,con));
						}
						else if(otherRouter.socialTieTable.containsKey(m.getTo()) 
								&& otherRouter.socialTieTable.get(m.getTo()).getValue() >= otherRouter.avgRelationship)
						{
							outgoingToOrder.put(this.priority[4], new Tuple<Message, Connection>(m,con));
						}
					}
				}
			}
			
			if(!outgoingToOrder.isEmpty())
			{
				for(Tuple<Message, Connection> t : outgoingToOrder.values())
					outgoingMessages.add(t);
				
				Tuple<Message, Connection> tupleSent = tryMessagesForConnected(outgoingMessages);
				
				if(tupleSent != null)
				{		
					Message m = tupleSent.getKey(); 
					DTNHost other = tupleSent.getValue().getOtherNode(this.getHost());
					CCNMessage message = this.messageFields(m);
					
					if(this.PIT.containsKey(message.getName()))
					{
						this.PIT.get(message.getName()).removeHost(other);
						if(!this.PIT.get(message.getName()).moreRequesters())
							deletePITEntry(message.getName());
					}						
				}
			}
			
			for(String name : entriesToRemove)
			{
				this.deletePITEntry(name);
			}
		}
	}
	
	/** After receiving a package, a node verifies if it's a interest or a data package
	 * @param id of the transferred message
	 * @param from host the message was from (previous hop)*/
	@Override
	public Message messageTransferred(String id, DTNHost from)
	{
		Message m = super.messageTransferred(id, from);
		CCNMessage message = this.messageFields(m);
		
		if(message.getType().equalsIgnoreCase(INTEREST_PACKAGE))
			onInterest(m, message.getName(), from);
		else if(message.getType().equalsIgnoreCase(CONTENT_PACKAGE))
			onData(m, message.getName(), from);
		
		return m;
	}
	
	/** Processing of interest packages. Here the packet is analyzed
	 * and the conditions for acceptance of new interest are checked. 
	 * If exists any content that satisfies the interest, then it is returned immediately, 
	 * otherwise the PIT is added a new entry with the conditions.
	 * @param m the message
	 * @param name the content name
	 * @param from the sender (previous hop)
	 */
	public void onInterest(Message m, String name, DTNHost from)
	{		
		double socialValue = 0.0;
		if(this.socialTieTable.containsKey(from))
			socialValue = this.socialTieTable.get(from).getValue();
		
		Message c = null;
		
		if(this.hasMessage(name))
		{  
			c = this.getMessage(name);
		}
		else if(this.deliveredMessages.containsKey(name))
		{
			c = this.deliveredMessages.get(name);
			this.addToMessages(c, true);
		}	
		else if(this.FIB.containsKey(name) || (this.avgRelationship > 0 && socialValue >= this.avgRelationship))
		{
			this.updatePIT(m, from);	
		}
		else
			this.deleteMessage(m.getId(), true);
		
		if(c != null)
		{
			this.updatePIT(m, from);
			
			this.getMessage(c.getId()).setTo(m.getFrom()); 
			
			this.sendData();
			
			if(this.hasMessage(m.getId()))
				this.deleteMessage(m.getId(), false);
		}
		
		this.ccnMessageTransferredListener(m, from, c != null);
	}
	
	/**Processing of data packages. If the node is subscriber data is 
	 * added to the repository. If not are just kept in buffer temporarily.
	 * @param m the message
	 * @param name the content name
	 * @param from the sender (previous hop)
	 */
	public void onData(Message m, String name, DTNHost from)
	{
		this.removeInterest(name);
		
		this.updateFIB(m, from);
		
		boolean finalTarget = false;
		
		if(this.PIT.containsKey(name))
		{
			PITEntry entry = this.PIT.get(name);
			finalTarget = entry.isRequester();
			if(finalTarget)
				this.deliveredMessages.put(m.getId(), m); 
			
			this.PIT.get(name).removeHost(this.getHost());
			if(!entry.moreRequesters())
				this.deletePITEntry(name);
		}
		else if(!this.socialTieTable.containsKey(m.getTo()) 
				|| this.avgRelationship < 0 || (this.socialTieTable.containsKey(m.getTo()) 
				&& this.socialTieTable.get(m.getTo()).getValue() < this.avgRelationship))
			this.deleteMessage(m.getId(), true);
		
		this.ccnMessageTransferredListener(m, from, finalTarget);
	}
	
	/**Called when this node receives a data and possesses pending interest messages.
	 * A message of interest is eliminated
	 * @param name is the name of content;
	 */	
	public void removeInterest(String name)
	{
		for (Message m : this.getMessageCollection()) 
		{
			CCNMessage message = this.messageFields(m);
				
			if(message.getName().equalsIgnoreCase(name))
			{
				if(message.getType().equalsIgnoreCase(INTEREST_PACKAGE))
					this.interestsToRemove.add(m.getId());
			}
		}
		for (String id : this.interestsToRemove)
			this.deleteMessage(id, false);
		this.interestsToRemove.clear();
	}
	
	@Override
	public RoutingInfo getRoutingInfo() {
		RoutingInfo ri = super.getRoutingInfo();
		RoutingInfo fib = new RoutingInfo(this.FIB.size() + " FIB's size");
		RoutingInfo pit = new RoutingInfo(this.PIT.size() + " PIT's size");
		ri.addMoreInfo(fib);
		ri.addMoreInfo(pit);
		
		for (PITEntry entry : this.PIT.values()) {
			pit.addMoreInfo(new RoutingInfo(entry.getName() + " requesters:" + entry.getHosts()));
		}
		
		for (String name : this.FIB.keySet()){
			fib.addMoreInfo(new RoutingInfo(name + " provider path:" + this.FIB.get(name)));
		}
		return ri;
	}

	@Override
	public MessageRouter replicate() {
		return new DIRESC(this);
	}
	
	@Override
	public void update() {
		super.update();
		if (this.isTransferring() || !this.canStartTransfer()) {
			return;
		}
		this.sendInterest(); 
		this.sendData();
	}
}
