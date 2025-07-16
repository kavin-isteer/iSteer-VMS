package com.isteer.vms.service.impl;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.isteer.vms.dao.ComputerDao;
import com.isteer.vms.dao.VulnerabilityDao;
import com.isteer.vms.dto.ComputerPayloadDto;
import com.isteer.vms.dto.ComputerResponseDto;
import com.isteer.vms.dto.DashboardMetricsDto;
import com.isteer.vms.model.Computer;
import com.isteer.vms.service.ApplicationService;
import com.isteer.vms.service.ComputerService;

import lombok.extern.log4j.Log4j2;

@Log4j2
@Service
public class ComputerServiceImpl implements ComputerService {

	private ComputerDao computerDao;
	private ApplicationService applicationService;
	private VulnerabilityDao vulnerabilityDao;

	public ComputerServiceImpl(ComputerDao computerDao, ApplicationService applicationService, VulnerabilityDao vulnerabilityDao) {
		super();
		this.computerDao = computerDao;
		this.applicationService = applicationService;
		this.vulnerabilityDao = vulnerabilityDao;
	}

	@Override
	@Transactional
	public int createOrUpdateComputer(ComputerPayloadDto computer) {
	    log.info("Processing computer with deviceId: {}", computer.getDeviceId());

	    Optional<Computer> existingOpt = computerDao.getComputerByDeviceId(computer.getDeviceId());

	    if (existingOpt.isPresent()) {
	    	log.info("Found existing computer with deviceId: {}", computer.getDeviceId());
	        Computer existing = existingOpt.get();

	        if (existing.isDeleted()) return -1;
	        if (!existing.isActive()) return -2;

	        if (checkIfUpdateRequired(computer, existing)) {
	            log.debug("No update required for computer with deviceId: {}", computer.getDeviceId());
	        } else {
	            updateExistingComputer(existing, computer);
	            log.info("Updating existing computer with deviceId: {}", computer.getDeviceId());
	            int status = computerDao.createOrUpdateComputer(existing);
	            return finalizeStatus(status, applicationService.createOrUpdateApplication(existing.getUuid(), computer.getInstalledSoftwares()), false);
	        }
	        return finalizeStatus(0, applicationService.createOrUpdateApplication(existing.getUuid(), computer.getInstalledSoftwares()), true);
	    }

	    Computer newComputer = buildNewComputer(computer);
	    log.info("Creating new computer with deviceId: {}", computer.getDeviceId());
	    int computerStatus = computerDao.createOrUpdateComputer(newComputer);
	    int applicationStatus = applicationService.createOrUpdateApplication(newComputer.getUuid(), computer.getInstalledSoftwares());
	    
	    return finalizeStatus(computerStatus, applicationStatus, false);
	}
	
	private void updateExistingComputer(Computer existing, ComputerPayloadDto dto) {
	    existing.toBuilder()
	        .machineName(dto.getMachineName())
	        .ipAddress(dto.getIpAddress())
	        .osVersion(dto.getOsVersion())
	        .antiVirusStatus(dto.getAntivirusStatus())
	        .firewallStatus(dto.getFirewallStatus())
	        .loggedinUser(dto.getLoggedInUser())
	        .lastUpdateCheck(dto.getLastUpdateCheck())
	        .timestamp(dto.getTimestamp())
	        .build();
	    existing.updatedAtNow();
	}
	
	private Computer buildNewComputer(ComputerPayloadDto dto) {
	    return Computer.builder()
	        .uuid(UUID.randomUUID().toString())
	        .deviceId(dto.getDeviceId())
	        .machineName(dto.getMachineName())
	        .ipAddress(dto.getIpAddress())
	        .osVersion(dto.getOsVersion())
	        .antiVirusStatus(dto.getAntivirusStatus())
	        .firewallStatus(dto.getFirewallStatus())
	        .loggedinUser(dto.getLoggedInUser())
	        .lastUpdateCheck(dto.getLastUpdateCheck())
	        .timestamp(dto.getTimestamp())
	        .build();
	}
	
	private int finalizeStatus(int compStatus, int appStatus, boolean noUpdateRequired) {
	    if (appStatus == -1) return -3;
	    if (compStatus == 1 && appStatus == 1) return 4;
	    if (compStatus == 2 && appStatus == 0 && !noUpdateRequired) return 1;
	    if (compStatus == 2 && appStatus > 0 && !noUpdateRequired) return 2;
	    if (compStatus == 0 && appStatus == 0) return 0;
	    if (compStatus == 0 && appStatus < 0) return 3;
	    if (compStatus == 0 && appStatus == 2) return 3;
	    return -4;
	}


	private boolean checkIfUpdateRequired(ComputerPayloadDto payload, Computer existing) {
	    return isEqual(existing.getMachineName(), payload.getMachineName())
	        && isEqual(existing.getIpAddress(), payload.getIpAddress())
	        && isEqual(existing.getOsVersion(), payload.getOsVersion())
	        && isEqual(existing.getAntiVirusStatus(), payload.getAntivirusStatus())
	        && isEqual(existing.getFirewallStatus(), payload.getFirewallStatus())
	        && isEqual(existing.getLoggedinUser(), payload.getLoggedInUser())
	        && isEqual(existing.getLastUpdateCheck(), payload.getLastUpdateCheck())
	        && isTimestampEqual(existing.getTimestamp(), payload.getTimestamp());
	}
	
	private boolean isEqual(Object a, Object b) {
	    return a == null ? b == null : a.equals(b);
	}

	private boolean isTimestampEqual(LocalDateTime a, LocalDateTime b) {
	    if (a == null || b == null) return a == b;
	    return a.truncatedTo(ChronoUnit.SECONDS).equals(b.truncatedTo(ChronoUnit.SECONDS));
	}

	@Override
	public List<Computer> getAllComnputers(String status) {
		if(status != null && !status.isEmpty()) {
			log.info("Fetching all computers from the database with active status {}", status);
			return computerDao.getAllComputers(status);
		}
		log.info("Fetching all computers from the database.");
		return computerDao.getAllComputers();
	}

	@Override
	public DashboardMetricsDto getDashboardMetrics() {
		log.info("Loading Dashboard Metrics");
		return DashboardMetricsDto.builder()
				.totalComputers(computerDao.getTotalComputersCount())
				.vulnerableComputers(computerDao.getVulnerableComputersCount())
				.computerDetails(getComputerDetails())
				.build();
	}

//	private List<ComputerResponseDto> getComputerDetails() {
//		return computerDao.getAllComputers().stream()
//				.map(computer -> ComputerResponseDto.builder()
//						.uuid(computer.getUuid())
//						.deviceId(computer.getDeviceId())
//						.machineName(computer.getMachineName())
//						.ipAddress(computer.getIpAddress())
//						.osVersion(computer.getOsVersion())
//						.antivirusStatus(computer.getAntiVirusStatus())
//						.firewallStatus(computer.getFirewallStatus())
//						.loggedInUser(computer.getLoggedinUser())
//						.criticalVulnerabilityCount(0)
//						.highVulnerabilityCount(0)
//						.mediumVulnerabilityCount(0)
//						.lowVulnerabilityCount(0)
//						.applicationDetails(applicationService.getApplicationDetails(computer.getUuid()))
//						.build()).toList();
//	}
	
	private List<ComputerResponseDto> getComputerDetails() {
	    Map<String, Map<String, Integer>> vulnerabilityCounts = 
	        vulnerabilityDao.getVulnerabilityCountsByComputer();
	    Map<String, Integer> installedAppCounts = computerDao.getInstalledAppCounts();
	    Map<String, Integer> vulnerableAppCounts = computerDao.getVulnerableAppCounts(); 

	    return computerDao.getAllComputers().stream()
	            .map(computer -> {
	                Map<String, Integer> severityCountMap = 
	                    vulnerabilityCounts.getOrDefault(computer.getUuid(), Collections.emptyMap());

	                return ComputerResponseDto.builder()
	                        .uuid(computer.getUuid())
	                        .deviceId(computer.getDeviceId())
	                        .machineName(computer.getMachineName())
	                        .ipAddress(computer.getIpAddress())
	                        .osVersion(computer.getOsVersion())
	                        .antivirusStatus(computer.getAntiVirusStatus())
	                        .firewallStatus(computer.getFirewallStatus())
	                        .loggedInUser(computer.getLoggedinUser())
	                        .installedSoftwareCount(installedAppCounts.getOrDefault(computer.getUuid(), 0))
	                        .vulnerableSoftwareCount(vulnerableAppCounts.getOrDefault(computer.getUuid(), 0))
	                        .criticalVulnerabilityCount(severityCountMap.getOrDefault("CRITICAL", 0))
	                        .highVulnerabilityCount(severityCountMap.getOrDefault("HIGH", 0))
	                        .mediumVulnerabilityCount(severityCountMap.getOrDefault("MEDIUM", 0))
	                        .lowVulnerabilityCount(severityCountMap.getOrDefault("LOW", 0))
	                        .applicationDetails(applicationService.getApplicationDetails(computer.getUuid()))
	                        .build();
	            }).toList();
	}


}
