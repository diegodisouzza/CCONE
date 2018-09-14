package routing;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cluster.Cluster;
import cluster.DoubleArray;
import clustering.kmeans.AlgoKMeans;
import core.Connection;
import core.DTNHost;
import core.Message;
import core.Settings;
import core.SimClock;
import core.Tuple;
import projeto.SocialTie;

public class STCR extends CcnArchitecture {
	
	/** Number max of clusters ({@value})**/
	public static final String K = "k";
	
	protected Map<DTNHost, ArrayList<Double>> encounterVector = new HashMap<DTNHost, ArrayList<Double>>(); //<host, time stamps>
	protected Map<DTNHost, SocialTie> socialTieTable = new HashMap<DTNHost, SocialTie>();
	protected Map<String, SocialTie> socialTieTableOthers = new HashMap<String, SocialTie>();
	protected Map<DTNHost, Double> centrality = new HashMap<DTNHost, Double>();
	protected Map<DTNHost, ArrayList<String>> digestTable = new HashMap<DTNHost, ArrayList<String>>();
	protected ArrayList<String> digest = new ArrayList<String>();
	protected Map<String, Double> lastRelayNode  = new HashMap<String, Double>();
	
	/** Number max of clusters*/
	protected int k = 10;
	
	protected final double alpha = 0.5;
	
	protected List<Cluster> clusters = null;
	
	public STCR(Settings s) {
		super(s);
		Settings settings= new Settings(CCN_NS);
		this.k = settings.getInt(K);
	}

	public STCR(STCR obj) {
		super(obj);
		Settings settings= new Settings(CCN_NS);
		this.k = settings.getInt(K);
	}

	/** Refresh informations about nodes encounters in social tie tables and send messages
	 * @param con Connection*/
	@Override
	public void changedConnection(Connection con)
	{
		if (con.isUp()) 
		{
			DTNHost otherHost = con.getOtherNode(getHost());
			
			recordTimestamp(otherHost);
			
			computeSocialRelationship(otherHost);
			
			exchangeSocialTie(otherHost);
			
			computeCentrality();
			
			myCentrality();

			this.clusters = checkcentrality();
			
			convergeDigestTable(otherHost);
						
			if (!isTransferring() && canStartTransfer())
			{	 // can start a new transfer
				this.sendInterest();
				this.sendData();
			}
		}
	}
	
	public void recordTimestamp(DTNHost h)
	{
		if (encounterVector.containsKey(h))
		{
			encounterVector.get(h).add(SimClock.getTime());
		}
		else
		{
			ArrayList<Double> times = new ArrayList<Double> ();
			times.add(SimClock.getTime());
			encounterVector.put(h, times);
		}

	}
	
	public void computeSocialRelationship(DTNHost host)
	{
		double RiJ = 0.0;
		double x;
		double tBase = SimClock.getTime(); 
		
		if (this.encounterVector.containsKey(host))
		{
			ArrayList<Double> times = this.encounterVector.get(host);
			
			for (Double t : times)
			{
				x =  (tBase - t);
				RiJ = RiJ + Fx(x);
			}
		}
		
		if(this.socialTieTable.containsKey(host))
		{
			this.socialTieTable.get(host).setValue(RiJ);
			this.socialTieTable.get(host).settBase(tBase);
		}else{
			SocialTie socialT= new SocialTie(RiJ, tBase, this.getHost(), host);
			this.socialTieTable.put(host, socialT);
		}		
	}
	
	public double Fx(double x)
	{
		// 1/2^(x.1exp^-4)
		double lamb = 0.018315;
		return Math.pow(0.5, (lamb*x));
	}
	
	public void exchangeSocialTie(DTNHost other)
	{
		MessageRouter otherRouter = other.getRouter();
		Map<String, SocialTie> otherSocialTie = ((STCR)otherRouter).socialTieTableOthers; 
		Map<DTNHost, SocialTie> otherSTie = ((STCR)otherRouter).socialTieTable; 

		for (String key : otherSocialTie.keySet())
		{
			if(otherSocialTie.get(key).getThisNode()!= this.getHost())
			{
				if(this.socialTieTableOthers.containsKey(key))
				{
					if(otherSocialTie.get(key).gettBase() > this.socialTieTableOthers.get(key).gettBase())
					{
						this.socialTieTableOthers.get(key).setValue(otherSocialTie.get(key).getValue());
						this.socialTieTableOthers.get(key).settBase(otherSocialTie.get(key).gettBase());
					}
				}
				else 
				{
					this.socialTieTableOthers.put(key, otherSocialTie.get(key));
				}
			}

		}
				
		for(DTNHost host : otherSTie.keySet())
		{	
			String auxKey1 = other.toString() + "_" + host.toString();
			this.socialTieTableOthers.put(auxKey1,otherSTie.get(host));
		}		
	}
	
	public void computeCentrality()
	{
		double c =0.0;
		int N = 0;
		double R=0.0;
		DTNHost host = null; 
		
		for(int h = 0; h <= this.nrofHosts; h++)
		{
			for(SocialTie st : this.socialTieTableOthers.values())
			{
				if(st.getThisNode().getAddress() == h)
				{
					N = N + 1;
					R = R + st.getValue();
					host = st.getThisNode();
				}
			}
			
			if(host!=null)
			{
				c = (alpha*R)/N + ((1-alpha))*(R*R)/(N*(R*R));
				centrality.put(host, c);
				host = null;
			}
		}
	}
	
	public void myCentrality()
	{
		int N = 0;
		double R = 0;
		double c;
		
		for(SocialTie st : this.socialTieTable.values())
		{
			N = N + 1;
			R = R + st.getValue();
		}
		
		if(R!=0)
		{
			c = (alpha*R)/N + ((1-alpha))*(R*R)/(N*(R*R));
			centrality.put(this.getHost(), c);
		}
		
	}
	
	public void convergeDigestTable(DTNHost host)
	{
		if(!this.getMessageCollection().isEmpty())
		{
			STCR otherRouter = (STCR)host.getRouter();
			
			if(centrality.containsKey(this.getHost()) && centrality.containsKey(host))
			{
				if(centrality.get(this.getHost()) < centrality.get(host))
				{
					if(!checkSameCluster(clusters, this.centrality.get(this.getHost()), this.centrality.get(host)))
					{
						for(Message m : this.getMessageCollection())
						{
							String type = m.getData().split(" ")[0];
							String name = m.getData().split(" ")[1];
							
							if(type.equals(CONTENT_PACKAGE) && !this.digest.contains(name))
								this.digest.add(name);
						}
						if(!this.digest.isEmpty())
							otherRouter.digestTable.put(this.getHost(), this.digest);
						for(DTNHost h : this.digestTable.keySet())
						{
							if(otherRouter.digestTable.containsKey(h))
							{
								if(otherRouter.digestTable.get(h).size() > this.digestTable.get(h).size())
									otherRouter.digestTable.put(h, this.digestTable.get(h));
							}
						}
					}
				}
			}
		}
	}
	
	public List<Cluster> checkcentrality()
	{
		AlgoKMeans algoKMeans = new AlgoKMeans();
		List<Cluster> clusters = null;
		
		if(!this.centrality.isEmpty())
		{
			try {
				clusters = algoKMeans.runAlgorithm(this.centrality, this.k);
			} catch (NumberFormatException | IOException e){
				e.printStackTrace();
			}
		}
		return clusters;
	}
	
	/** After receiving a package, a node verifies if it's a interest or a data package
	 * @param id of the transferred message
	 * @param from host the message was from (previous hop)*/
	@Override
	public Message messageTransferred(String id, DTNHost from)
	{
		Message m = super.messageTransferred(id, from);
		
		if(!(m.getFrom().equals(this.getHost())))
		{
			String type = m.getData().split(" ")[0];
			String name = m.getData().split(" ")[1];
			
			if(type.equalsIgnoreCase(INTEREST_PACKAGE))
				onInterest(m, name, from);
			else if(type.equalsIgnoreCase(CONTENT_PACKAGE))
				onData(m, name, from);
		}
		else
			this.deleteMessage(m.getId(), true);
		
		return m;
	}
	
	/** Processing of packages of interest. Here the packet is analyzed
	 * and the conditions for acceptance of new interest are checked. 
	 * If exists any content that satisfies the interest, then it is returned immediately, 
	 * otherwise the PIT is added a new entry with the conditions.
	 * @param m is message
	 * @param name is the name of content
	 * @param from is the sender
	 */
	public void onInterest(Message m, String name, DTNHost from)
	{		
		this.nonces.add(m.getId());
		
		Message c = null;
		
		if(this.hasMessage(name))
		{
			c = this.getMessage(name).replicate();
		}
		else if(this.deliveredMessages.containsKey(name))
		{
			c = this.deliveredMessages.get(name);
		}
		
		if(c!=null)
		{
			c.setTo(m.getFrom());  
			addToMessages(c, true);
			this.sendData();
			
			if(this.hasMessage(m.getId()))
				this.removeFromMessages(m.getId());
		}		
		else
		{
			for(DTNHost h: this.digestTable.keySet())
			{
				for(String content: this.digestTable.get(h))
				{
					if(content.equalsIgnoreCase(name))
					{
						m.setTo(h);
						break;
					}
				}
			}
		}
		
		this.updatePIT(m, false, from); //creates a PIT entry and subscriber is false
		this.ccnMessageTransferredListener(m, from, c!=null);

	}
	
	/** Processing of packages of data. If the node is subscriber data is 
	 * added to the repository. If not are just kept in the CS temporarily.
	 * @param m is message
	 * @param name is the name of content
	 * @param from is the sender
	 */
	public void onData(Message m, String name, DTNHost from)
	{
boolean finalTarget = Boolean.FALSE;
		
		if (this.PIT.containsKey(name))
		{
			PITEntry entry = this.PIT.get(name);
			finalTarget = entry.isSubscriber();
			if(finalTarget) /*It means the node is a primary requester of content*/
			{
				this.deliveredMessages.put(m.getId(), m); // add message to repository 
			}
			if(entry.getHosts().size() <= 1)
			{
				this.removeFromMessages(m.getId());
				this.deletePITEntry(name);
			}
			else
				this.PIT.get(name).getHosts().remove(this.getHost());
		}
		
		this.removeInterest(name);
		this.ccnMessageTransferredListener(m, from, finalTarget);
	}
	
	/**
	 * Called when it receives data and possesses pending interest messages.
	 * A message of interest is eliminated, created by the node itself.
	 * @param name is the name of content;
	 */	
	public void removeInterest(String name)
	{
		Collection<Message> msgCollection = getMessageCollection();	
		
		try {
			for (Message m : msgCollection) 
			{
				String type = m.getData().split(" ")[0];
				String mName = m.getData().split(" ")[1];
				
				if(mName.equalsIgnoreCase(name))
				{
					if(type.equalsIgnoreCase(INTEREST_PACKAGE))
					{
						this.removeFromMessages(m.getId());
					}
				}
			}
		} catch (Exception e) {
		}
	}
	
	/**Attempts to send Interest to other router
	 * @param con Connection with the other host
	 */
	private void sendInterest()
	{
		Collection<Message> messages = this.getMessageCollection();
		List<Tuple<Message, Connection>> outgoingMessages = new ArrayList<Tuple<Message, Connection>>();
		
		for (Connection con : this.getConnections())
		{
			DTNHost other = con.getOtherNode(getHost());
			STCR otherRouter = (STCR)other.getRouter();
			
			if (otherRouter.isTransferring())
				return; // skip hosts that are transferring
			
			try {
				for (Message m : messages) 
				{
					if (other.getAddress() == 0)
						continue; // skip messages that the other one has
					
					String type = m.getData().split(" ")[0];
					String name = m.getData().split(" ")[1];
					
					if(type.equalsIgnoreCase(INTEREST_PACKAGE))
					{			
						if(!this.PIT.containsKey(name))
						{
							if(m.getFrom().equals(this.getHost()))
							{
								updatePIT(m, true, m.getFrom());
							}
						}
						else{
							if(m.getTo().getAddress() == 0)
							{
								if(this.centrality.containsKey(other))
								{
									double otherCentrality = this.centrality.get(other);
									double myCentrality = this.centrality.get(this.getHost());
									if(otherCentrality > myCentrality)
									{
										if(!checkSameCluster(clusters, myCentrality, otherCentrality))
										{
											if(this.lastRelayNode.containsKey(m.getId()))
											{
												if(otherCentrality > this.lastRelayNode.get(m.getId()))
													outgoingMessages.add(new Tuple<Message, Connection>(m,con));
											}
											else
												outgoingMessages.add(new Tuple<Message, Connection>(m,con));
										}
									}
								}
							}
							else if(m.getTo().equals(other))
							{
								outgoingMessages.add(new Tuple<Message, Connection>(m,con));
							}
							else
							{
								String auxKey = other.toString() + "_" + m.getTo().toString();
								double myRelation = this.socialTieTable.get(m.getTo()).getValue();
								double otherRelation = this.socialTieTableOthers.get(auxKey).getValue();
								
								if(otherRelation > myRelation)
									outgoingMessages.add(new Tuple<Message, Connection>(m,con));
							}
						}
					}
				}
			} catch (Exception e) {
			}
		}
				
		if(outgoingMessages.size() > 0)
		{
			Tuple<Message, Connection> tupleSent = tryMessagesForConnected(outgoingMessages);
			
			if(tupleSent!=null)
			{	
				Message m = tupleSent.getKey();
				DTNHost other = tupleSent.getValue().getOtherNode(this.getHost());
				this.lastRelayNode.put(m.getId(), this.centrality.get(other));
			}
		}
	}
	
	public boolean checkSameCluster(List<Cluster> clusters, double thisNode, double otherNode)
	{
		boolean check = false;
		List<DoubleArray> vector; 
		
		for (int i=0; i<clusters.size(); i++)
		{
			vector = clusters.get(i).getVectors();
			if (vector.contains(thisNode) && vector.contains(otherNode))
			{
				check = true;
			}
		}
		return check;
	}
	
	/**
	 * Attempts send Data to other router
	 * @return Tuple<Message, Connection>
	 */
	private void sendData()
	{
		Collection<Message> messages = this.getMessageCollection();
		List<Tuple<Message, Connection>> outgoingMessages = new ArrayList<Tuple<Message, Connection>>();
		
		for(Connection con : this.getConnections())
		{
			DTNHost other = con.getOtherNode(getHost());
			STCR otherRouter = (STCR)other.getRouter();
				
			if (otherRouter.isTransferring())
				return; // skip hosts that are transferring

			try {
				for (Message m : messages) 
				{
					String type = m.getData().split(" ")[0];
					String name = m.getData().split(" ")[1];
					
					if(type.equalsIgnoreCase(CONTENT_PACKAGE)) 
					{
						if(m.getTo().equals(other))
							outgoingMessages.add(new Tuple<Message, Connection>(m,con));
						else if(this.PIT.containsKey(name) && this.PIT.get(name).getHosts().contains(other))
						{
							outgoingMessages.add(new Tuple<Message, Connection>(m,con));
						}
						else
						{
							if (otherRouter.socialTieTable.containsKey(m.getTo())) 
							{
								if (this.socialTieTable.containsKey(m.getTo()))
								{
									if (otherRouter.socialTieTable.get(m.getTo()).getValue() > this.socialTieTable.get(m.getTo()).getValue())
										outgoingMessages.add(new Tuple<Message, Connection>(m,con));
								}
								else
								{
									outgoingMessages.add(new Tuple<Message, Connection>(m,con));
								}
							}	
						}
					}		
				}
			} catch (Exception e) {
			} 
		}
				
		if(outgoingMessages.size() > 0)
		{
			Tuple<Message, Connection> tupleSent = tryMessagesForConnected(outgoingMessages);	// try to send messages
		
			if(tupleSent!=null)
			{		
				Message m = tupleSent.getKey(); 
				Connection c = tupleSent.getValue();
				DTNHost h = c.getOtherNode(this.getHost());
				String name = m.getData().split(" ")[1];
				
				if(this.PIT.containsKey(name) && this.PIT.get(name).getHosts().size()<=1)
				{	
					deletePITEntry(name);
					this.removeFromMessages(m.getId()); //removes content from buffer if the node already sends this to all nodes in its PIT
				}	
				else if(this.PIT.containsKey(name))
					this.PIT.get(name).getHosts().remove(h);
			}
		}
	}

	@Override
	public STCR replicate() {
		return new STCR(this);
	}
	
	@Override
	public void update() {
		super.update();
		
//		if (isTransferring() && !canStartTransfer())
//			 return;// can't start a new transfer
//		
//		this.sendInterest();
//		this.sendData();
		
	}
}
