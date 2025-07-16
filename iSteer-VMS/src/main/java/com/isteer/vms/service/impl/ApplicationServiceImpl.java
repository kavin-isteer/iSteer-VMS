package com.isteer.vms.service.impl;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.isteer.vms.dao.ApplicationDao;
import com.isteer.vms.dao.VulnerabilityDao;
import com.isteer.vms.dto.ApplicationResponseDto;
import com.isteer.vms.dto.SoftwarePayloadDto;
import com.isteer.vms.model.Application;
import com.isteer.vms.model.ComputerApplication;
import com.isteer.vms.service.ApplicationService;
import com.isteer.vms.service.VulnerabilityService;

import lombok.extern.log4j.Log4j2;

@Log4j2
@Service
public class ApplicationServiceImpl implements ApplicationService {

	private final ApplicationDao applicationDao;
	private final VulnerabilityService vulnerabilityService;
	private final VulnerabilityDao vulnerabilityDao;

	public ApplicationServiceImpl(ApplicationDao applicationDao, VulnerabilityService vulnerabilityService, VulnerabilityDao vulnerabilityDao) {
		this.applicationDao = applicationDao;
		this.vulnerabilityService = vulnerabilityService;
		this.vulnerabilityDao = vulnerabilityDao;
	}

	@Override
	@Transactional
	public int createOrUpdateApplication(String computerUuid, List<SoftwarePayloadDto> software) {
		log.info("Processing applications for computer UUID: {}", computerUuid);

		List<Application> existingApps = applicationDao.getAllApplications();
		List<ComputerApplication> existingCompApps = applicationDao.getApplicationsByComputerUuid(computerUuid);

		Set<String> appKeys = toKeySet(existingApps);
		Set<String> compAppKeys = toKeySetFromCompApps(existingCompApps);

		List<SoftwarePayloadDto> newSoftware = findNewSoftware(software, appKeys);
		if (!newSoftware.isEmpty()) {
			List<Application> inserted = createNewApplications(newSoftware);
			if (inserted == null || inserted.isEmpty())
				return -1;
			existingApps = applicationDao.getAllApplications(); // refresh
			appKeys.addAll(toKeySet(inserted));
		}

		int deleted = handleDeletedApplications(software, existingCompApps);
		int activated = handleActivatedApplications(software, existingCompApps);
		int mapped = handleNewMappings(computerUuid, software, existingApps, appKeys, compAppKeys);

		return determineFinalStatus(mapped, deleted, activated);
	}

	private Set<String> toKeySet(List<? extends Application> apps) {
		return apps.stream().map(app -> key(app.getSoftwareName(), app.getSoftwareVersion(), app.getVendorName()))
				.collect(Collectors.toSet());
	}

	private Set<String> toKeySetFromCompApps(List<ComputerApplication> compApps) {
		return compApps.stream().map(this::key).collect(Collectors.toSet());
	}

	private List<SoftwarePayloadDto> findNewSoftware(List<SoftwarePayloadDto> software, Set<String> existingKeys) {
		return software.stream().filter(s -> !existingKeys.contains(key(s))).toList();
	}

	private int handleDeletedApplications(List<SoftwarePayloadDto> software, List<ComputerApplication> existingCompApps) {
	    List<ComputerApplication> toDelete = existingCompApps.stream()
	        .filter(ca -> !ca.isDeleted() && software.stream().noneMatch(s -> key(s).equals(key(ca))))
	        .map(ca -> {
	            ca.setDeleted(true);
	            return ca;
	        })
	        .toList();

	    if (toDelete.isEmpty()) return 0;

	    log.info("Deleting {} stale computer application mappings", toDelete.size());
	    return applicationDao.deleteOrActivateComputerApplications(toDelete);
	}


	private int handleActivatedApplications(List<SoftwarePayloadDto> software, List<ComputerApplication> existingCompApps) {
	    
	    List<ComputerApplication> toActivate = existingCompApps.stream()
	        .filter(ComputerApplication::isDeleted)
	        .filter(ca -> software.stream().anyMatch(s -> key(s).equals(key(ca))))
	        .map(ca -> {
	            ca.setDeleted(false);
	            return ca;
	        })
	        .toList();

	    if (toActivate.isEmpty()) return 0;

	    log.info("Reactivating {} application mappings", toActivate.size());
	    return applicationDao.deleteOrActivateComputerApplications(toActivate);
	}


	private int handleNewMappings(String computerUuid, List<SoftwarePayloadDto> software,
			List<Application> existingApps, Set<String> appKeys, Set<String> compAppKeys) {
		List<String> newKeys = software.stream().map(this::key)
				.filter(k -> !compAppKeys.contains(k) && appKeys.contains(k)).toList();

		if (newKeys.isEmpty())
			return 0;

		List<Application> newMappings = existingApps.stream().filter(app -> newKeys.contains(key(app)))
				.toList();

		List<ComputerApplication> computerApplications = mapApplicationsToComputer(computerUuid, newMappings);
		mergeInstalledDates(computerApplications, software);
		return applicationDao.insertComputerApplications(computerApplications);
	}

	private void mergeInstalledDates(List<ComputerApplication> mappedApps, List<SoftwarePayloadDto> software) {
		mappedApps.forEach(app -> software.stream().filter(s -> key(s).equals(key(app))).findFirst()
				.ifPresent(s -> app.setInstalledDate(s.getInstalledDate())));
	}

	private int determineFinalStatus(int mapped, int deleted, int activated) {
		if (mapped == 0 && deleted == 0 && activated == 0)
			return 0;
		if (mapped == 1 && deleted == 0 && activated == 0)
			return 1;
		if (mapped == 1 || deleted == 1 || activated == 1)
			return 2;
		return -1;
	}

	private String key(String name, String version, String vendor) {
		return name + "|" + version + "|" + vendor;
	}

	private String key(SoftwarePayloadDto dto) {
		return key(dto.getSoftwareName(), dto.getSoftwareVersion(), dto.getVendorName());
	}

	private String key(Application app) {
		return key(app.getSoftwareName(), app.getSoftwareVersion(), app.getVendorName());
	}

	private String key(ComputerApplication app) {
		return key(app.getSoftwareName(), app.getSoftwareVersion(), app.getVendorName());
	}

	private List<Application> createNewApplications(List<SoftwarePayloadDto> newSoftware) {
		List<Application> applications = newSoftware.stream()
				.map(s -> Application.builder().uuid(UUID.randomUUID().toString()).softwareName(s.getSoftwareName())
						.softwareVersion(s.getSoftwareVersion()).vendorName(s.getVendorName()).build())
				.toList();

		log.debug("Creating {} new applications in the database", applications.size());
		int status = applicationDao.insertApplications(applications);

		if (status == 0) {
			log.error("Failed to insert new applications into the database");
			return Collections.emptyList();
		}

		log.info("Successfully inserted {} new applications", applications.size());
		log.error("Applications to be analyzed: {}", applications);
		vulnerabilityService.analyzeAndSaveApplicationVulnerabilitiesAsync(applications);
		return applications;
	}

	private List<ComputerApplication> mapApplicationsToComputer(String computerUuid, List<Application> applications) {
		return applications.stream()
				.map(app -> ComputerApplication.builder().uuid(UUID.randomUUID().toString()).computerUuid(computerUuid)
						.applicationUuid(app.getUuid()).softwareName(app.getSoftwareName())
						.softwareVersion(app.getSoftwareVersion()).vendorName(app.getVendorName())
						.isDeleted(false).build())
				.toList();
	}

	@Override
	public List<Application> getAllApplications(String isVulnerable) {
		if(isVulnerable != null && !isVulnerable.isEmpty()) {
			return applicationDao.getAllApplications(isVulnerable);
		}
		return applicationDao.getAllApplications();
		
	}

	@Override
	public List<ApplicationResponseDto> getApplicationDetails(String computerUuid) {
		
		Map<String, Map<String, Integer>> vulnerabilityCounts = vulnerabilityDao.getVulnerabilityCountsByApplication();
		
		return applicationDao.getApplicationsByComputerUuid(computerUuid).stream()
				.map(app -> {
					Map<String, Integer> severityCountMap = 
							vulnerabilityCounts.getOrDefault(app.getApplicationUuid(), Collections.emptyMap());
					
					return ApplicationResponseDto.builder()
							.uuid(app.getApplicationUuid())
							.softwareName(app.getSoftwareName())
							.vendor(app.getVendorName())
							.softwareVersion(app.getSoftwareVersion())
							.criticalVulnerabilityCount(severityCountMap.getOrDefault("CRITICAL", 0))
							.highVulnerabilityCount(severityCountMap.getOrDefault("HIGH", 0))
							.mediumVulnerabilityCount(severityCountMap.getOrDefault("MEDIUM", 0))
							.lowVulnerabilityCount(severityCountMap.getOrDefault("LOW", 0))
							.vulnerabilities(vulnerabilityDao.getVulnerabilitiesByApplicationUuid(app.getApplicationUuid()))
							.build();
				}).toList();
		
	}
}
