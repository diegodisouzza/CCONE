///* 
///* 
//* Copyright 2016 Universidade Federal do Estado do Rio de Janeiro, RJ, Brasil
//* DIRESC: Protocolo de Descoberta e Recuperacao de Conteudo baseado em Cooperacao Social
//* DIRESC: Content DIscovery and REtrieval protocol based on Social Cooperation
//* Modified by Claudio Diego T. de Souza.
//* Released under GPLv3. See LICENSE.txt for details. 
//*/
//package routing;
//
//import java.util.ArrayList;
//import java.util.Collection;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//import java.util.TreeMap;
//
//import core.Connection;
//import core.DTNHost;
//import core.Message;
//import core.Settings;
//import core.SimClock;
//import core.Tuple;
//import projeto.SocialTie;
//
//public class DirescOld extends CcnArchitecture {
//	
//	/** Submission Limiter ({@value})**/
//	public static final String SubmissionLimitter = "submissionLimitter";
//	
//	/** Density factor ({@value})**/
//	public static final String DensityFactor = "densityFactor";
//
//	protected Map<DTNHost, ArrayList<Double>> encounterTable = new HashMap<DTNHost, ArrayList<Double>>(); //<host, time stamps>
//	protected Map<DTNHost, SocialTie> socialTieTable = new HashMap<DTNHost, SocialTie>();
//	
//	private final int PRIORITY[] = {1,2,3,4,5}; // 1-CS; 2-FIB; 3-this.PIT; 4-other.PIT; 5-ST(social tie)
//	
//	/** Submission Limiter*/
//	protected int submissionLimitter;
//	
//	/** Submission Limiter*/
//	protected double densityFactor;
//	
//	/** Threshold to check if two nodes are friends*/
//	protected double avgRelationship = 10.0;
//	
//	protected ArrayList<String> interestsToRemove = new ArrayList<String>();
//
//	/**Constructor 
//	 * @param s Settings*/
//	public DirescOld(Settings s) {
//		super(s);
//		Settings settings= new Settings(CCN_NS);
//		this.submissionLimitter = settings.getInt(SubmissionLimitter);
//		this.densityFactor = settings.getDouble(DensityFactor);
//	}
//	
//	/**Constructor 
//	 * @param obj DIRESC*/
//	public DirescOld(DirescOld obj) {
//		super(obj);
//		this.submissionLimitter = obj.submissionLimitter;
//		this.densityFactor = obj.densityFactor;
//	}
//	
//	/** Refresh informations about nodes encounters in social tie tables and send messages
//	 * @param con Connection*/
//	@Override
//	public void changedConnection(Connection con)
//	{
//		if (con.isUp()) 
//		{
//			DTNHost otherHost = con.getOtherNode(getHost());
//			/*refresh informations about nodes encounter*/
//			recordTimestamp(otherHost);
//			computeSocialRelationship(otherHost);
//			
//			computeAvgRelationship();
//		}
//	}
//	
//	/** Refresh the time stamp encounter of two nodes
//	 * @param host of encounter
//	 * */
//	public void recordTimestamp(DTNHost host)
//	{		
//		if (encounterTable.containsKey(host))
//		{
//			encounterTable.get(host).add(SimClock.getTime());
//		}
//		else
//		{
//			ArrayList<Double> times = new ArrayList<Double> ();
//			times.add(SimClock.getTime());
//			encounterTable.put(host, times);
//		}
//
//	}
//	
//	/** Compute the social relationship value
//	 * @param host of social relationship
//	 * */
//	public void computeSocialRelationship(DTNHost host)
//	{
//		double RiJ = 0.0;
//		double x;
//		double tBase = SimClock.getTime(); 
//		
//		if (this.encounterTable.containsKey(host))
//		{
//			ArrayList<Double> times = this.encounterTable.get(host);
//			
//			for (Double t : times)
//			{
//				x =  (tBase - t);
//				RiJ = RiJ + Fx(x);
//			}
//		}
//		
//		if(this.socialTieTable.containsKey(host)){
//			this.socialTieTable.get(host).setValue(RiJ);
//			this.socialTieTable.get(host).settBase(tBase);
//		}else{
//			SocialTie socialT= new SocialTie(RiJ, tBase, this.getHost(), host);
//			this.socialTieTable.put(host, socialT);
//		}		
//	}
//	
//	/** F(x) function for social relationship
//	 * @param x value of this time stamp minus last encounter's time stamp
//	 * */
//	private double Fx(double x)
//	{
//		double lamb = 0.0001; // lamb = 1/(e^4) = 0.018315
//		return Math.pow(0.5, (lamb*x)); //(1/2)^(lamb*x)
//	}
//	
//	/** Computes the social relationship average of some node
//	 * After, multiply this by the density factor */
//	public void computeAvgRelationship()
//	{
//		double sum = 0, avg = 0, total = 0;
//		
//		for(SocialTie st : this.socialTieTable.values())
//		{
//			sum += st.getValue();
//			total++;
//		}
//		avg = this.densityFactor*(sum/total);
//		this.avgRelationship = avg;
//	}
//	
//	/**Attempts to send Interest to other router
//	 * @param con Connection with the other host
//	 */
//	private void sendInterest()
//	{
//		Collection<Message> messages = this.getMessageCollection();
//		
//		List<Tuple<Message, Connection>> outgoingMessages = new ArrayList<Tuple<Message, Connection>>();
//		TreeMap<Integer, Tuple<Message, Connection>> outgoingToOrder = new TreeMap<Integer, Tuple<Message, Connection>>();
//		
//		for (Connection con : this.getConnections())
//		{
//			DTNHost other = con.getOtherNode(getHost());
//			DirescOld otherRouter = (DirescOld)other.getRouter();
//			double socialValue = 0.0;
//			
//			if(this.socialTieTable.containsKey(other))
//				socialValue = this.socialTieTable.get(other).getValue();
//
//			for (Message m : messages) 
//			{
//				String type = m.getData().split(" ")[0];
//				String name = m.getData().split(" ")[1];
//					
//				if(type.equalsIgnoreCase(INTEREST_PACKAGE))
//				{					
//										
//					if(!this.PIT.containsKey(name))
//					{
//						if(m.getFrom().equals(this.getHost()))
//						{
//							updatePIT(m, true, m.getFrom());
//						}
//					}
//						
//					if(this.PIT.containsKey(name))
//					{
//						/*if the other host has data*/
//						if(otherRouter.hasMessage(name) || otherRouter.deliveredMessages.containsKey(name))
//						{ 
//							outgoingToOrder.put(this.PRIORITY[0], new Tuple<Message, Connection>(m,con));
//						}
//						/* if the other node has knowledge of the content and number of retransmissions <= nr_relays_threshold*/
//						else if(this.PIT.get(name).toCluster < this.submissionLimitter &&
//								((this.FIB.containsKey(name) && this.FIB.get(name).contains(other)) || otherRouter.FIB.containsKey(name)))
//						{
//							outgoingToOrder.put(this.PRIORITY[1], new Tuple<Message, Connection>(m,con));
//							this.PIT.get(name).toCluster++;
//						}
//						/*if socialValue > friendship and the number of retransmissions <= nr_relays_threshold*/
//						else if(socialValue >= this.avgRelationship && this.PIT.get(name).toFriends < this.submissionLimitter)
//						{
//							outgoingToOrder.put(this.PRIORITY[4], new Tuple<Message, Connection>(m,con));
//							this.PIT.get(name).toFriends++;
//						}
//					}
//				}
//			}
//		}
//				
//		if(!outgoingToOrder.isEmpty())
//		{
//			for(Tuple<Message, Connection> t : outgoingToOrder.values())
//				outgoingMessages.add(t);
//			
//			Tuple<Message, Connection> tupleSent = tryMessagesForConnected(outgoingMessages);
//			
//			if(tupleSent!=null)
//			{	
//				Message m = tupleSent.getKey(); 
//				String name = m.getData().split(" ")[1];
//				
//				if(m.getFrom().equals(this.getHost()))
//				{
//					this.PIT.get(name).setSent(true);
//				}
//				else if(this.nonces.contains(m.getId()) && this.PIT.get(name).toFriends == this.submissionLimitter 
//				&& this.PIT.get(name).toCluster == this.submissionLimitter)
//				{
//					this.deleteMessage(m.getId(), true);
//				}
//			}
//		}
//	}
//
//	/**
//	 * Attempts send Data to other router
//	 * @return Tuple<Message, Connection>
//	 */
//	private void sendData()
//	{
//		Collection<Message> messages = this.getMessageCollection();
//		
//		List<Tuple<Message, Connection>> outgoingMessages = new ArrayList<Tuple<Message, Connection>>();
//		TreeMap<Integer, Tuple<Message, Connection>> outgoingToOrder = new TreeMap<Integer, Tuple<Message, Connection>>();
//		
//		for(Connection con : this.getConnections())
//		{
//			DTNHost other = con.getOtherNode(getHost());
//			DirescOld otherRouter = (DirescOld)other.getRouter();
//			
//			for (Message m : messages) 
//			{
//				String type = m.getData().split(" ")[0];
//				String name = m.getData().split(" ")[1];
//				
//				if(type.equalsIgnoreCase(CONTENT_PACKAGE)) 
//				{
//					/*if the other node is a requester to this one*/
//					if(this.PIT.containsKey(name) && this.PIT.get(name).containsHost(other)) 
//					{
//						if(!otherRouter.PIT.containsKey(name))
//						{
//							this.PIT.get(name).removeHost(other);
//							if(!this.PIT.get(name).moreRequesters())
//								this.deletePITEntry(name);
//						}
//						else
//							outgoingToOrder.put(this.PRIORITY[2], new Tuple<Message, Connection>(m,con));
//					}
//					/*if the node is a requester to the content*/
//					else if(otherRouter.PIT.containsKey(name) && otherRouter.PIT.get(name).isSent)
//					{
//						outgoingToOrder.put(this.PRIORITY[3], new Tuple<Message, Connection>(m,con));
//					}
//					/*if the other node has socialValue>THRESHOLD with the requester*/
//					else if(otherRouter.socialTieTable.containsKey(m.getTo()) 
//							&& otherRouter.socialTieTable.get(m.getTo()).getValue() >= otherRouter.avgRelationship)
//					{
//						outgoingToOrder.put(this.PRIORITY[4], new Tuple<Message, Connection>(m,con));
//					}
//				}		
//			}
//		}
//				
//		if (!outgoingToOrder.isEmpty())
//		{
//			for(Tuple<Message, Connection> t : outgoingToOrder.values())
//				outgoingMessages.add(t);
//			
//			Tuple<Message, Connection> tupleSent = tryMessagesForConnected(outgoingMessages);	// try to send messages
//		
//			if(tupleSent!=null)
//			{		
//				Message m = tupleSent.getKey(); 
//				Connection con = tupleSent.getValue();
//				DTNHost other = con.getOtherNode(this.getHost());
//				String name = m.getData().split(" ")[1];
//				
//				if(this.PIT.containsKey(name) && !this.PIT.get(name).moreRequesters())
//					deletePITEntry(name);
//				else if(this.PIT.containsKey(name))
//					this.PIT.get(name).removeHost(other);
//			}
//		}
//	}
//
//	/** After receiving a package, a node verifies if it's a interest or a data package
//	 * @param id of the transferred message
//	 * @param from host the message was from (previous hop)*/
//	@Override
//	public Message messageTransferred(String id, DTNHost from)
//	{
//		Message m = super.messageTransferred(id, from);
//		
//		String type = m.getData().split(" ")[0];
//		String name = m.getData().split(" ")[1];
//			
//		if(type.equalsIgnoreCase(INTEREST_PACKAGE))
//			onInterest(m, name, from);
//		else if(type.equalsIgnoreCase(CONTENT_PACKAGE))
//			onData(m, name, from);
//		
//		return m;
//	}
//
//	/** Processing of packages of interest. Here the packet is analyzed
//	 * and the conditions for acceptance of new interest are checked. 
//	 * If exists any content that satisfies the interest, then it is returned immediately, 
//	 * otherwise the PIT is added a new entry with the conditions.
//	 * @param m is message
//	 * @param name is the name of content
//	 * @param from is the sender
//	 */
//	public void onInterest(Message m, String name, DTNHost from)
//	{		
//		double socialValue = 0;
//		if(this.socialTieTable.containsKey(m.getFrom()))
//			socialValue = this.socialTieTable.get(m.getFrom()).getValue();
//		
//		Message c = null;
//		
//		if(this.hasMessage(name))
//		{  
//			c = this.getMessage(name); /*if there is content in the buffer*/
//		}
//		else if(this.deliveredMessages.containsKey(name))
//		{
//			c = this.deliveredMessages.get(name); /*if there is content in repository*/
//			this.addToMessages(c, true);
//		}	
//		else if(this.FIB.containsKey(name) || socialValue >= this.avgRelationship)
//		{
//			/*if there is knowledge of the content in FIB or socialValue > this.friendship*/
//			this.updatePIT(m, false, from); //creates a PIT entry and subscriber is false
//			
//		}
//		else /*if you do not have PIT or FIB entry for the interest or socialValue < THRESHOLD, discard the message*/
//			this.deleteMessage(m.getId(), true);
//		
//		if(c!=null) /*this host has the searched content == the interest was delivered*/
//		{
//			this.updatePIT(m, false, from);
//			
//			this.getMessage(c.getId()).setTo(m.getFrom()); 
//			
//			this.sendData();
//			
//			if(this.hasMessage(m.getId()))
//				this.deleteMessage(m.getId(), false);
//		}
//		
//		this.ccnMessageTransferredListener(m, from, c!=null);
//	}
//
//	/** Processing of packages of data. If the node is subscriber data is 
//	 * added to the repository. If not are just kept in the CS temporarily.
//	 * @param m is message
//	 * @param name is the name of content
//	 * @param from is the sender
//	 */
//	public void onData(Message m, String name, DTNHost from)
//	{
//		this.removeInterest(name);
//		
//		this.updateFIB(m, from);
//		
//		boolean finalTarget = Boolean.FALSE;
//		
//		if (this.PIT.containsKey(name))
//		{
//			PITEntry entry = this.PIT.get(name);
//			finalTarget = entry.isSubscriber;
//			if(finalTarget) /*It means the node is a primary requester of content*/
//				this.deliveredMessages.put(m.getId(), m); // add message to repository 
//						
//			if(!entry.moreRequesters())
//				this.deletePITEntry(name);
//			else
//				this.PIT.get(name).removeHost(this.getHost());
//		}
//		else if(!this.socialTieTable.containsKey(m.getTo()) 
//				|| this.socialTieTable.containsKey(m.getTo()) && this.socialTieTable.get(m.getTo()).getValue() < this.avgRelationship)
//			this.deleteMessage(m.getId(), true); //there's no or there's a entry in socialTieTable, but the value is lower
//		
//		this.ccnMessageTransferredListener(m, from, finalTarget);
//	}
//
//	/**
//	 * Called when it receives data and possesses pending interest messages.
//	 * A message of interest is eliminated, created by the node itself.
//	 * @param name is the name of content;
//	 */	
//	public void removeInterest(String name)
//	{
//		Collection<Message> msgCollection = getMessageCollection();	
//		
//		for (Message m : msgCollection) 
//		{
//			String type = m.getData().split(" ")[0];
//			String mName = m.getData().split(" ")[1];
//				
//			if(mName.equalsIgnoreCase(name))
//			{
//				if(type.equalsIgnoreCase(INTEREST_PACKAGE))
//				{
//					this.interestsToRemove.add(m.getId());
//				}
//			}
//		}
//		for (String id : this.interestsToRemove)
//		{
//			this.deleteMessage(id, false);
//		}
//		this.interestsToRemove.clear();
//	}
//	
//	@Override
//	public RoutingInfo getRoutingInfo() {
//		RoutingInfo ri = super.getRoutingInfo();
//		RoutingInfo fib = new RoutingInfo(this.FIB.size() + " FIB's size");
//		RoutingInfo pit = new RoutingInfo(this.PIT.size() + " PIT's size");
//		ri.addMoreInfo(fib);
//		ri.addMoreInfo(pit);
//		
//		for (PITEntry entry : this.PIT.values()) {
//			pit.addMoreInfo(new RoutingInfo(entry.getName() + " requesters:" + entry.getHosts()));
//		}
//		
//		for (String name : this.FIB.keySet()){
//			fib.addMoreInfo(new RoutingInfo(name + " provider path:" + this.FIB.get(name)));
//		}
//		return ri;
//	}
//
//	@Override
//	public MessageRouter replicate() {
//		return new DirescOld(this);
//	}
//	
//	@Override
//	public void update() {
//		super.update();
//		if (this.isTransferring() || !this.canStartTransfer()) {
//			return; // can't start a new transfer
//		}
//		// communication is driven by sending messages of interest 
//		this.sendInterest(); // try send Interest first 
//		this.sendData();	// try send data
//	}
//}
