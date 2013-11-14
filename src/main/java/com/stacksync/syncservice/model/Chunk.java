package com.stacksync.syncservice.model;

public class Chunk {

	private Integer order = null;
	private String clientChunkName = null;
	
	public Chunk() {
	}
	
	public Chunk (String name , Integer order){
		this.clientChunkName = name;
		this.order = order;
	}
	
	public Integer getOrder() {
		return order;
	}

	public void setOrder(Integer order) {
		this.order = order;
	}

	public String getClientChunkName() {
		return clientChunkName;
	}

	public void setClientChunkName(String clientChunkName) {
		this.clientChunkName = clientChunkName;
	}
	
	public boolean isValid() {
		//TODO: Unimplemented method
		return true;
	}
	
	@Override
	public String toString() {
		String format = "Chunk[clientChunkName=%s, order=%s]";
		String result = String.format(format, clientChunkName, order);
		
		return result;
	}
}
