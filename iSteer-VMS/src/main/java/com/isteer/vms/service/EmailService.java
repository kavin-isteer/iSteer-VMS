package com.isteer.vms.service;

import java.util.List;

import com.isteer.vms.dto.ComputerResponseDto;

public interface EmailService {

	void sendVulnEmailNotification(ComputerResponseDto data);
	void sendVulnEmailNotifications(List<ComputerResponseDto> data);

}
