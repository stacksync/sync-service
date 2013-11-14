package com.stacksync.syncservice.rpc.parser;

import com.stacksync.syncservice.rpc.messages.APIResponse;

public interface IParser {
	public String createResponse(APIResponse m);
}
