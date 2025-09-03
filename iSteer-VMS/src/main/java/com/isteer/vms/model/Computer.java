package com.isteer.vms.model;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
@Builder(toBuilder = true)
public class Computer {
	
	private Long id;
	private String uuid;
	private String deviceId;
	private String machineName;
	private String ipAddress;
	private String osVersion;
	private String antiVirusStatus;
	private String firewallStatus;
	private String loggedinUserName;
	private String loggedInUserEmail;
	private LocalDateTime lastUpdateCheck;
	private LocalDateTime timestamp;
	@Builder.Default
	private boolean isActive = true;
	@Builder.Default
	private boolean isDeleted = false;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
	
	public void updatedAtNow() {
        this.updatedAt = java.time.LocalDateTime.now();
    }
	
}
