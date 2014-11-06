package com.stacksync.syncservice.rmiclient;

import java.io.Serializable;


public class CommitInfoRMI implements Serializable {

	private static final long serialVersionUID = -1205107021066864318L;
	private Long committedVersion;
	private boolean commitSucceed;
	private ItemMetadataRMI metadata;

	public CommitInfoRMI(Long committedVersion, boolean commitSucceed,
			ItemMetadataRMI metadata) {

		this.committedVersion = committedVersion;
		this.commitSucceed = commitSucceed;
		this.metadata = metadata;

	}

	public long getCommittedVersion() {
		return committedVersion;
	}

	public void setCommittedVersion(Long committedVersion) {
		this.committedVersion = committedVersion;
	}

	public boolean isCommitSucceed() {
		return commitSucceed;
	}

	public void setCommitSucceed(boolean commitSucceed) {
		this.commitSucceed = commitSucceed;
	}

	public ItemMetadataRMI getMetadata() {
		return metadata;
	}

	public void setMetadata(ItemMetadataRMI metadata) {
		this.metadata = metadata;
	}

}
