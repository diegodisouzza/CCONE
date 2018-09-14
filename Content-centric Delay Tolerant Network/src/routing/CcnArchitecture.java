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

public class CcnArchitecture extends ActiveRouter{
	
	/** Router's setting name space ({@value})**/
	public static final String CCN_NS = "ArquiteturaCCN";
	
	/** Interest package's setting ({@value})**/
	public static final String INTEREST_PACKAGE = "interest";
	
	/** Content package's setting ({@value})**/
	public static final String CONTENT_PACKAGE = "content";
		
	/** Hosts' setting ({@value})**/
	public static final String HOSTS = "nrofHosts";
		
	/** Forwarding Information Base*/
	protected HashMap<String, ArrayList<DTNHost>> FIB;
	
	/** Pending Interest Table*/
	protected HashMap<String, PITEntry> PIT;
	
	/** Control of interests received*/
	protected ArrayList<String> nonces = new ArrayList<>();
	
	/** Number of hosts*/
	protected int nrofHosts;
	
	public CcnArchitecture(Settings s) {
		super(s);
		Settings settings= new Settings(CCN_NS);
		this.nrofHosts = settings.getInt(HOSTS);
				
		initRouter();
	}
	public CcnArchitecture(CcnArchitecture obj) {
		super(obj);
		this.nrofHosts = obj.nrofHosts;
		
		initRouter();
	}
	
	/** Init PIT, FIB*/
	public void initRouter()
	{
		this.PIT = new HashMap<String,PITEntry>();
		this.FIB = new HashMap<String, ArrayList<DTNHost>>();
	}

	/** Update PIT entry
	 * Define if this router is subscriber or not
	 * @param m interest message
	 * @param isSub true if this router is subscriber, false if isn't subscriber
	 * @param from host from where the message came
	 * */
	public void updatePIT(Message m, boolean isSub, DTNHost from)
	{
		DTNHost source = m.getFrom();
		String type = m.getData().split(" ")[0];
		String name = m.getData().split(" ")[1];
		
		if(type.equals(CONTENT_PACKAGE) || this.nonces.contains(m.getId()))
			return;
		
		if(this.PIT.containsKey(name)) // if it exists in PIT add only the identifier of the node that requested
		{ 
			if(!this.PIT.get(name).getHosts().contains(source)) //if the host is not in PIT entry
				this.PIT.get(name).addHost(source); //insert requester identifier in PIT entry
			if(!this.PIT.get(name).getHosts().contains(from)) //if the host is not in PIT entry
				this.PIT.get(name).addHost(from); //insert previous hop identifier in PIT entry
						
			pitTtlUpdate(m, name);
		}
		else //There is no entry in the PIT, create a new entry
		{ 			
			ArrayList<DTNHost> listOfHosts = new ArrayList<DTNHost>();
			listOfHosts.add(source);
			
			if(!from.equals(null) && !from.equals(source))
				listOfHosts.add(from);
			
			PITEntry entry = new PITEntry(name, listOfHosts, SimClock.getTime(), m.getTtl(), isSub);
			
			if(isSub)
				entry.setId(m.getId());
			
			this.PIT.put(name,entry);
		}
		this.nonces.add(m.getId());
	}
	
	/** Update FIB entry
	 * @param m data message
	 * @param from host where the message came from
	 * */
	public void updateFIB(Message m, DTNHost from)
	{
		DTNHost source = m.getFrom(); 
		String type = m.getData().split(" ")[0];
		String name = m.getData().split(" ")[1];
		
		if(type.equals(INTEREST_PACKAGE))
			return;
		
		if(this.FIB.containsKey(name)) // if it exists in FIB add only the identifier of the node that requested
		{ 
			if(!this.FIB.get(name).contains(source)) //if the host is not in FIB entry
				this.FIB.get(name).add(source); //insert requester identifier in FIB entry
			if(!this.FIB.get(name).contains(from)) //if the host is not in FIB entry
				this.FIB.get(name).add(from); //insert previous hop identifier in FIB entry
		}
		else //There is no entry in the FIB, create a new entry
		{ 			
			ArrayList<DTNHost> listOfHosts = new ArrayList<DTNHost>();
			listOfHosts.add(source);
			
			if(!from.equals(null) && !from.equals(source))
				listOfHosts.add(from);
			
			this.FIB.put(name, listOfHosts);
		}
	}
	
	/** Soften the TTL value in the PIT with the arrival of new interest 
	* messages that have TTL larger than the current TTL in PIT
	* difference = TTL message - TTL PIT
	* (New) TTL PIT = difference + (Old) TTL PIT
	* */	
	public int pitTtlUpdate(Message m, String key)
	{
	  int TTLPIT =  (int)(((this.PIT.get(key).getTtl() * 60) - (SimClock.getTime()-this.PIT.get(key).getTimeCreated())) /60.0); // get PIT TTL
	  
	  int TTLMessage = m.getTtl(); // get Message TTL
	  
	  if(TTLMessage > TTLPIT)
	  {
		  int  ttlToPIT = TTLMessage - TTLPIT;
		  this.PIT.get(key).setTtl(TTLPIT + ttlToPIT); // set new TTL to PIT
	  }
	 
	  return (int)(((this.PIT.get(key).getTtl() * 60) - (SimClock.getTime()-this.PIT.get(key).getTimeCreated())) /60.0);
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
			
			if(m.getData() != null)
			{
				int ttl = m.getTtl();

				if (ttl <= 0)
				{
					String type = m.getData().split(" ")[0];
					String name = m.getData().split(" ")[1];
					
					if(type.equalsIgnoreCase(INTEREST_PACKAGE))
					{
						if (this.PIT.containsKey(name))
						{
							int val = pitTtlUpdate(m, name);
							if(val <= 0)
							{	
								deletePITEntry(name); // update PIT
								
								if(m.getFrom().equals(this.getHost()) && !(this.hasMessage(name) 
										|| this.deliveredMessages.containsKey(name)))
									m.setTtl(this.ttlMessageSource()); // update the message ttl on requester
								else
									deleteMessage(m.getId(), true); // remove interest message from buffer of intermediates nodes
							}
						}
					}
					else if(type.equalsIgnoreCase(CONTENT_PACKAGE))
					{
						if(m.getFrom().equals(this.getHost())) // this host is the producer of content
						{
							m.setTtl(this.ttlMessageSource()); // update the message ttl on producer
						}
						else
						{
							deleteMessage(m.getId(), true); // remove data message from buffer of nodes
						}
					}
				}
			}
			else
			{
				System.out.println("Message "+m.getId()+" without data");
				deleteMessage(m.getId(), true); // remove data message from buffer
			}
		}
	}
	
	/** Adjusts the TTL of messages on sources when the original TTL expires.
	 * This increments TTL on its own value every time it ends (just on sources)**/
	public int ttlMessageSource()
	{
		int adjustment = (int) SimClock.getTime() / (this.msgTtl * 60);
		adjustment++;
		return adjustment * this.msgTtl;
	}
	
	public CcnMessage messageFields(Message m)
	{
		CcnMessage ccnMessage = new CcnMessage(m.getData().split(" ")[0], m.getData().split(" ")[1]);
		return ccnMessage;
	}
	
	/** Rejects interest messages this node already received*/
	@Override
	protected int checkReceiving(Message m)
	{
		
		String type = m.getData().split(" ")[0];
		String name = m.getData().split(" ")[1];
				
		if(type.equals(INTEREST_PACKAGE))
		{
			if(this.nonces.contains(m.getId()))
				return MessageRouter.DENIED_UNSPECIFIED;
		}
		else if(type.equals(CONTENT_PACKAGE))
		{
			if(this.deliveredMessages.containsKey(m.getId()))
				return MessageRouter.DENIED_UNSPECIFIED;
		}
		return super.checkReceiving(m);
	}

	@Override
	public MessageRouter replicate()
	{
		return new CcnArchitecture(this);
	}

	/** Checks out all sending connections to finalize the 
	 * ready ones and abort those whose connection went down. 
	 * Also drops messages whose TTL <= 0 (checking every one simulated minute).
	 * Load content from file*/
	@Override
	public void update()
	{
		super.update();
	}
	
}

class CcnMessage{
	private String type;
	private String name;
	
	public CcnMessage(String type, String name)
	{
		this.type = type;
		this.name = name;
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
