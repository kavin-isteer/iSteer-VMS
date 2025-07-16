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
	private int criticalVulnerabilityCount = 0;
	@Builder.Default
	private int highVulnerabilityCount = 0;
	@Builder.Default
	private int mediumVulnerabilityCount = 0;
	@Builder.Default
	private int lowVulnerabilityCount = 0;
	private List<ApplicationResponseDto> applicationDetails;

}
