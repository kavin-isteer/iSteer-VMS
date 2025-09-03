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

import com.isteer.vms.dao.ApplicationDao;
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
	private ApplicationDao applicationDao;

	public ComputerServiceImpl(ComputerDao computerDao, ApplicationService applicationService,
			VulnerabilityDao vulnerabilityDao, ApplicationDao applicationDao) {
		this.computerDao = computerDao;
		this.applicationService = applicationService;
		this.vulnerabilityDao = vulnerabilityDao;
		this.applicationDao = applicationDao;
	}

	@Override
	@Transactional
	public int createOrUpdateComputer(ComputerPayloadDto computer) {
		log.info("Processing computer with deviceId: {}", computer.getDeviceId());

		Optional<Computer> existingOpt = computerDao.getComputerByDeviceId(computer.getDeviceId());

		if (existingOpt.isPresent()) {
			log.info("Found existing computer with deviceId: {}", computer.getDeviceId());
			Computer existing = existingOpt.get();

			if (existing.isDeleted()) {
				log.warn("Computer with deviceId: {} is deleted, cannot update.", computer.getDeviceId());
				return -1;
			}
			if (!existing.isActive()) {
				log.warn("Computer with deviceId: {} is inactive, cannot update.", computer.getDeviceId());
				return -2;
			}

			if (!checkIfUpdateRequired(computer, existing)) {
				existing = updateExistingComputer(existing, computer);
				log.info("Updating existing computer with deviceId: {}", computer.getDeviceId());
				int status = computerDao.createOrUpdateComputer(existing);
				return finalizeStatus(status, applicationService.createOrUpdateApplication(existing.getUuid(),
						computer.getInstalledSoftwares()), false);

			} else {
				log.info("Computer details are already up to date for computer UUID:  {}", computer.getDeviceId());
				return finalizeStatus(0, applicationService.createOrUpdateApplication(existing.getUuid(),
						computer.getInstalledSoftwares()), true);
			}

		}

		log.info("No existing computer found with deviceId: {}, creating a new one.", computer.getDeviceId());
		Computer newComputer = buildNewComputer(computer);
		log.info("Creating new computer with deviceId: {}", computer.getDeviceId());
		int computerStatus = computerDao.createOrUpdateComputer(newComputer);
		int applicationStatus = applicationService.createOrUpdateApplication(newComputer.getUuid(),
				computer.getInstalledSoftwares());

		return finalizeStatus(computerStatus, applicationStatus, false);
	}

	private Computer updateExistingComputer(Computer existing, ComputerPayloadDto dto) {
		Computer updated = existing.toBuilder().machineName(dto.getMachineName()).ipAddress(dto.getIpAddress())
				.osVersion(dto.getOsVersion()).antiVirusStatus(dto.getAntivirusStatus())
				.firewallStatus(dto.getFirewallStatus()).loggedinUserName(dto.getLoggedInUser().getUserName())
				.loggedInUserEmail(dto.getLoggedInUser().getUserEmail())
				.lastUpdateCheck(dto.getLastUpdateCheck()).timestamp(dto.getTimestamp()).build();
		existing.updatedAtNow();
		return updated;
	}

	private Computer buildNewComputer(ComputerPayloadDto dto) {
		return Computer.builder().uuid(UUID.randomUUID().toString()).deviceId(dto.getDeviceId())
				.machineName(dto.getMachineName()).ipAddress(dto.getIpAddress()).osVersion(dto.getOsVersion())
				.antiVirusStatus(dto.getAntivirusStatus()).firewallStatus(dto.getFirewallStatus())
				.loggedinUserName(dto.getLoggedInUser().getUserName()).loggedInUserEmail(dto.getLoggedInUser().getUserEmail()).lastUpdateCheck(dto.getLastUpdateCheck())
				.timestamp(dto.getTimestamp()).build();
	}

	private int finalizeStatus(int compStatus, int appStatus, boolean noUpdateRequired) {
		log.debug("Finalizing status with computer status: {}, application status: {}, noUpdateRequired: {}",
				compStatus, appStatus, noUpdateRequired);
		if (appStatus == -1)
			return -3;
		if (compStatus == 1 && appStatus == 1)
			return 4;
		if (compStatus == 2 && appStatus == 0 && !noUpdateRequired)
			return 1;
		if (compStatus == 2 && appStatus > 0 && !noUpdateRequired)
			return 2;
		if (compStatus == 0 && appStatus == 0)
			return 0;
		if (compStatus == 0 && appStatus == 1)
			return 3;
		if (compStatus == 0 && appStatus == 2)
			return 3;
		return -4;
	}

	private boolean checkIfUpdateRequired(ComputerPayloadDto payload, Computer existing) {
		log.debug("Checking if update is required for computer with deviceId: {}", existing.getDeviceId());
		return isEqual(existing.getMachineName(), payload.getMachineName())
				&& isEqual(existing.getIpAddress(), payload.getIpAddress())
				&& isEqual(existing.getOsVersion(), payload.getOsVersion())
				&& isEqual(existing.getAntiVirusStatus(), payload.getAntivirusStatus())
				&& isEqual(existing.getFirewallStatus(), payload.getFirewallStatus())
				&& isEqual(existing.getLoggedinUserName(), payload.getLoggedInUser().getUserName())
				&& isEqual(existing.getLoggedInUserEmail(), payload.getLoggedInUser().getUserEmail())
				&& isEqual(existing.getLastUpdateCheck(), payload.getLastUpdateCheck())
				&& isTimestampEqual(existing.getTimestamp(), payload.getTimestamp());
	}

	private boolean isEqual(Object a, Object b) {
		return a == null ? b == null : a.equals(b);
	}

	private boolean isTimestampEqual(LocalDateTime a, LocalDateTime b) {
		if (a == null || b == null)
			return a == b;
		return a.truncatedTo(ChronoUnit.MINUTES).equals(b.truncatedTo(ChronoUnit.MINUTES));
	}

	@Override
	public DashboardMetricsDto getDashboardMetrics() {
		log.info("Loading Dashboard Metrics...");
		Map<String, Integer> installedVulnerableAppCounts = applicationDao.getInstalledVulnerableAppCounts();
		return DashboardMetricsDto.builder().totalComputers(computerDao.getTotalComputersCount())
				.vulnerableComputers(computerDao.getVulnerableComputersCount())
				.totalCriticalVulnerableApplications(installedVulnerableAppCounts.getOrDefault("CRITICAL", 0))
				.totalHighVulnerableApplications(installedVulnerableAppCounts.getOrDefault("HIGH", 0))
				.totalMediumVulnerableApplications(installedVulnerableAppCounts.getOrDefault("MEDIUM", 0))
				.totalLowVulnerableApplications(installedVulnerableAppCounts.getOrDefault("LOW", 0))
				.computerDetails(getComputerDetails()).build();
	}

	private List<ComputerResponseDto> getComputerDetails() {
		log.debug("Fetching computer details for dashboard metrics");
		Map<String, Map<String, Integer>> vulnerabilityCounts = vulnerabilityDao.getVulnerabilityCountsByComputer();
		Map<String, Integer> installedAppCounts = computerDao.getInstalledAppCounts();
		Map<String, Integer> vulnerableAppCounts = computerDao.getVulnerableAppCounts();

		return computerDao.getAllComputers().stream().map(computer -> {
			Map<String, Integer> severityCountMap = vulnerabilityCounts.getOrDefault(computer.getUuid(),
					Collections.emptyMap());

			return ComputerResponseDto.builder().uuid(computer.getUuid()).deviceId(computer.getDeviceId())
					.machineName(computer.getMachineName()).ipAddress(computer.getIpAddress())
					.osVersion(computer.getOsVersion()).antivirusStatus(computer.getAntiVirusStatus())
					.firewallStatus(computer.getFirewallStatus()).loggedInUserName(computer.getLoggedinUserName())
					.loggedInUserEmail(computer.getLoggedInUserEmail())
					.installedSoftwareCount(installedAppCounts.getOrDefault(computer.getUuid(), 0))
					.vulnerableSoftwareCount(vulnerableAppCounts.getOrDefault(computer.getUuid(), 0))
					.criticalVulnerableApplicationCount(severityCountMap.getOrDefault("CRITICAL", 0))
					.highVulnerableApplicationCount(severityCountMap.getOrDefault("HIGH", 0))
					.mediumVulnerableApplicationCount(severityCountMap.getOrDefault("MEDIUM", 0))
					.lowVulnerableApplicationCount(severityCountMap.getOrDefault("LOW", 0))
					.applicationDetails(applicationService.getApplicationDetails(computer.getUuid())).build();
		}).toList();
	}
	@Override
	public List<ComputerResponseDto> getAllComputersWithVulnerabilities() {
		log.info("Fetching all computers with vulnerabilities.");
		return computerDao.getAllComputersWithVulnerabilities();
	}

	@Override
	public ComputerResponseDto getComputerWithVulnerabilitiesByUuid(String computerUuid) {
		log.info("Fetching computer with vulnerabilities by UUID: {}", computerUuid);
		return computerDao.getComputerWithVulnerabilitiesByUuid(computerUuid);
	}
}
