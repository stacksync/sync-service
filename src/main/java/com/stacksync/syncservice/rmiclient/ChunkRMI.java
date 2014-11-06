package com.stacksync.syncservice.rmiclient;

public class ChunkRMI {

	private Integer order = null;
	private String clientChunkName = null;
	
	public ChunkRMI() {
	}
	
	public ChunkRMI (String name , Integer order){
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
