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
public class Application {

	private Long id;
	private String uuid;
	private String softwareName;
	private String softwareVersion;
	private String vendorName;
	private LocalDateTime createdAt;
}
