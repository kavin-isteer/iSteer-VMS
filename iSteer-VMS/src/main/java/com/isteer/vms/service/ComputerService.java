package com.isteer.vms.service;

import com.isteer.vms.dto.ComputerPayloadDto;

public interface ComputerService {

	int createOrUpdateComputer(ComputerPayloadDto computer);

}
