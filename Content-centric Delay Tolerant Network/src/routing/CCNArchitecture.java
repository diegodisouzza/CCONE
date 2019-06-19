/* 
 /* 
 * Copyright 2016 Universidade Federal do Estado do Rio de Janeiro, RJ, Brasil
 * Modified by Claudio Diego T. de Souza.
 * Released under GPLv3. See LICENSE.txt for details. 
 */

package routing;

import java.util.ArrayList;
import java.util.HashMap;

import core.DTNHost;
import core.Message;
import core.Settings;
import core.SimClock;

public class CCNArchitecture extends ActiveRouter{
	
	/** Router's setting name space ({@value})**/
	public static final String CCN_NS = "ArquiteturaCCN";
	
	/** Interest package's setting ({@value})**/
	public static final String INTEREST_PACKAGE = "interest";
	
	/** Content package's setting ({@value})**/
	public static final String CONTENT_PACKAGE = "content";
		
	/** Forwarding Information Base*/
	protected HashMap<String, ArrayList<DTNHost>> FIB = new HashMap<String, ArrayList<DTNHost>>();;
	
	/** Pending Interest Table*/
	protected HashMap<String, PITEntry> PIT = new HashMap<String,PITEntry>();;
	
	/** Control of interests received*/
	protected ArrayList<String> nonces = new ArrayList<>();
	
	public CCNArchitecture(Settings s) {
		super(s);				
	}
	public CCNArchitecture(CCNArchitecture obj) {
		super(obj);		
	}

	/** Update PIT entry
	 * Define if this router is subscriber or not
	 * @param m interest message
	 * @param from host from where the message came
	 * */
	public void updatePIT(Message m, DTNHost from)
	{
		DTNHost source = m.getFrom();
		CCNMessage message = this.messageFields(m);
		
		if(message.getType().equals(INTEREST_PACKAGE)) // if is a interest package 
		{
			if(!this.nonces.contains(m.getId())) // if is not in dead list
			{
				if(this.PIT.containsKey(message.getName())) // if the name exists in PIT
				{
					if(!this.PIT.get(message.getName()).getHosts().contains(from)) //if the from host is not in PIT entry
						this.PIT.get(message.getName()).addHost(from); //insert previous hop identifier in PIT entry
					
					PELUpdate(m, message.getName());
				}
				else //There is no entry in the PIT, create a new entry
				{ 	
					if(searchFIB(m, message.getName()))
					{
						ArrayList<DTNHost> listOfHosts = new ArrayList<DTNHost>();
						
						if(from.equals(null)) 
							from = this.getHost();
						
						listOfHosts.add(from);
						
						PITEntry entry = new PITEntry(message.getName(), listOfHosts, SimClock.getTime(), 2 * m.getTtl(), source.equals(this.getHost()));
						
						if(source.equals(from))
							entry.setId(m.getId());
						
						this.PIT.put(message.getName(),entry);
					}
				}
				
				this.nonces.add(m.getId());
			}
			else
			{
				this.removeFromMessages(m.getId());
			}
		}
		else // if is a content package 
		{
			if(this.PIT.containsKey(message.getName())) // if the name exists in PIT
			{
				PITEntry entry = this.PIT.get(message.getName());
				
				if(entry.isRequester()) {
					
					this.deliveredMessages.put(m.getId(), m); // add message to repository
					
				}
				
				if(entry.getHosts().size() <= 1) {
					
					this.deletePITEntry(message.getName());
					
				}
				else
					this.PIT.get(message.getName()).getHosts().remove(this.getHost());
			}
			else
			{
				this.removeFromMessages(m.getId());
			}
		}
	}
	
	/** Verifies if FIB has a entry to the content searched
	 * @param m Message
	 * @param name of the content
	 * @return true if there is a entry, false if there isn't**/
	public boolean searchFIB(Message m, String name)
	{
		if(this.FIB.containsKey(name) || m.getTo().equals(this.getHost()))
			return true;
		else
			return false;
	}
	
	/** Update FIB entry
	 * @param m data message
	 * @param from host where the message came from
	 * */
	public void updateFIB(Message m, DTNHost from)
	{
		CCNMessage message = this.messageFields(m);
		
		if(message.getType().equals(CONTENT_PACKAGE))
		{
			if(this.FIB.containsKey(message.getName())) // if it exists in FIB add only the identifier of the node that requested
			{ 
				if(!this.FIB.get(message.getName()).contains(from)) //if the host is not in FIB entry
					this.FIB.get(message.getName()).add(from); //insert previous hop identifier in FIB entry
			}
			else //There is no entry in the FIB, create a new entry
			{ 			
				ArrayList<DTNHost> listOfHosts = new ArrayList<DTNHost>();
				listOfHosts.add(from);
				
				this.FIB.put(message.getName(), listOfHosts);
			}
		}
		
	}
	
	/** Soften the TTL value in the PIT with the arrival of new interest 
	* messages that have TTL larger than the current TTL in PIT
	* difference = TTL message - TTL PIT
	* (New) TTL PIT = difference + (Old) TTL PIT
	* @param m Message
	* @param name of the content
	**/	
	public void PELUpdate(Message m, String name)
	{
	  int remainingPel =  (int)(((this.PIT.get(name).getPel() * 60) - (SimClock.getTime() - this.PIT.get(name).getTimeCreated())) / 60); // get remaining pel in minutes
	  
	  int messageTTL = m.getTtl(); // get Message TTL in minutes
	  
	  if(messageTTL > remainingPel)
	  {
		  int  pelIncrement = messageTTL - remainingPel;
		  this.PIT.get(name).setPel(remainingPel + pelIncrement); // set new pel
	  }	 
	}
	
	/** Checks if the pit entry lifetime is expired or not to decide whether to drop or not a message
	 * @param name to search in pit
	 * @return true if the pit entry lifetime is expired, false if it is not**/
	public boolean isPelExpired(String name)
	{
		int pel = (int) this.PIT.get(name).getPel() * 60;
		int timeCreated = (int) this.PIT.get(name).getTimeCreated();
		
		if(timeCreated + pel >= SimClock.getIntTime())
			return true;
		else
			return false;
	}
	
	/** Delete PIT entry
	 * @param name of the content
	 */
	public void deletePITEntry(String name)
	{
		if(this.PIT.containsKey(name)){
			this.PIT.remove(name);
		}
	}

	/** Drop messages of Interest and Data
	 * TTL is updated and if it expired messages are removed
	 * The TTL of the PIT is adjusted with the arrival of new interests
	 * */	
	@Override
	protected void dropExpiredMessages() 
	{	
		Message[] messages = getMessageCollection().toArray(new Message[0]);
		
		for(Message m : messages)
		{
			int ttl = m.getTtl();

			if (ttl <= 0)
			{
				CCNMessage message = this.messageFields(m);
				if(message.getType().equalsIgnoreCase(INTEREST_PACKAGE))
				{
					if (this.PIT.containsKey(message.getName()))
					{
						if(isPelExpired(message.getName()))
						{	
							deletePITEntry(message.getName()); // update PIT
							
							if(m.getFrom().equals(this.getHost()) && !(this.hasMessage(message.getName()) 
									|| this.deliveredMessages.containsKey(message.getName())))
								m.setTtl(this.ttlMessageSource()); // update the message ttl on requester
							else
								deleteMessage(m.getId(), true); // remove interest message from buffer of intermediates nodes
						}
					}
				}
				else
				{
					if(m.getFrom().equals(this.getHost())) // this host is the producer of content
						m.setTtl(this.ttlMessageSource()); // update the message ttl on producer
					else
						deleteMessage(m.getId(), true); // remove data message from buffer of nodes
				}
			}
		}
	}
	
	/** Adjusts the TTL of messages on sources when the original TTL expires.
	 * This increments TTL on its own value every time it ends (just on sources)
	 * @return the new ttl for the message**/
	public int ttlMessageSource()
	{
		int adjustment = (int) SimClock.getTime() / (this.msgTtl * 60);
		adjustment++;
		return adjustment * this.msgTtl;
	}
	
	/**
	 * Splits the data of the message into type and name
	 * @param m Message
	 * @return object of CCNMessage class**/
	public CCNMessage messageFields(Message m)
	{
		CCNMessage ccnMessage = new CCNMessage(m.getData().split(" ")[0], m.getData().split(" ")[1], m);
		return ccnMessage;
	}
	
	/** Rejects messages this node already received
	 * @param m Message
	 * @return the code informing the message can be received or not
	 * */
	@Override
	protected int checkReceiving(Message m)
	{
		CCNMessage message = this.messageFields(m);
				
		if(message.getType().equals(INTEREST_PACKAGE))
		{
			if(this.nonces.contains(m.getId()))
				return MessageRouter.DENIED_UNSPECIFIED;
		}
		else
		{
			if(this.deliveredMessages.containsKey(m.getId()))
				return MessageRouter.DENIED_UNSPECIFIED;
		}
		return super.checkReceiving(m);
	}

	@Override
	public MessageRouter replicate()
	{
		return new CCNArchitecture(this);
	}

	@Override
	public void update()
	{
		super.update();
	}
	
}

class CCNMessage{
	private String type;
	private String name;
	private Message m;
	
	public CCNMessage(String type, String name, Message m)
	{
		this.type = type;
		this.name = name;
		this.m = m;
	}
	public String getType()
	{
		return type;
	}
	public void setType(String type)
	{
		this.type = type;
	}
	public String getName()
	{
		return name;
	}
	public void setName(String name)
	{
		this.name = name;
	}
}
