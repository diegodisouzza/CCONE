/* 
/* 
* Copyright 2016 Universidade Federal do Estado do Rio de Janeiro, RJ, Brasil
* Modified by Claudio Diego T. de Souza.
* Released under GPLv3. See LICENSE.txt for details. 
*/
package routing;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import core.Connection;
import core.DTNHost;
import core.Message;
import core.Settings;
import core.Tuple;

public class CCNbroadcasting extends CCNArchitecture {
	
	/**Constructor 
	 * @param obj CCNbroadcasting*/
	public CCNbroadcasting(CCNbroadcasting obj) {
		super(obj);
	}
	/**Constructor 
	 * @param obj Settings*/
	public CCNbroadcasting(Settings obj) {
		super(obj);
	}

	/** Refresh informations about nodes encounters in social tie tables and send messages
	 * @param con Connection*/
	@Override
	public void changedConnection(Connection con)
	{
		if (con.isUp()) 
		{
			DTNHost otherHost = con.getOtherNode(getHost());
			
			/* FIB and CS announcement*/
			csAnnouncement(otherHost); //direct announcement
			fibAnnouncement(otherHost); //transitive announcement
		}
	}
	/** Make the direct announcement between CS of nodes
	 *  @param host host of contact
	 * */
	public void csAnnouncement(DTNHost host)
	{
		CCNArchitecture otherRouter = (CCNArchitecture)host.getRouter();
		
		for(Message m : this.getMessageCollection())
		{
			String type = m.getData().split(" ")[0];
			String name = m.getData().split(" ")[1];
			
			if(type.equals(CONTENT_PACKAGE))
			{
				if(!otherRouter.FIB.containsKey(name) && !otherRouter.hasMessage(name))
				{
					ArrayList<DTNHost> path = new ArrayList<DTNHost>();
					path.add(this.getHost());
					otherRouter.FIB.put(name, path);
				}
				else if(otherRouter.FIB.containsKey(name) && !otherRouter.FIB.get(name).contains(this.getHost()))
				{
					otherRouter.FIB.get(name).add(this.getHost());
				}
			}
		}
	}
	
	/** Make the transitive announcement between FIB of nodes
	 * @param host host of contact
	 * */
	public void fibAnnouncement(DTNHost host)
	{
		CCNArchitecture otherRouter = (CCNArchitecture)host.getRouter();
		
		for(Message m : this.getMessageCollection())
		{
			String type = m.getData().split(" ")[0];
			String name = m.getData().split(" ")[1];
			
			if(type.equals(CONTENT_PACKAGE))
			{
				if(!otherRouter.FIB.containsKey(name) && !otherRouter.hasMessage(name)){
					ArrayList<DTNHost> path = new ArrayList<>();
					path.add(this.getHost());
					otherRouter.FIB.put(name, path);
				}
				else if(otherRouter.FIB.containsKey(name) && !otherRouter.FIB.get(name).contains(this.getHost()))
				{
					otherRouter.FIB.get(name).add(this.getHost());
				}
			}
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
			CCNbroadcasting otherRouter = (CCNbroadcasting)other.getRouter();
			
			if (otherRouter.isTransferring())
				return; // skip hosts that are transferring
			
			try {
				for (Message m : messages) 
				{
					if (otherRouter.hasMessage(m.getId())
							|| m.getFrom().equals(other)
							|| otherRouter.nonces.contains(m.getId()))
						continue; // skip messages that the other one has
					
					String type = m.getData().split(" ")[0];
					String name = m.getData().split(" ")[1];
					
					if(type.equalsIgnoreCase(INTEREST_PACKAGE))
					{					
						if(!this.PIT.containsKey(name))
						{
							if(m.getFrom().equals(this.getHost()))
							{
								updatePIT(m, m.getFrom());
							}
						}
						
						if(this.PIT.containsKey(name))
						{
							outgoingMessages.add(new Tuple<Message, Connection>(m,con));
						}
					}
				}
			} catch (Exception e) {
			}
		}
				
		if(outgoingMessages.size() > 0)
		{
			Tuple<Message, Connection> tupleSent = tryMessagesForConnected(outgoingMessages);
			
			if(tupleSent!=null && this.getHost() == tupleSent.getKey().getFrom())
			{	
				
			}
		}
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
			CCNbroadcasting otherRouter = (CCNbroadcasting)other.getRouter();
				
			if (otherRouter.isTransferring())
				return; // skip hosts that are transferring

			try {
				for (Message m : messages) 
				{
					String type = m.getData().split(" ")[0];
					String name = m.getData().split(" ")[1];
					
					if(otherRouter.hasMessage(m.getId())
							|| otherRouter.deliveredMessages.containsKey(name)
							|| m.getFrom().equals(other))
						continue; // skip messages that the other one has

					if(type.equalsIgnoreCase(CONTENT_PACKAGE)) 
					{
						/*if the other node is a requester to this one*/
						if(this.PIT.containsKey(name) && this.PIT.get(name).getHosts().contains(other)) 
						{
							outgoingMessages.add(new Tuple<Message, Connection>(m,con));
						}
						/*if the node is a requester to the content*/
						else if(otherRouter.PIT.containsKey(name) && otherRouter.PIT.get(name).getHosts().size() >= 1)
						{
							outgoingMessages.add(new Tuple<Message, Connection>(m,con));
						}
					}		
				}
			} catch (Exception e) {
			} 
		}
				
		if (outgoingMessages.size() > 0)
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
		boolean isbuffered = false;
		
		if(this.hasMessage(name))
		{  
			c = this.getMessage(name).replicate(); /*if there is content in the buffer*/
			isbuffered = true;
		}
		else if(this.deliveredMessages.containsKey(name)) 
			c = this.deliveredMessages.get(name); /*if there is content in repository*/
		else if(this.PIT.containsKey(name))
		{
			this.updatePIT(m, from); //creates a PIT entry and subscriber is false
			this.removeFromMessages(m.getId());
			
		}
		else if(this.FIB.containsKey(name))
		{
			this.updatePIT(m, from); //creates a PIT entry and subscriber is false
		}
		else /*if you do not have PIT or FIB entry for the interest or socialValue < THRESHOLD, discard the message*/
			this.removeFromMessages(m.getId());
		
		if(c!=null)
		{
			this.updatePIT(m, from);
			
			if(isbuffered)
				c.setFrom(this.getHost());  
			c.setTo(m.getFrom());  
			addToMessages(c, true);
			
			this.sendData();
			
			if(this.hasMessage(m.getId()))
				this.removeFromMessages(m.getId());
		}
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
		this.removeInterest(name);
		
		boolean finalTarget = Boolean.FALSE;
		
		if (this.PIT.containsKey(name))
		{
			PITEntry entry = this.PIT.get(name);
			finalTarget = entry.isRequester();
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
		else this.removeFromMessages(m.getId());
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
	
	@Override
	public RoutingInfo getRoutingInfo() {
		RoutingInfo ri = super.getRoutingInfo();
		RoutingInfo fib = new RoutingInfo(this.FIB.size() + " FIB's size");
		RoutingInfo pit = new RoutingInfo(this.PIT.size() + " PIT's size");
		ri.addMoreInfo(fib);
		ri.addMoreInfo(pit);
		return ri;
	}

	@Override
	public MessageRouter replicate() {
		// TODO Auto-generated method stub
		return new CCNbroadcasting(this);
	}
	
	@Override
	public void update() {
		super.update();
		if (this.isTransferring() || !this.canStartTransfer()) {
			return; // can't start a new transfer
		}
		// communication is driven by sending messages of interest 
		this.sendInterest(); // try send Interest first 
		this.sendData();	// try send data
	}
}