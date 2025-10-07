package com.isteer.vms.service.impl;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.isteer.vms.dao.ApplicationDao;
import com.isteer.vms.dao.ComputerDao;
import com.isteer.vms.dao.VulnerabilityDao;
import com.isteer.vms.dto.ApplicationResponseDto;
import com.isteer.vms.dto.ComputerPayloadDto;
import com.isteer.vms.dto.ComputerResponseDto;
import com.isteer.vms.dto.DashboardMetricsDto;
import com.isteer.vms.exception.BusinessException;
import com.isteer.vms.model.Computer;
import com.isteer.vms.model.Vulnerability;
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
	public int createOrUpdateComputer(ComputerPayloadDto computerPayload) {
		log.info("Processing computer with deviceId: {}", computerPayload.getDeviceId());
		Optional<Computer> existingComputerOpt = computerDao.getComputerByDeviceId(computerPayload.getDeviceId());
		if (existingComputerOpt.isPresent()) {
			log.info("Found existing computer with deviceId: {}", computerPayload.getDeviceId());
			Computer existingComputer = existingComputerOpt.get();

			if (existingComputer.isDeleted()) {
				log.warn("Computer with deviceId: {} is deleted, cannot update.", computerPayload.getDeviceId());
				return -1;
			}
			if (!existingComputer.isActive()) {
				log.warn("Computer with deviceId: {} is inactive, cannot update.", computerPayload.getDeviceId());
				return -2;
			}

			if (!checkIfUpdateRequired(computerPayload, existingComputer)) {
				existingComputer = updateExistingComputer(existingComputer, computerPayload);
				log.info("Updating existing computer with deviceId: {}", computerPayload.getDeviceId());
				int updateStatus = computerDao.updateComputer(existingComputer);
				if (updateStatus == 0) {
					log.error("Failed to update computer with deviceId: {}", computerPayload.getDeviceId());
					return -5;
				}
				return finalizeStatus(updateStatus, applicationService.createOrUpdateApplication(existingComputer.getUuid(),
						computerPayload.getInstalledSoftwares()), false);

			} else {
				log.info("Computer details are already up to date for computer UUID:  {}", computerPayload.getDeviceId());
				return finalizeStatus(0, applicationService.createOrUpdateApplication(existingComputer.getUuid(),
						computerPayload.getInstalledSoftwares()), true);
			}

		}

		log.info("No existing computer found with deviceId: {}, creating a new one.", computerPayload.getDeviceId());
		Computer newComputer = buildNewComputer(computerPayload);
		log.info("Creating new computer with deviceId: {}", computerPayload.getDeviceId());
		int createStatus = computerDao.createComputer(newComputer);
		if (createStatus == 0) {
			log.error("Failed to create new computer with deviceId: {}", computerPayload.getDeviceId());
			return -6;
		}
		int applicationStatus = applicationService.createOrUpdateApplication(newComputer.getUuid(),
				computerPayload.getInstalledSoftwares());

		return finalizeStatus(createStatus, applicationStatus, false);
	}

	private Computer updateExistingComputer(Computer existingComputer, ComputerPayloadDto computerPayload) {
		Computer updatedComputer = existingComputer.toBuilder().machineName(computerPayload.getMachineName()).ipAddress(computerPayload.getIpAddress())
				.osVersion(computerPayload.getOsVersion()).antiVirusStatus(computerPayload.getAntivirusStatus())
				.firewallStatus(computerPayload.getFirewallStatus()).loggedinUserName(computerPayload.getLoggedInUser().getUserName())
				.loggedInUserEmail(computerPayload.getLoggedInUser().getUserEmail()).lastUpdateCheck(computerPayload.getLastUpdateCheck())
				.timestamp(computerPayload.getTimestamp()).build();
		existingComputer.updatedAtNow();
		return updatedComputer;
	}

	private Computer buildNewComputer(ComputerPayloadDto computerPayload) {
		return Computer.builder().uuid(UUID.randomUUID().toString()).deviceId(computerPayload.getDeviceId())
				.machineName(computerPayload.getMachineName()).serialNumber(computerPayload.getSerialNumber()).macAddress(computerPayload.getMacAddress())
				.ipAddress(computerPayload.getIpAddress()).osVersion(computerPayload.getOsVersion()).antiVirusStatus(computerPayload.getAntivirusStatus())
				.firewallStatus(computerPayload.getFirewallStatus()).loggedinUserName(computerPayload.getLoggedInUser().getUserName())
				.loggedInUserEmail(computerPayload.getLoggedInUser().getUserEmail()).lastUpdateCheck(computerPayload.getLastUpdateCheck())
				.timestamp(computerPayload.getTimestamp()).build();
	}

	private int finalizeStatus(int computerStatus, int applicationStatus, boolean noUpdateRequired) {
		log.debug("Finalizing status with computer status: {}, application status: {}, noUpdateRequired: {}",
				computerStatus, applicationStatus, noUpdateRequired);
		if (computerStatus == 1 && applicationStatus == 0 && !noUpdateRequired)
			return 1;
		if (computerStatus == 1 && applicationStatus > 0 && !noUpdateRequired)
			return 2;
		if (applicationStatus == -1)
			return -3;
		if (computerStatus == 1 && applicationStatus == 1)
			return 4;
		if (computerStatus == 0 && applicationStatus == 0)
			return 0;
		if (computerStatus == 0 && applicationStatus == 1)
			return 3;
		if (computerStatus == 0 && applicationStatus == 2)
			return 3;
		return -4;
	}

	private boolean checkIfUpdateRequired(ComputerPayloadDto computerPayload, Computer existingComputer) {
		log.debug("Checking if update is required for computer with deviceId: {}", existingComputer.getDeviceId());
		return isEqual(existingComputer.getMachineName(), computerPayload.getMachineName())
				&& isEqual(existingComputer.getSerialNumber(), computerPayload.getSerialNumber())
				&& isEqual(existingComputer.getMacAddress(), computerPayload.getMacAddress())
				&& isEqual(existingComputer.getIpAddress(), computerPayload.getIpAddress())
				&& isEqual(existingComputer.getOsVersion(), computerPayload.getOsVersion())
				&& isEqual(existingComputer.getAntiVirusStatus(), computerPayload.getAntivirusStatus())
				&& isEqual(existingComputer.getFirewallStatus(), computerPayload.getFirewallStatus())
				&& isEqual(existingComputer.getLoggedinUserName(), computerPayload.getLoggedInUser().getUserName())
				&& isEqual(existingComputer.getLoggedInUserEmail(), computerPayload.getLoggedInUser().getUserEmail())
				&& isEqual(existingComputer.getLastUpdateCheck(), computerPayload.getLastUpdateCheck())
				&& isTimestampEqual(existingComputer.getTimestamp(), computerPayload.getTimestamp());
	}

	private boolean isEqual(Object a, Object b) {
		return a == null ? b == null : a.equals(b);
	}

	private boolean isTimestampEqual(LocalDateTime a, LocalDateTime b) {
		if (a == null || b == null)
			return a == b;
		return a.truncatedTo(ChronoUnit.SECONDS).equals(b.truncatedTo(ChronoUnit.SECONDS));
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
					.machineName(computer.getMachineName()).serialNumber(computer.getSerialNumber())
					.macAddress(computer.getMacAddress()).ipAddress(computer.getIpAddress())
					.osVersion(computer.getOsVersion()).antivirusStatus(computer.getAntiVirusStatus())
					.firewallStatus(computer.getFirewallStatus()).loggedInUserName(computer.getLoggedinUserName())
					.loggedInUserEmail(computer.getLoggedInUserEmail()).updatedAt(computer.getUpdatedAt())
					.createdAt(computer.getCreatedAt())
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

	@Override
	public ComputerResponseDto getComputerByUuid(String uuid) {
		log.info("Fetching computer details for UUID: {}", uuid);

		// Get computer basic info
		Computer computer = computerDao.findByUuid(uuid)
				.orElseThrow(() -> new BusinessException("Computer not found with UUID: " + uuid, 404));

		// Get applications for this computer (simplified - no grouping needed)
		List<ApplicationResponseDto> applications = getApplicationsForComputer(uuid);

		// Calculate vulnerability counts at computer level
		VulnerabilityCountsSummary summary = calculateComputerVulnerabilitySummary(applications);

		return ComputerResponseDto.builder().uuid(computer.getUuid()).deviceId(computer.getDeviceId())
				.machineName(computer.getMachineName()).serialNumber(computer.getSerialNumber())
				.macAddress(computer.getMacAddress()).ipAddress(computer.getIpAddress())
				.osVersion(computer.getOsVersion()).antivirusStatus(computer.getAntiVirusStatus())
				.firewallStatus(computer.getFirewallStatus()).loggedInUserName(computer.getLoggedinUserName())
				.loggedInUserEmail(computer.getLoggedInUserEmail()).createdAt(computer.getCreatedAt())
				.updatedAt(computer.getUpdatedAt()).installedSoftwareCount(applications.size())
				.vulnerableSoftwareCount(summary.vulnerableSoftwareCount)
				.criticalVulnerableApplicationCount(summary.criticalAppsCount)
				.highVulnerableApplicationCount(summary.highAppsCount)
				.mediumVulnerableApplicationCount(summary.mediumAppsCount)
				.lowVulnerableApplicationCount(summary.lowAppsCount).applicationDetails(applications).build();
	}

	// Simplified method - no complex grouping needed
	private List<ApplicationResponseDto> getApplicationsForComputer(String computerUuid) {
		List<Map<String, Object>> applicationData = applicationDao.findApplicationsByComputerUuid(computerUuid);

		return applicationData.stream()
				.map(this::buildApplicationResponseDto)
				.collect(Collectors.toList());
	}

	// Simplified DTO builder
	private ApplicationResponseDto buildApplicationResponseDto(Map<String, Object> appData) {
		String applicationUuid = (String) appData.get("application_uuid");
		String processIdsJson = (String) appData.get("process_ids");

		// Parse process IDs
		List<Integer> processIds = applicationDao.parseProcessIds(processIdsJson);

		// Get vulnerabilities for this application
		List<Vulnerability> vulnerabilities = vulnerabilityDao.findByApplicationUuid(applicationUuid);

		// Calculate vulnerability counts
		VulnerabilityCountsApp vulnCounts = calculateApplicationVulnerabilityCounts(vulnerabilities);

		// Get CPE name directly (no grouping needed)
		String cpeName = (String) appData.get("cpe_name");
		if (cpeName == null) {
			cpeName = ""; // Default to empty string if no CPE name
		}

		// Check if resolved
		Boolean isResolvedCpe = (Boolean) appData.get("is_resolved_cpe");
		boolean isResolved = isResolvedCpe != null && isResolvedCpe;

		return ApplicationResponseDto.builder().uuid(applicationUuid)
				.softwareName((String) appData.get("software_name"))
				.softwareVersion((String) appData.get("software_version")).vendor((String) appData.get("vendor"))
				.runningProcessIds(processIds).cpeName(cpeName).isResolved(isResolved)
				.criticalVulnerabilityCount(vulnCounts.critical).highVulnerabilityCount(vulnCounts.high)
				.mediumVulnerabilityCount(vulnCounts.medium).lowVulnerabilityCount(vulnCounts.low)
				.vulnerabilities(vulnerabilities).build();
	}

	private VulnerabilityCountsApp calculateApplicationVulnerabilityCounts(List<Vulnerability> vulnerabilities) {
		int critical = 0, high = 0, medium = 0, low = 0;

		for (Vulnerability vuln : vulnerabilities) {
			switch (vuln.getSeverity()) {
			case CRITICAL -> critical++;
			case HIGH -> high++;
			case MEDIUM -> medium++;
			case LOW -> low++;
			}
		}

		return new VulnerabilityCountsApp(critical, high, medium, low);
	}

	private VulnerabilityCountsSummary calculateComputerVulnerabilitySummary(
			List<ApplicationResponseDto> applications) {
		int vulnerableSoftwareCount = 0;
		int criticalAppsCount = 0;
		int  highAppsCount = 0;
		int  mediumAppsCount = 0;
		int  lowAppsCount = 0;

		for (ApplicationResponseDto app : applications) {
			// Count as vulnerable if it has any vulnerabilities
			if (app.getCriticalVulnerabilityCount() > 0 || app.getHighVulnerabilityCount() > 0
					|| app.getMediumVulnerabilityCount() > 0 || app.getLowVulnerabilityCount() > 0) {
				vulnerableSoftwareCount++;
			}

			// Count applications by their highest severity vulnerability
			if (app.getCriticalVulnerabilityCount() > 0) {
				criticalAppsCount++;
			} else if (app.getHighVulnerabilityCount() > 0) {
				highAppsCount++;
			} else if (app.getMediumVulnerabilityCount() > 0) {
				mediumAppsCount++;
			} else if (app.getLowVulnerabilityCount() > 0) {
				lowAppsCount++;
			}
		}

		return new VulnerabilityCountsSummary(vulnerableSoftwareCount, criticalAppsCount, highAppsCount,
				mediumAppsCount, lowAppsCount);
	}

	// Helper records
	private record VulnerabilityCountsApp(int critical, int high, int medium, int low) {
	}

	private record VulnerabilityCountsSummary(int vulnerableSoftwareCount, int criticalAppsCount, int highAppsCount,
			int mediumAppsCount, int lowAppsCount) {
	}
}
