package com.isteer.vms.dao;

import java.util.List;
import java.util.Optional;

import com.isteer.vms.model.Computer;

public interface ComputerDao {

	List<Computer> getAllComputers();
	
	Optional<Computer> getComputerByDeviceId(String deviceId);

	int createOrUpdateComputer(Computer computer);
}
