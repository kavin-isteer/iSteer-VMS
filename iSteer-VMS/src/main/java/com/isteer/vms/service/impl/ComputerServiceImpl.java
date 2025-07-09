package com.isteer.vms.service.impl;

import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.isteer.vms.dao.ComputerDao;
import com.isteer.vms.dto.ComputerPayloadDto;
import com.isteer.vms.model.Computer;
import com.isteer.vms.service.ApplicationService;
import com.isteer.vms.service.ComputerService;

@Service
public class ComputerServiceImpl implements ComputerService {

	private static final Logger logger = LogManager.getLogger(ComputerServiceImpl.class);
	
	@Autowired
	private ComputerDao computerDao;

	@Autowired
	private ApplicationService applicationService;

	@Override
	@Transactional
	public int createOrUpdateComputer(ComputerPayloadDto computer) {
		boolean isDeleted = false;
		boolean isActive = true;
		boolean isUpdate = true;
		int computerStatus = 0;
		int applicationStatus = 0;

		logger.info("Processing computer with deviceId: {}", computer.getDeviceId());
		Optional<Computer> existingComputerOpt = computerDao.getComputerByDeviceId(computer.getDeviceId());
		Computer existingComputer = new Computer();

		if (existingComputerOpt.isPresent()) {
			existingComputer = existingComputerOpt.get();
			logger.debug("Checking the status of existing computer with deviceId: {}", computer.getDeviceId());
			isDeleted = existingComputerOpt.get().isDeleted();
			isActive = existingComputerOpt.get().isActive();
			if (!isDeleted && isActive) {
				logger.debug("Existing computer is active and not deleted. Proceeding with update check.");
				isUpdate = checkIfUpdateRequired(computer, existingComputer);
				if (!isUpdate) {
					logger.debug("Update required for computer with deviceId: {}", computer.getDeviceId());
					existingComputer.setMachineName(computer.getMachineName());
					existingComputer.setIpAddress(computer.getIpAddress());
					existingComputer.setOsVersion(computer.getOsVersion());
					existingComputer.setAntiVirusStatus(computer.getAntivirusStatus());
					existingComputer.setFirewallStatus(computer.getFirewallStatus());
					existingComputer.setLoggedinUser(computer.getLoggedInUser());
					existingComputer.setLastUpdateCheck(computer.getLastUpdateCheck());
					existingComputer.setTimestamp(computer.getTimestamp());
					existingComputer.updatedAtNow();

					logger.info("Updating existing computer with deviceId: {}", computer.getDeviceId());
					computerStatus = computerDao.createOrUpdateComputer(existingComputer);
				}
				logger.debug("No update required for computer with deviceId: {}", computer.getDeviceId());
			}
		} else {
			logger.info("No existing computer found with deviceId: {}", computer.getDeviceId());
			existingComputer.setUuid(UUID.randomUUID().toString());
			existingComputer.setDeviceId(computer.getDeviceId());
			existingComputer.setMachineName(computer.getMachineName());
			existingComputer.setIpAddress(computer.getIpAddress());
			existingComputer.setOsVersion(computer.getOsVersion());
			existingComputer.setAntiVirusStatus(computer.getAntivirusStatus());
			existingComputer.setFirewallStatus(computer.getFirewallStatus());
			existingComputer.setLoggedinUser(computer.getLoggedInUser());
			existingComputer.setLastUpdateCheck(computer.getLastUpdateCheck());
			existingComputer.setTimestamp(computer.getTimestamp());
			existingComputer.createdAtNow();
			logger.info("Creating new computer with deviceId: {}", computer.getDeviceId());
			computerStatus = computerDao.createOrUpdateComputer(existingComputer);
		}
		if (!isDeleted && isActive) {
			logger.info("Creating or updating applications for computer with deviceId: {}", computer.getDeviceId());
			applicationStatus = applicationService.createOrUpdateApplication(existingComputer.getUuid(),
					computer.getInstalledSoftwares());
		}

		if (isDeleted) {
			return -1;
		}
		if (!isActive)
			return -2;
		if(applicationStatus == -1) {
			return -3; // Error while processing application data
		}
		if (!isUpdate) {
			System.out.println("Inside !isUpdate");
			if (computerStatus == 2 && applicationStatus == 0) {
				return 1;
			}
			if (computerStatus == 2 && applicationStatus > 0) {
				return 2;
			}
			if (computerStatus == 0 && applicationStatus == 0) {
				return 0;
			}
			if (computerStatus == 0 && applicationStatus < 0) {
				return 3;
			}
		}
		if (computerStatus == 1 && applicationStatus == 1) {
			return 4;
		}
		if (computerStatus == 0 && applicationStatus == 0) {
			return 0;
		}
		if (computerStatus == 0 && applicationStatus == 2) {
			return 3;
		}
		return -4;
	}

	private boolean checkIfUpdateRequired(ComputerPayloadDto payload, Computer existingComputer) {
		return existingComputer.getMachineName().equals(payload.getMachineName())
				&& existingComputer.getIpAddress().equals(payload.getIpAddress())
				&& existingComputer.getOsVersion().equals(payload.getOsVersion())
				&& (existingComputer.getAntiVirusStatus() == null ? payload.getAntivirusStatus() == null
						: existingComputer.getAntiVirusStatus().equals(payload.getAntivirusStatus()))
				&& (existingComputer.getFirewallStatus() == null ? payload.getFirewallStatus() == null
						: existingComputer.getFirewallStatus().equals(payload.getFirewallStatus()))
				&& (existingComputer.getLoggedinUser() == null ? payload.getLoggedInUser() == null
						: existingComputer.getLoggedinUser().equals(payload.getLoggedInUser()))
				&& (existingComputer.getLastUpdateCheck() == null ? payload.getLastUpdateCheck() == null
						: existingComputer.getLastUpdateCheck().equals(payload.getLastUpdateCheck()))
				&& (existingComputer.getTimestamp() == null ? payload.getTimestamp() == null
						: existingComputer.getTimestamp().truncatedTo(ChronoUnit.SECONDS).equals(payload.getTimestamp().truncatedTo(ChronoUnit.SECONDS)));
	}

}
