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

/**
 * Service implementation for managing application-related business logic.
 * This class orchestrates the process of synchronizing software data from computers,
 * creating new application records, managing computer-application mappings, and retrieving detailed application information.
 */
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

		// Fetch all existing applications and the specific computer's application mappings from the DB.
		List<Application> existingApps = applicationDao.getAllApplications();
		log.debug("Found {} existing applications in the database", existingApps.size());
		List<ComputerApplication> existingComputerApps = applicationDao.getApplicationsByComputerUuid(computerUuid);
		log.debug("Found {} existing computer application mappings for computer UUID: {}", existingComputerApps.size(), computerUuid);

		// Create sets of unique keys for efficient lookups.
		Set<String> existingAppKeys = toKeySet(existingApps);
		Set<String> existingComputerAppKeys = toKeySetFromCompApps(existingComputerApps);

		// Identify software from the payload that doesn't exist in the main applications table.
		List<SoftwarePayloadDto> newApplications = findNewSoftware(software, existingAppKeys);
		log.debug("Found {} new software entries to process", newApplications.size());
		if (!newApplications.isEmpty()) {
			// Create records for the new software.
			List<Application> insertedApps = createNewApplications(newApplications);
			if (insertedApps == null || insertedApps.isEmpty())
				return -1; // Return error code if insertion fails.
			// Refresh the list and keyset with the newly inserted applications.
			existingApps = applicationDao.getAllApplications(); 
			existingAppKeys.addAll(toKeySet(insertedApps));
		}
		log.debug("Total applications after insertion: {}", existingApps.size());

		// Handle the various states: uninstalled, reinstalled, and newly installed.
		int deleted = handleDeletedApplications(software, existingComputerApps);
		int reinstalled = handleActivatedApplications(software, existingComputerApps);
		int mapped = handleNewMappings(computerUuid, software, existingApps, existingAppKeys, existingComputerAppKeys);

		// Update running process IDs for all applications reported by the computer.
		updateProcessIdsForComputerApplications(computerUuid, software, existingApps);
		
		// Determine the final summary status code.
		return determineFinalStatus(mapped, deleted, reinstalled);
	}

	/**
	 * A private helper method to update the running process IDs for applications on a specific computer.
	 *
	 * @param computerUuid   The UUID of the computer.
	 * @param software       The list of software from the payload.
	 * @param existingApps   A list of all known applications in the database.
	 */
	private void updateProcessIdsForComputerApplications(String computerUuid, List<SoftwarePayloadDto> software,
			List<Application> existingApps) {
		
		// Create a map for quick lookup of an application's UUID from its key.
		Map<String, String> keyToAppUuid = existingApps.stream()
		        .collect(Collectors.toMap(this::key, Application::getUuid));
		
		List<ComputerApplication> computerAppsToUpdate = new ArrayList<>();

		// For each software in the payload, find its application UUID and create an update object.
	    for (SoftwarePayloadDto s : software) {
	        String appUuid = keyToAppUuid.get(key(s));
	        if (appUuid != null) {
	            ComputerApplication compApp = ComputerApplication.builder()
	            		.computerUuid(computerUuid)
	            		.applicationUuid(appUuid)
	            		.processIds(s.getRunningProcessIds())
	            		.build();
	            computerAppsToUpdate.add(compApp);
	        }
	    }
	    
	    // If there are applications to update, call the DAO to perform a batch update.
	    if (!computerAppsToUpdate.isEmpty()) {
	        applicationDao.updateProcessIdsBatch(computerAppsToUpdate);
	    }
	}

	/**
	 * Creates a set of unique string keys from a list of Application objects.
	 *
	 * @param apps The list of applications.
	 * @return A set of unique string keys.
	 */
	private Set<String> toKeySet(List<Application> apps) {
		return apps.stream().map(this::key).collect(Collectors.toSet());
	}

	/**
	 * Creates a set of unique string keys from a list of ComputerApplication objects.
	 *
	 * @param compApps The list of computer-application mappings.
	 * @return A set of unique string keys.
	 */
	private Set<String> toKeySetFromCompApps(List<ComputerApplication> compApps) {
		return compApps.stream().map(this::key).collect(Collectors.toSet());
	}

	/**
	 * Filters the incoming software list to find entries that are not yet in the database.
	 *
	 * @param software      The full list of software from the computer.
	 * @param existingAppKeys  A set of keys for all applications already in the database.
	 * @return A list of {@link SoftwarePayloadDto} for new software.
	 */
	private List<SoftwarePayloadDto> findNewSoftware(List<SoftwarePayloadDto> software, Set<String> existingAppKeys) {
		return software.stream().filter(s -> !existingAppKeys.contains(key(s))).toList();
	}

	/**
	 * Identifies and marks computer-application mappings as deleted if the software is no longer reported by the computer.
	 *
	 * @param software         The current list of software on the computer.
	 * @param existingComputerApps The list of existing mappings for that computer.
	 * @return The result of the database update operation (1 for success, 0 for no change/failure).
	 */
	private int handleDeletedApplications(List<SoftwarePayloadDto> software, List<ComputerApplication> existingComputerApps) {
	    // Find active mappings that are NOT in the latest software payload.
	    List<ComputerApplication> toDelete = existingComputerApps.stream()
	        .filter(ca -> !ca.isDeleted() && software.stream().noneMatch(s -> key(s).equals(key(ca))))
	        .map(ca -> {
	            // Set the flag to deleted and clear process IDs.
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

	/**
	 * Identifies and reactivates mappings for software that was previously marked as deleted but is now reported again.
	 *
	 * @param software         The current list of software on the computer.
	 * @param existingComputerApps The list of existing mappings for that computer.
	 * @return The result of the database update operation (1 for success, 0 for no change/failure).
	 */
	private int handleActivatedApplications(List<SoftwarePayloadDto> software, List<ComputerApplication> existingComputerApps) {
	    // Find deleted mappings that ARE present in the latest software payload.
	    List<ComputerApplication> toActivate = existingComputerApps.stream()
	        .filter(ComputerApplication::isDeleted) // Filter for already deleted ones
	        .filter(ca -> software.stream().anyMatch(s -> key(s).equals(key(ca)))) // Check if they are back
	        .map(ca -> {
	            ca.setDeleted(false); // Reactivate them
	            return ca;
	        })
	        .toList();

	    if (toActivate.isEmpty()) {
	    	log.debug("No computer application mappings to reactivate");
	    	return 0;
	    }

	    log.info("Reactivating {} application mappings", toActivate.size());
	    return applicationDao.deleteOrActivateComputerApplications(toActivate);
	}

	/**
	 * Identifies and creates new mappings for software that exists in the applications table but is not yet associated with the computer.
	 *
	 * @param computerUuid    The UUID of the computer.
	 * @param software        The current software list.
	 * @param existingApps    All applications from the database.
	 * @param appKeys         Keyset of all applications.
	 * @param compAppKeys     Keyset of existing mappings for the computer.
	 * @return The result of the database insert operation.
	 */
	private int handleNewMappings(String computerUuid, List<SoftwarePayloadDto> software,
			List<Application> existingApps, Set<String> existingAppKeys, Set<String> existingComputerAppKeys) {
		// Find keys from the payload that exist in the main app table but not in this computer's mappings.
		List<String> newKeys = software.stream().map(this::key)
				.filter(k -> !existingComputerAppKeys.contains(k) && existingAppKeys.contains(k)).toList();
		log.info("Found {} new mappings for computer UUID: {}", newKeys.size(), computerUuid);

		if (newKeys.isEmpty()) {
			log.debug("No new mappings to process for computer UUID: {}", computerUuid);
			return 0;
		}

		// Get the full Application objects for the new mappings.
		List<Application> newMappings = existingApps.stream().filter(app -> newKeys.contains(key(app)))
				.toList();

		// Create ComputerApplication objects for the new mappings.
		List<ComputerApplication> computerApplications = mapApplicationsToComputer(computerUuid, newMappings);
		// Add installed dates and process IDs from the payload.
		mergeInstalledDates(computerApplications, software);
		log.debug("Mapping {} new applications to computer UUID: {}", computerApplications.size(), computerUuid);
		return applicationDao.insertComputerApplications(computerApplications);
	}

	/**
	 * Merges installation dates and process IDs from the payload into the newly created ComputerApplication objects.
	 *
	 * @param mappedApps  The list of new ComputerApplication mappings.
	 * @param software    The original software payload.
	 */
	private void mergeInstalledDates(List<ComputerApplication> mappedApps, List<SoftwarePayloadDto> software) {
		mappedApps.forEach(app -> software.stream().filter(s -> key(s).equals(key(app))).findFirst()
				.ifPresent(s -> {
					app.setInstalledDate(s.getInstalledDate());
					app.setProcessIds(s.getRunningProcessIds());
				}));
	}

	/**
	 * Determines a final summary status code based on the results of the sub-operations.
	 *
	 * @param mapped    Status from the mapping operation.
	 * @param deleted   Status from the deletion operation.
	 * @param activated Status from the activation operation.
	 * @return A final integer status code.
	 */
	private int determineFinalStatus(int mapped, int deleted, int activated) {
		log.debug("Final status - mapped: {}, deleted: {}, activated: {}", mapped, deleted, activated);
		if (mapped == 0 && deleted == 0 && activated == 0)
			return 0; // No changes
		if (mapped == 1 && deleted == 0 && activated == 0)
			return 1; // Only new mappings
		if (mapped >= 1 || deleted >= 1 || activated >= 1)
			return 2; // A mix of operations
		return -1; // Error case
	}
	
	/**
	 * Generates a consistent, unique key from application details. The key is case-insensitive and trimmed.
	 *
	 * @param name    The software name.
	 * @param version The software version.
	 * @param vendor  The software vendor.
	 * @return A concatenated key string (e.g., "firefox|105.0|mozilla").
	 */
	private String key(String name, String version, String vendor) {
		String safeName = name != null ? name.trim().toLowerCase() : "";
		String safeVersion = version != null ? version.trim().toLowerCase() : "";
		String safeVendor = vendor != null ? vendor.trim().toLowerCase() : "";
		return safeName + "|" + safeVersion + "|" + safeVendor;
	}

	// Overloaded key methods for convenience with different object types.
	private String key(SoftwarePayloadDto dto) {
		return key(dto.getSoftwareName(), dto.getSoftwareVersion(), dto.getVendorName());
		}
	
	private String key(Application app) {
		return key(app.getSoftwareName(), app.getSoftwareVersion(), app.getVendorName());
		}
	
	private String key(ComputerApplication app) {
		return key(app.getSoftwareName(), app.getSoftwareVersion(), app.getVendorName());
		}
	

	/**
	 * Creates new Application entities and persists them to the database. Also triggers an asynchronous vulnerability analysis.
	 *
	 * @param newSoftware The list of new software to create.
	 * @return A list of the newly created and persisted {@link Application} objects.
	 * @throws BusinessException if the database insertion fails.
	 */
	private List<Application> createNewApplications(List<SoftwarePayloadDto> newSoftware) {
		// Map DTOs to Application model objects with new UUIDs.
		List<Application> applications = newSoftware.stream()
				.map(s -> Application.builder().uuid(UUID.randomUUID().toString()).softwareName(s.getSoftwareName())
						.softwareVersion(s.getSoftwareVersion()).vendorName(s.getVendorName()).build())
				.toList();

		log.debug("Creating {} new applications in the database", applications.size());
		int insertStatus = applicationDao.insertApplications(applications);

		if (insertStatus == 0) {
			log.error("Failed to insert new applications into the database");
			throw new BusinessException(ResponseCode.APPLICATION_ERROR.getMessage(), 500);
		}

		log.info("Successfully inserted {} new applications", applications.size());
		// Trigger vulnerability analysis for the new applications without blocking the current thread.
		vulnerabilityService.analyzeAndSaveApplicationVulnerabilitiesAsync(applications);
		return applications;
	}

	/**
	 * Maps a list of Application objects to a list of ComputerApplication objects for a specific computer.
	 *
	 * @param computerUuid The UUID of the computer.
	 * @param applications The list of applications to map.
	 * @return A list of new {@link ComputerApplication} mapping objects.
	 */
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
		// If the filter parameter is provided, use the filtered DAO method.
		if (isVulnerable != null && !isVulnerable.isEmpty()) {
			return applicationDao.getAllApplications(isVulnerable);
		}
		// Otherwise, get all applications.
		return applicationDao.getAllApplications();
	}

	
	@Override
	public List<ApplicationResponseDto> getApplicationDetails(String computerUuid) {
		log.debug("Fetching application details for computer UUID: {}", computerUuid);
		// Pre-fetch vulnerability counts and CPE names to avoid N+1 query problems.
		Map<String, Map<String, Integer>> vulnerabilityCounts = vulnerabilityDao.getVulnerabilityCountsByApplication();
		Map<String, ApplicationCpeName> cpeNames = vulnerabilityDao.getCpeNamesByComputerUuid(computerUuid);
		
		// Get the list of applications for the computer and map them to the response DTO.
		return applicationDao.getApplicationsByComputerUuid(computerUuid).stream()
				.map(app -> {
					// Safely get the vulnerability and CPE data for the current application.
					Map<String, Integer> severityCountMap =
							vulnerabilityCounts.getOrDefault(app.getApplicationUuid(), Collections.emptyMap());
					ApplicationCpeName cpeName = cpeNames.get(app.getApplicationUuid());
					
					// Build the detailed response DTO.
					return ApplicationResponseDto.builder()
							.uuid(app.getApplicationUuid())
							.softwareName(app.getSoftwareName())
							.vendor(app.getVendorName())
							.softwareVersion(app.getSoftwareVersion())
							.runningProcessIds(app.getProcessIds())
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