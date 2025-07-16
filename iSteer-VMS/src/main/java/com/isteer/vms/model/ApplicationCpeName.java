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
public class ApplicationCpeName {

	private Long id;
	private String uuid;
	private String applicationUuid;
	private String cpeName;
	@Builder.Default
	private boolean isResolvedCpe = false;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
}
