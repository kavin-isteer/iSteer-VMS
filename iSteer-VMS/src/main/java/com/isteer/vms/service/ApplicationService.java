package com.isteer.vms.service;

import java.util.List;

import com.isteer.vms.dto.SoftwarePayloadDto;

public interface ApplicationService {
	
	int createOrUpdateApplication(String computerUuid, List<SoftwarePayloadDto> software);

}
