package com.stacksync.syncservice.util;

import org.apache.log4j.PatternLayout;

public class NoStackTracePatternLayout extends PatternLayout {

	@Override
	public boolean ignoresThrowable() {
		return false;
	}
}