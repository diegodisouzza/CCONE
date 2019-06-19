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
	
	/**Pit entry life time*/
	private double pel;
	
	/**If this host is a primary requester*/
	private boolean requester;

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
	 * @param pel pit entry lifetime
	 * @param isRequester if is a primary requester*/
	public PITEntry(String name, ArrayList<DTNHost> hosts, double timeCreated, double pel, boolean isRequester){
		
		this.name = name;
		this.hosts= hosts;
		this.timeCreated = timeCreated;
		this.pel = pel;
		this.requester = isRequester;
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
	
	public void setPel(double pel)
	{
		this.pel = pel;
	}
	
	public double getPel()
	{
		return this.pel;
	}
	
	public boolean isRequester() {
		return requester;
	}

	public void setRequester(boolean requester) {
		this.requester = requester;
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
