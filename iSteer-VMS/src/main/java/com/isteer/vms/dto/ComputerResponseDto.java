package com.isteer.vms.dto;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
@Builder(toBuilder = true)
public class ComputerResponseDto {
	
	private String uuid;
	private String deviceId;
	private String machineName;
	private String serialNumber;
	private String macAddress;
	private String ipAddress;
	private String osVersion;
	private String antivirusStatus;
	private String firewallStatus;
	private String loggedInUserName;
	private String loggedInUserEmail;
	private LocalDateTime updatedAt;
	private LocalDateTime createdAt;
	@Builder.Default
	private int installedSoftwareCount = 0;
	@Builder.Default
	private int vulnerableSoftwareCount = 0;
	@Builder.Default
	private int criticalVulnerableApplicationCount = 0;
	@Builder.Default
	private int highVulnerableApplicationCount = 0;
	@Builder.Default
	private int mediumVulnerableApplicationCount = 0;
	@Builder.Default
	private int lowVulnerableApplicationCount = 0;
	private List<ApplicationResponseDto> applicationDetails;

}
