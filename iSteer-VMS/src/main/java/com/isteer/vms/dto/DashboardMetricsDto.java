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
public class DashboardMetricsDto {
	
	@Builder.Default
	private int totalComputers = 0;
	@Builder.Default
	private int vulnerableComputers = 0;
	private List<ComputerResponseDto> computerDetails;

}
