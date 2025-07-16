package com.isteer.vms.dto;

import java.util.List;

import com.isteer.vms.model.Vulnerability;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
@Builder(toBuilder = true)
public class ApplicationResponseDto {

	private String uuid;
	private String softwareName;
	private String softwareVersion;
	private String vendor;
	@Builder.Default
	private int criticalVulnerabilityCount = 0;
	@Builder.Default
	private int highVulnerabilityCount = 0;
	@Builder.Default
	private int mediumVulnerabilityCount = 0;
	@Builder.Default
	private int lowVulnerabilityCount =0;
	private List<Vulnerability> vulnerabilities;
}
