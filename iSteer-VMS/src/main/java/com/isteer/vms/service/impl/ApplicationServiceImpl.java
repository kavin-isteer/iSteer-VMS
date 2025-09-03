package com.isteer.vms.service.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.isteer.vms.dao.ApplicationDao;
import com.isteer.vms.dao.VulnerabilityDao;
import com.isteer.vms.dto.ApplicationResponseDto;
import com.isteer.vms.dto.SoftwarePayloadDto;
import com.isteer.vms.exception.BusinessException;
import com.isteer.vms.model.Application;
import com.isteer.vms.model.ApplicationCpeName;
import com.isteer.vms.model.ComputerApplication;
import com.isteer.vms.response.ResponseCode;
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
	public int createOrUpdateApplication(String computerUuid, List<SoftwarePayloadDto> software) {
		log.info("Processing applications for computer UUID: {}", computerUuid);

		List<Application> existingApps = applicationDao.getAllApplications();
		log.debug("Found {} existing applications in the database", existingApps.size());
		List<ComputerApplication> existingCompApps = applicationDao.getApplicationsByComputerUuid(computerUuid);
		log.debug("Found {} existing computer application mappings for computer UUID: {}", existingCompApps.size(), computerUuid);

		Set<String> appKeys = toKeySet(existingApps);
		Set<String> compAppKeys = toKeySetFromCompApps(existingCompApps);

		List<SoftwarePayloadDto> newSoftware = findNewSoftware(software, appKeys);
		log.debug("Found {} new software entries to process", newSoftware.size());
		if (!newSoftware.isEmpty()) {
			List<Application> inserted = createNewApplications(newSoftware);
			if (inserted == null || inserted.isEmpty())
				return -1;
			existingApps = applicationDao.getAllApplications(); // refresh
			appKeys.addAll(toKeySet(inserted));
		}
		log.debug("Total applications after insertion: {}", existingApps.size());

		int deleted = handleDeletedApplications(software, existingCompApps);
		int activated = handleActivatedApplications(software, existingCompApps);
		int mapped = handleNewMappings(computerUuid, software, existingApps, appKeys, compAppKeys);

		updateProcessIdsForComputerApplications(computerUuid, software, existingApps);
		return determineFinalStatus(mapped, deleted, activated);
	}

	private void updateProcessIdsForComputerApplications(String computerUuid, List<SoftwarePayloadDto> software,
			List<Application> existingApps) {
		
		Map<String, String> keyToAppUuid = existingApps.stream()
		        .collect(Collectors.toMap(app -> key(app), Application::getUuid));
		
		 List<ComputerApplication> compAppsToUpdate = new ArrayList<>();

		    for (SoftwarePayloadDto s : software) {
		        String appUuid = keyToAppUuid.get(key(s));
		        if (appUuid != null) {
		            ComputerApplication compApp = ComputerApplication.builder()
		            		.computerUuid(computerUuid)
		            		.applicationUuid(appUuid)
		            		.processIds(s.getRunningProcessIds())
		            		.build();
		            compAppsToUpdate.add(compApp);
		        }
		    }
		    
		    if (!compAppsToUpdate.isEmpty()) {
		        applicationDao.updateProcessIdsBatch(compAppsToUpdate);
		    }
		    
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
	            ca.setProcessIds(null);
	            return ca;
	        })
	        .toList();

	    if (toDelete.isEmpty()) {
	    	log.debug("No stale computer application mappings to delete");
	    	return 0;
	    }

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

	    if (toActivate.isEmpty()) {
	    	log.debug("No computer application mappings to rectivate");
	    	return 0;
	    }

	    log.info("Reactivating {} application mappings", toActivate.size());
	    return applicationDao.deleteOrActivateComputerApplications(toActivate);
	}


	private int handleNewMappings(String computerUuid, List<SoftwarePayloadDto> software,
			List<Application> existingApps, Set<String> appKeys, Set<String> compAppKeys) {
		List<String> newKeys = software.stream().map(this::key)
				.filter(k -> !compAppKeys.contains(k) && appKeys.contains(k)).toList();
		log.info("Found {} new mappings for computer UUID: {}", newKeys.size(), computerUuid);

		if (newKeys.isEmpty()) {
			log.debug("No new mappings to process for computer UUID: {}", computerUuid);
			return 0;
		}

		List<Application> newMappings = existingApps.stream().filter(app -> newKeys.contains(key(app)))
				.toList();

		List<ComputerApplication> computerApplications = mapApplicationsToComputer(computerUuid, newMappings);
		mergeInstalledDates(computerApplications, software);
		log.debug("Mapping {} new applications to computer UUID: {}", computerApplications.size(), computerUuid);
		return applicationDao.insertComputerApplications(computerApplications);
	}

	private void mergeInstalledDates(List<ComputerApplication> mappedApps, List<SoftwarePayloadDto> software) {
		mappedApps.forEach(app -> software.stream().filter(s -> key(s).equals(key(app))).findFirst()
				.ifPresent(s -> { app.setInstalledDate(s.getInstalledDate());
				app.setProcessIds(s.getRunningProcessIds());
				}));
	}

	private int determineFinalStatus(int mapped, int deleted, int activated) {
		log.debug("Final status - mapped: {}, deleted: {}, activated: {}", mapped, deleted, activated);
		if (mapped == 0 && deleted == 0 && activated == 0)
			return 0;
		if (mapped == 1 && deleted == 0 && activated == 0)
			return 1;
		if (mapped == 1 || deleted == 1 || activated == 1)
			return 2;
		return -1;
	}

	private String key(String name, String version, String vendor) {
		String safeName = name != null ? name.trim().toLowerCase() : "";
		String safeVersion = version != null ? version.trim().toLowerCase() : "";
		String safeVendor = vendor != null ? vendor.trim().toLowerCase() : "";
		return safeName + "|" + safeVersion + "|" + safeVendor;
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
			throw new BusinessException(ResponseCode.APPLICATION_ERROR.getMessage(), 500);
		}

		log.info("Successfully inserted {} new applications", applications.size());
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
		log.debug("Fetching application details for computer UUID: {}", computerUuid);
		Map<String, Map<String, Integer>> vulnerabilityCounts = vulnerabilityDao.getVulnerabilityCountsByApplication();
		Map<String, ApplicationCpeName> cpeNames = vulnerabilityDao.getCpeNamesByComputerUuid(computerUuid);
		
		return applicationDao.getApplicationsByComputerUuid(computerUuid).stream()
				.map(app -> {
					Map<String, Integer> severityCountMap = 
							vulnerabilityCounts.getOrDefault(app.getApplicationUuid(), Collections.emptyMap());
					ApplicationCpeName cpeName = cpeNames.get(app.getApplicationUuid());
					
					return ApplicationResponseDto.builder()
							.uuid(app.getApplicationUuid())
							.softwareName(app.getSoftwareName())
							.vendor(app.getVendorName())
							.softwareVersion(app.getSoftwareVersion())
							.cpeName(cpeName != null ? cpeName.getCpeName() : null)
							.isResolved(cpeName != null ? cpeName.isResolvedCpe() : false)
							.criticalVulnerabilityCount(severityCountMap.getOrDefault("CRITICAL", 0))
							.highVulnerabilityCount(severityCountMap.getOrDefault("HIGH", 0))
							.mediumVulnerabilityCount(severityCountMap.getOrDefault("MEDIUM", 0))
							.lowVulnerabilityCount(severityCountMap.getOrDefault("LOW", 0))
							.vulnerabilities(vulnerabilityDao.getVulnerabilitiesByApplicationUuid(app.getApplicationUuid()))
							.build();
				}).toList();
		
	}

	@Override
	public List<Application> getUnresolvedApplications() {
		log.debug("Fetching unresolved applications");
		return applicationDao.getUnresolvedApplications();
	}
}
