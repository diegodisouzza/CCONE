/* 
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details. 
 */
package report;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import core.DTNHost;
import core.Message;
import core.MessageListener;
import core.Settings;
import core.UpdateListener;
import routing.CCNArchitecture;

/**
 * Report for generating different kind of total statistics about message
 * relaying performance. Messages that were Sentout during the warm up period
 * are ignored.
 * <P><strong>Note:</strong> if some statistics could not be Sentout (e.g.
 * overhead ratio if no messages were delivered) "NaN" is reported for
 * double values and zero for integer median(s).
 */
public class MessageCCNStatsReport extends Report implements MessageListener, UpdateListener {
	/** Reporting granularity -setting id ({@value}). 
	 * Defines the interval how often (seconds) a new snapshot of energy levels
	 * is Sentout */
	public static final String GRANULARITY = "granularity";
	/** value of the granularity setting */
	protected final int granularity;
	/** time of last update*/
	protected double lastUpdate; 
	
	private Map<String, Double> creationTimes;
	private List<Double> latencies;
	private List<Integer> hopCounts;
	private List<Double> msgBufferTime;
	private List<Double> rtt; // round trip times
	
	private int nrofInterestDropped;
	private int nrofDataDropped;
	private int nrofInterestRemoved;
	private int nrofDataRemoved;
	private int nrofInterestStarted;
	private int nrofDataStarted;
	private int nrofInterestAborted;
	private int nrofDataAborted;
	private int nrofInterestRelayed;
	private int nrofDataRelayed;
	private int nrofInterestSentout;
	private int nrofDataSentout;
	private int nrofInterestDelivered;
	private int nrofDataDelivered;
	
	private int nrofInterestDropped_past;
	private int nrofDataDropped_past;
	private int nrofInterestRemoved_past;
	private int nrofDataRemoved_past;
	private int nrofInterestStarted_past;
	private int nrofDataStarted_past;
	private int nrofInterestAborted_past;
	private int nrofDataAborted_past;
	private int nrofInterestRelayed_past;
	private int nrofDataRelayed_past;
	private int nrofInterestSentout_past;
	private int nrofDataSentout_past;
	private int nrofInterestDelivered_past;
	private int nrofDataDelivered_past;
	
	private int totalInterestSentout;
	private int totalDataDelivered;
	
	private ArrayList<String> interestsSentout;
	/**
	 * Constructor.
	 */
	public MessageCCNStatsReport() {
		Settings settings = getSettings();
		this.lastUpdate = 0;
		this.granularity = settings.getInt(GRANULARITY);
		init();
	}

	@Override
	protected void init() {
		super.init();
		this.creationTimes = new HashMap<String, Double>();
		this.latencies = new ArrayList<Double>();
		this.msgBufferTime = new ArrayList<Double>();
		this.hopCounts = new ArrayList<Integer>();
		this.rtt = new ArrayList<Double>();
		
		this.nrofInterestDropped = 0;
		this.nrofDataDropped = 0;
		this.nrofInterestRemoved = 0;
		this.nrofDataRemoved = 0;
		this.nrofInterestStarted = 0;
		this.nrofDataStarted = 0;
		this.nrofInterestAborted = 0;
		this.nrofDataAborted = 0;
		this.nrofInterestRelayed = 0;
		this.nrofDataRelayed = 0;
		this.nrofInterestSentout = 0;
		this.nrofDataSentout = 0;
		this.nrofInterestDelivered = 0;
		this.nrofDataDelivered = 0;
		
		this.nrofInterestDropped_past = 0;
		this.nrofDataDropped_past = 0;
		this.nrofInterestRemoved_past = 0;
		this.nrofDataRemoved_past = 0;
		this.nrofInterestStarted_past = 0;
		this.nrofDataStarted_past = 0;
		this.nrofInterestAborted_past = 0;
		this.nrofDataAborted_past = 0;
		this.nrofInterestRelayed_past = 0;
		this.nrofDataRelayed_past = 0;
		this.nrofInterestSentout_past = 0;
		this.nrofDataSentout_past = 0;
		this.nrofInterestDelivered_past = 0;
		this.nrofDataDelivered_past = 0;
		
		this.totalInterestSentout = 0;
		this.totalDataDelivered = 0;
		
		this.interestsSentout = new ArrayList<String>();
		
		this.header();
		
	}
	
	public void header() {
		String header = "interval i_sentout i_started i_relayed i_dropped i_removed i_delivered i_overhead "
			+ "d_sentout d_started d_relayed d_dropped d_removed d_delivered d_overhead latency_avg hopcount_avg satisfaction_rate";
			
			write(header);
	}

	
	public void messageDeleted(Message m, DTNHost where, boolean dropped) {
		if (isWarmupID(m.getId())) {
			return;
		}
		
		String type = m.getData().split(" ")[0];
		
		if(type.equals(CCNArchitecture.INTEREST_PACKAGE))
		{
			if (dropped) {
				this.nrofInterestDropped++;
			}
			else {
				this.nrofInterestRemoved++;
			}
		}
		else if(type.equals(CCNArchitecture.CONTENT_PACKAGE))
		{
			if (dropped) {
				this.nrofDataDropped++;
			}
			else {
				this.nrofDataRemoved++;
			}
		}
		
		this.msgBufferTime.add(getSimTime() - m.getReceiveTime());
	}

	
	public void messageTransferAborted(Message m, DTNHost from, DTNHost to) {
		if (isWarmupID(m.getId())) {
			return;
		}
		
		String type = m.getData().split(" ")[0];
		
		if(type.equals(CCNArchitecture.INTEREST_PACKAGE))
		{
			this.nrofInterestAborted++;
			
		}
		else if(type.equals(CCNArchitecture.CONTENT_PACKAGE))
		{
			this.nrofDataAborted++;
		}
	}

	
	public void messageTransferred(Message m, DTNHost from, DTNHost to,
			boolean finalTarget) {
		if (isWarmupID(m.getId())) {
			return;
		}
		String type = m.getData().split(" ")[0];
		String name = m.getData().split(" ")[1];
		
		if(type.equals(CCNArchitecture.INTEREST_PACKAGE))
		{
			this.nrofInterestRelayed++;
			if (finalTarget) {
				this.nrofInterestDelivered++;
				this.hopCounts.add(m.getHops().size() - 1);
			}
			
		}
		else if(type.equals(CCNArchitecture.CONTENT_PACKAGE))
		{
			this.nrofDataRelayed++;
			if (finalTarget) {
				this.latencies.add(getSimTime() - 
						this.creationTimes.get(name+" "+to));
				this.nrofDataDelivered++;
				this.hopCounts.add(m.getHops().size() - 1);
			}
		}
	}

	public void newMessage(Message m) {
		if (isWarmup()) {
			addWarmupID(m.getId());
			return;
		}
		
		String type = m.getData().split(" ")[0];
		String name = m.getData().split(" ")[1];
		
		if(type.equals(CCNArchitecture.INTEREST_PACKAGE))
		{
			this.creationTimes.put(name+" "+m.getFrom(), getSimTime());
		}
		else if(type.equals(CCNArchitecture.CONTENT_PACKAGE))
		{
			this.creationTimes.put(name+" "+m.getFrom(), getSimTime());
		}
	}
	
	
	public void messageTransferStarted(Message m, DTNHost from, DTNHost to) {
		if (isWarmupID(m.getId())) {
			return;
		}
		
		String type = m.getData().split(" ")[0];
		
		if(type.equals(CCNArchitecture.INTEREST_PACKAGE))
		{
			this.nrofInterestStarted++;
			if(m.getFrom().equals(from)){
				if(!this.interestsSentout.contains(m.getId())){
					this.nrofInterestSentout++;
					this.interestsSentout.add(m.getId());
				}
			}
		}
		else if(type.equals(CCNArchitecture.CONTENT_PACKAGE))
		{
			this.nrofDataStarted++;
			if(m.getFrom().equals(from)){
				this.nrofDataSentout++;
			}
		}
	}
	
	/**
	 * Creates a new snapshot of the energy levels if "granularity" 
	 * seconds have passed since the last snapshot. 
	 * @param hosts All the hosts in the world
	 */
	public void updated(List<DTNHost> hosts) {
		double simTime = getSimTime();
		if (isWarmup()) {
			return; /* warmup period is on */
		}
		/* creates a snapshot once every granularity seconds */
		if (simTime - lastUpdate >= granularity) {
			createSnapshot();
			this.lastUpdate = simTime - simTime % granularity;
		}
	}

	public void createSnapshot() {
		double interestOverHead = Double.NaN;	// interest overhead ratio 
		double dataOverHead = Double.NaN;	// interest overhead ratio 
		double satisfactionRate = Double.NaN;
		
		this.nrofInterestDropped = this.nrofInterestDropped - this.nrofInterestDropped_past;
		this.nrofDataDropped = this.nrofDataDropped - this.nrofDataDropped_past;
		this.nrofInterestRemoved = this.nrofInterestRemoved - this.nrofInterestRemoved_past;
		this.nrofDataRemoved = this.nrofDataRemoved - this.nrofDataRemoved_past;
		this.nrofInterestStarted = this.nrofInterestStarted - this.nrofInterestStarted_past;
		this.nrofDataStarted = this.nrofDataStarted - this.nrofDataStarted_past;
		this.nrofInterestAborted = this.nrofInterestAborted - this.nrofInterestAborted_past;
		this.nrofDataAborted = this.nrofDataAborted - this.nrofDataAborted;
		this.nrofInterestRelayed = this.nrofInterestRelayed - this.nrofInterestRelayed_past;
		this.nrofDataRelayed = this.nrofDataRelayed - this.nrofDataRelayed_past;
		this.nrofInterestSentout = this.nrofInterestSentout - this.nrofInterestSentout_past;
		this.nrofDataSentout = this.nrofDataSentout - this.nrofDataSentout_past;
		this.nrofInterestDelivered = this.nrofInterestDelivered - this.nrofInterestDelivered_past;
		this.nrofDataDelivered = this.nrofDataDelivered - this.nrofDataDelivered_past;
		
		this.nrofInterestDropped_past = this.nrofInterestDropped;
		this.nrofDataDropped_past = this.nrofDataDropped;
		this.nrofInterestRemoved_past = this.nrofInterestRemoved;
		this.nrofDataRemoved_past = this.nrofDataRemoved;
		this.nrofInterestStarted_past = this.nrofInterestStarted;
		this.nrofDataStarted_past = this.nrofDataStarted;
		this.nrofInterestAborted_past = this.nrofInterestAborted;
		this.nrofDataAborted_past = this.nrofDataAborted;
		this.nrofInterestRelayed_past = this.nrofInterestRelayed;
		this.nrofDataRelayed_past = this.nrofDataRelayed;
		this.nrofInterestSentout_past = this.nrofInterestSentout;
		this.nrofDataSentout_past = this.nrofDataSentout;
		this.nrofInterestDelivered_past = this.nrofInterestDelivered;
		this.nrofDataDelivered_past = this.nrofDataDelivered;
		
		this.totalInterestSentout = this.totalInterestSentout + this.nrofInterestSentout;
		this.totalDataDelivered = this.totalDataDelivered + this.nrofDataDelivered;
		
		if (this.nrofInterestDelivered > 0) {
			interestOverHead = (1.0 * (this.nrofInterestRelayed - this.nrofInterestDelivered)) /
				this.nrofInterestRelayed;
		}
		if (this.nrofDataDelivered > 0) {
			dataOverHead = (1.0 * (this.nrofDataRelayed - this.nrofDataDelivered)) /
				this.nrofDataRelayed;
		}
		if (this.totalDataDelivered > 0) {
			satisfactionRate = (double) this.totalDataDelivered / this.totalInterestSentout;
		}
		
		String header = "interval i_sentout i_started i_relayed i_dropped i_removed i_delivered i_overhead d_sentout d_started "
				+ "d_relayed d_dropped d_removed d_delivered d_overhead latency_avg hopcount_avg satisfaction_rate";
		
		String statsText = (int)(getSimTime() / this.granularity) +" "+ this.nrofInterestSentout +" "+ this.nrofInterestStarted 
				+" "+ this.nrofInterestRelayed +" "+ this.nrofInterestDropped +" "+ this.nrofInterestRemoved 
				+" "+ this.nrofInterestDelivered +" "+ format(interestOverHead) +" "+ this.nrofDataSentout 
				+" "+ this.nrofDataStarted +" "+	this.nrofDataRelayed +" "+ this.nrofDataDropped 
				+" "+ this.nrofDataRemoved +" "+ this.nrofDataDelivered +" "+ format(dataOverHead) 
				+" "+ getAverage(this.latencies) +" "+ getIntAverage(this.hopCounts) +" "+ format(satisfactionRate);
		
		this.latencies = new ArrayList<Double>();
		this.hopCounts = new ArrayList<Integer>();
		
		System.out.println(header);
		System.out.println(statsText);
		write(statsText);
	}
	
}
