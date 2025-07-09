package com.isteer.vms.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;

public class SoftwarePayloadDto {

	@NotBlank(message = "Software name cannot be blank")
	private String softwareName;
	
	private String softwareVersion;
	private String vendorName;
	private LocalDateTime installedDate;
	
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
	
	@Override
	public String toString() {
		return "SoftwarePayloadDto [softwareName=" + softwareName + ", softwareVersion=" + softwareVersion
				+ ", vendorName=" + vendorName + ", installedDate=" + installedDate + "]";
	}
	
}
