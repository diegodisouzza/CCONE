package projeto;

import core.DTNHost;

public class SocialTie {

	/**The node that stores the social tie information*/
	private DTNHost thisHost;
	
	/**The encountered node*/
	private DTNHost otherHost;
	
	/**The last stored time stamp (the current one)*/
	private double tBase;
	
	/**The social relationship value computed from encounters time stamp*/
	private double value;
	
	/**Constructor for SocialTie
	 * @param value social relationship value
	 * @param tBase last stored time stamp (the current one)
	 * @param thisHost node that stores the social tie information
	 * @param otherHost encountered node*/
	public SocialTie(double value, double tBase, DTNHost thisHost, DTNHost otherHost){
		this.value = value;
		this.tBase = tBase;
		this.thisHost = thisHost;
		this.otherHost = otherHost;
	}

	public DTNHost getThisNode() {
		return thisHost;
	}

	public void setThisNode(DTNHost thisNode) {
		this.thisHost = thisNode;
	}

	public DTNHost getOtherNode() {
		return otherHost;
	}

	public void setOtherNode(DTNHost otherNode) {
		this.otherHost = otherNode;
	}

	public double gettBase() {
		return tBase;
	}

	public void settBase(double tBase) {
		this.tBase = tBase;
	}

	public double getValue() {
		return value;
	}

	public void setValue(double value) {
		this.value = value;
	}
	
}
