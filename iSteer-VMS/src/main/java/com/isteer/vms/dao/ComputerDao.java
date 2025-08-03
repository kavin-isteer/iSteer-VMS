package com.isteer.vms.dao;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.isteer.vms.dto.ComputerResponseDto;
import com.isteer.vms.model.Computer;

public interface ComputerDao {

	List<Computer> getAllComputers();
	
	List<Computer> getAllComputers(String status);
	
	Optional<Computer> getComputerByDeviceId(String deviceId);

	int createOrUpdateComputer(Computer computer);

	int getTotalComputersCount();

	int getVulnerableComputersCount();

	Map<String, Integer> getInstalledAppCounts();

	Map<String, Integer> getVulnerableAppCounts();
	
	List<ComputerResponseDto> getAllComputersWithVulnerabilities();
}
