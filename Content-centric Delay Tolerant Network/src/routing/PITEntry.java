/* 
 /* 
 * Copyright 2015 Universidade do Minho, GCOM
 * Modified by Paulo Duarte.
 * Released under GPLv3. See LICENSE.txt for details. 
 */

package routing;

import java.util.ArrayList;

import core.DTNHost;

public class PITEntry {

	/**The interest message id*/
	private String id;
	
	/**The searched content name*/
	private String name;
	
	/**All hosts requested the content */
	private  ArrayList<DTNHost> hosts;
	
	/**The creation time*/
	private double timeCreated;
	
	/**Time to live*/
	private double ttl;
	
	/**If this host is a primary requester*/
	private boolean subscriber;

	/**If is a primary requester and already sent the interest*/
	private boolean sent = false;
	
	/**If it already sent some interest to a friend*/
	private boolean sentToFriends = false;
	
	/**If it already sent some interest to the cluster*/
	private boolean sentToCluster = false;
		
	/**Constructor for PITEntry
	 * @param name searched content name
	 * @param hosts requester hosts
	 * @param timeCreated creation time
	 * @param timeToLive time to live
	 * @param isSubscriber if is a primary requester*/
	public PITEntry(String name, ArrayList<DTNHost> hosts, double timeCreated, double timeToLive, boolean isSubscriber){
		
		this.name = name;
		this.hosts= hosts;
		this.timeCreated = timeCreated;
		this.ttl = timeToLive;
		this.subscriber = isSubscriber;
	}
	
	public String getId(){
		return this.id;
	}
	
	public void setId(String id){
		this.id = id;
	}
	
	public String getName(){
		return this.name;
	}
	
	public void setName(String name){
		this.name = name;
	}
	
	public void addHost(DTNHost host)
	{
		this.hosts.add(host);
	}
		
	public ArrayList<DTNHost> getHosts()
	{
		return this.hosts;
	}
	
	public boolean containsHost(DTNHost host)
	{
		return this.hosts.contains(host);
	}
	
	public boolean moreRequesters()
	{
		return this.hosts.size() > 1;
	}
	
	public boolean removeHost(DTNHost host)
	{
		return this.hosts.remove(host);
	}
	
	public void setTimeCreated(double timeCreated)
	{
		this.timeCreated = timeCreated;
	}
	
	public double getTimeCreated()
	{
		return this.timeCreated;
	}
	
	public void setTtl(double ttl)
	{
		this.ttl = ttl;
	}
	
	public double getTtl()
	{
		return this.ttl;
	}
	
	public boolean isSubscriber() {
		return subscriber;
	}

	public void setSubscriber(boolean subscriber) {
		this.subscriber = subscriber;
	}

	public boolean isSent() {
		return sent;
	}

	public void setSent(boolean isSent) {
		this.sent = isSent;
	}
	
	public boolean sentToFriends() {
		return this.sentToFriends;
	}
	
	public void setSentToFriends(boolean sentToFriends) {
		this.sentToFriends = sentToFriends;
	}
	
	public boolean sentToCluster() {
		return this.sentToCluster;
	}
	
	public void setSentToCluster(boolean sentToCluster) {
		this.sentToCluster = sentToCluster;
	}
	
}
