package com.isteer.vms.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
@Builder(toBuilder = true)
public class SoftwarePayloadDto {

	@NotBlank(message = "Software name cannot be blank")
	private String softwareName;
	
	private String softwareVersion;
	private String vendorName;
	private LocalDateTime installedDate;
}
