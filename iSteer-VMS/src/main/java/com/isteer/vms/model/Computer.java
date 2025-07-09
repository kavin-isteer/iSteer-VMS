package com.isteer.vms.model;

import java.time.LocalDateTime;

public class Computer {
	
	private Long id;
	private String uuid;
	private String deviceId;
	private String machineName;
	private String ipAddress;
	private String osVersion;
	private String antiVirusStatus;
	private String firewallStatus;
	private String loggedinUser;
	private LocalDateTime lastUpdateCheck;
	private LocalDateTime timestamp;
	private boolean isActive = true;
	private boolean isDeleted = false;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
	
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public String getUuid() {
		return uuid;
	}
	public void setUuid(String uuid) {
		this.uuid = uuid;
	}
	public String getDeviceId() {
		return deviceId;
	}
	public void setDeviceId(String deviceId) {
		this.deviceId = deviceId;
	}
	public String getMachineName() {
		return machineName;
	}
	public void setMachineName(String machineName) {
		this.machineName = machineName;
	}
	public String getIpAddress() {
		return ipAddress;
	}
	public void setIpAddress(String ipAddress) {
		this.ipAddress = ipAddress;
	}
	public String getOsVersion() {
		return osVersion;
	}
	public void setOsVersion(String osVersion) {
		this.osVersion = osVersion;
	}
	public String getAntiVirusStatus() {
		return antiVirusStatus;
	}
	public void setAntiVirusStatus(String antiVirusStatus) {
		this.antiVirusStatus = antiVirusStatus;
	}
	public String getFirewallStatus() {
		return firewallStatus;
	}
	public void setFirewallStatus(String firewallStatus) {
		this.firewallStatus = firewallStatus;
	}
	public String getLoggedinUser() {
		return loggedinUser;
	}
	public void setLoggedinUser(String loggedinUser) {
		this.loggedinUser = loggedinUser;
	}
	public LocalDateTime getLastUpdateCheck() {
		return lastUpdateCheck;
	}
	public void setLastUpdateCheck(LocalDateTime lastUpdateCheck) {
		this.lastUpdateCheck = lastUpdateCheck;
	}
	public LocalDateTime getTimestamp() {
		return timestamp;
	}
	public void setTimestamp(LocalDateTime timestamp) {
		this.timestamp = timestamp;
	}
	public boolean isActive() {
		return isActive;
	}
	public void setActive(boolean isActive) {
		this.isActive = isActive;
	}
	public boolean isDeleted() {
		return isDeleted;
	}
	public void setDeleted(boolean isDeleted) {
		this.isDeleted = isDeleted;
	}
	public LocalDateTime getCreatedAt() {
		return createdAt;
	}
	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}
	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}
	public void setUpdatedAt(LocalDateTime updatedAt) {
		this.updatedAt = updatedAt;
	}
	
	public void createdAtNow() {
		this.createdAt = LocalDateTime.now();
	}
	
	public void updatedAtNow() {
		this.updatedAt = LocalDateTime.now();
	}
	
	@Override
	public String toString() {
		return "Computer [id=" + id + ", uuid=" + uuid + ", deviceId=" + deviceId + ", machineName=" + machineName
				+ ", ipAddress=" + ipAddress + ", osVersion=" + osVersion + ", antiVirusStatus=" + antiVirusStatus
				+ ", firewallStatus=" + firewallStatus + ", loggedinUser=" + loggedinUser + ", lastUpdateCheck="
				+ lastUpdateCheck + ", timestamp=" + timestamp + ", isActive=" + isActive + ", isDeleted=" + isDeleted
				+ ", createdAt=" + createdAt + ", updatedAt=" + updatedAt + "]";
	}
}
