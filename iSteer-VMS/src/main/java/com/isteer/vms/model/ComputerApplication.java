package com.isteer.vms.model;

import java.time.LocalDateTime;

public class ComputerApplication {

	private String uuid;
	private String applicationUuid;
	private String computerUuid;
	private String softwareName;
	private String softwareVersion;
	private String vendorName;
	private LocalDateTime installedDate;
	private boolean isDeleted = false;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
	
	public String getUuid() {
		return uuid;
	}
	public void setUuid(String uuid) {
		this.uuid = uuid;
	}
	public String getApplicationUuid() {
		return applicationUuid;
	}
	public void setApplicationUuid(String applicationUuid) {
		this.applicationUuid = applicationUuid;
	}
	public String getComputerUuid() {
		return computerUuid;
	}
	public void setComputerUuid(String computerUuid) {
		this.computerUuid = computerUuid;
	}
	public String getSoftwareName() {
		return softwareName;
	}
	public void setSoftwareName(String softwareName) {
		this.softwareName = softwareName;
	}
	public String getSoftwareVersion() {
		return softwareVersion;
	}
	public void setSoftwareVersion(String softwareVersion) {
		this.softwareVersion = softwareVersion;
	}
	public String getVendorName() {
		return vendorName;
	}
	public void setVendorName(String vendorName) {
		this.vendorName = vendorName;
	}
	public LocalDateTime getInstalledDate() {
		return installedDate;
	}
	public void setInstalledDate(LocalDateTime installedDate) {
		this.installedDate = installedDate;
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
	
	@Override
	public String toString() {
		return "ComputerApplication [uuid=" + uuid + ", applicationUuid=" + applicationUuid + ", computerUuid="
				+ computerUuid + ", softwareName=" + softwareName + ", softwareVersion=" + softwareVersion
				+ ", vendorName=" + vendorName + ", installedDate=" + installedDate + ", isDeleted=" + isDeleted
				+ ", createdAt=" + createdAt + ", updatedAt=" + updatedAt + "]";
	}
	
}
