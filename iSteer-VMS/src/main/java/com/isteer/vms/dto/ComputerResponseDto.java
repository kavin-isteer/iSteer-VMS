package com.isteer.vms.dto;

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
	private String ipAddress;
	private String osVersion;
	private String antivirusStatus;
	private String firewallStatus;
	private String loggedInUser;
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
