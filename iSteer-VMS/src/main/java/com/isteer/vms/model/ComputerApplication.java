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
public class ComputerApplication {

	private String uuid;
	private String applicationUuid;
	private String computerUuid;
	private String softwareName;
	private String softwareVersion;
	private String vendorName;
	private LocalDateTime installedDate;
	@Builder.Default
	private boolean isDeleted = false;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
}
