package com.isteer.vms.service;

import java.util.List;

import com.isteer.vms.dto.ApplicationResponseDto;
import com.isteer.vms.dto.SoftwarePayloadDto;
import com.isteer.vms.model.Application;

public interface ApplicationService {
	
	int createOrUpdateApplication(String computerUuid, List<SoftwarePayloadDto> software);

	List<Application> getAllApplications(String isVulnerable);
	
	List<ApplicationResponseDto> getApplicationDetails(String computerUuid);

}
