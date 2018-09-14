/* 
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details. 
 */
package input;

/**
 * A message related external event
 */
public abstract class MessageEvent extends ExternalEvent {
	/** address of the node the message is from */
	protected int fromAddr;
	/** address of the node the message is to */
	protected int toAddr;
	/** identifier of the message */
	protected String id;
	/** data of the message */
	protected String data;
	
	/**
	 * Creates a message  event
	 * @param from Where the message comes from
	 * @param to Who the message goes to 
	 * @param id ID of the message
	 * @param time Time when the message event occurs
	 * @param data Data of message
	 */
	public MessageEvent(int from, int to, String id, double time, String data) {
		super(time);
		this.fromAddr = from;
		this.toAddr= to;
		this.id = id;
		this.data = data;
	}
	
	/**
	 * Creates a message  event
	 * @param from Where the message comes from
	 * @param id ID of the message
	 * @param time Time when the message event occurs
	 * @param data Data of message
	 */
	public MessageEvent(int from, String id, double time, String data) {
		super(time);
		this.fromAddr = from;
		this.id = id;
		this.data = data;
	}
	
	/**
	 * Creates a message  event
	 * @param from Where the message comes from
	 * @param to Who the message goes to 
	 * @param id ID of the message
	 * @param time Time when the message event occurs
	 */
	public MessageEvent(int from, int to, String id, double time) {
		super(time);
		this.fromAddr = from;
		this.toAddr= to;
		this.id = id;
	}
	
	@Override
	public String toString() {
		return "MSG @" + this.time + " " + id;
	}
}
