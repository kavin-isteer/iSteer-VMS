package com.isteer.vms.service.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.isteer.vms.dao.ApplicationDao;
import com.isteer.vms.dto.SoftwarePayloadDto;
import com.isteer.vms.model.Application;
import com.isteer.vms.model.ComputerApplication;
import com.isteer.vms.service.ApplicationService;

@Service
public class ApplicationServiceImpl implements ApplicationService {

	private static final Logger logger = LogManager.getLogger(ApplicationServiceImpl.class);

	@Autowired
	private ApplicationDao applicationDao;

	@Override
	@Transactional
	public int createOrUpdateApplication(String computerUuid, List<SoftwarePayloadDto> software) {
		logger.info("Processing applications for computer UUID: {}", computerUuid);
		List<Application> existingApplications = applicationDao.getAllApplications();
		logger.debug("Found {} existing applications in the database", existingApplications.size());
		List<ComputerApplication> existingComputerApplications = applicationDao
				.getApplicationsByComputerUuid(computerUuid);
		logger.debug("Found {} existing computer applications mapped to computer UUID: {}",
				existingComputerApplications.size(), computerUuid);

		int mappingStatus = 0;
		int deletedStatus = 0;
		int activatedStatus = 0;

		Set<String> existingApplicationKeys = existingApplications.stream()
				.map(app -> key(app.getSoftwareName(), app.getSoftwareVersion(), app.getVendorName()))
				.collect(Collectors.toSet());

		Set<String> existingComputerApplicationKeys = existingComputerApplications.stream()
				.map(app -> key(app.getSoftwareName(), app.getSoftwareVersion(), app.getVendorName()))
				.collect(Collectors.toSet());

		List<SoftwarePayloadDto> newSoftware = software.stream()
				.filter(s -> !existingApplicationKeys
						.contains(key(s.getSoftwareName(), s.getSoftwareVersion(), s.getVendorName())))
				.collect(Collectors.toList());
		logger.info("Identified {} new software applications to be added", newSoftware.size());

		if (!newSoftware.isEmpty()) {
			logger.debug("Creating new applications for the software payloads");
			List<Application> insertedApplications = createNewApplications(newSoftware);
			if (insertedApplications.isEmpty() || insertedApplications == null) {
				return -1; // Indicate failure to insert new applications
			}
			existingApplications = applicationDao.getAllApplications();
			logger.debug("Fetched {} applications after insertion", existingApplications.size());
			existingApplicationKeys.addAll(insertedApplications.stream()
					.map(app -> key(app.getSoftwareName(), app.getSoftwareVersion(), app.getVendorName()))
					.collect(Collectors.toSet()));
		}

		List<ComputerApplication> computerApplicationsToDelete = existingComputerApplications.stream()
				.filter(ca -> !ca.isDeleted() && software.stream()
						.noneMatch(s -> key(s.getSoftwareName(), s.getSoftwareVersion(), s.getVendorName())
								.equals(key(ca.getSoftwareName(), ca.getSoftwareVersion(), ca.getVendorName()))))
				.collect(Collectors.toList());
		logger.info("Identified {} computer applications mapping to be deleted", computerApplicationsToDelete.size());

		if (!computerApplicationsToDelete.isEmpty()) {
			computerApplicationsToDelete.forEach(ca -> {
				ca.setDeleted(true);
				ca.setUpdatedAt(LocalDateTime.now());
			});
			logger.debug("Deleting computer applications that are no longer present in the software payloads");
			deletedStatus = applicationDao.deleteOrActivateComputerApplications(computerApplicationsToDelete);
			
		}

		List<ComputerApplication> computerApplicationsToActivate = existingComputerApplications.stream()
				.filter(ca -> ca.isDeleted() && software.stream()
						.anyMatch(s -> key(s.getSoftwareName(), s.getSoftwareVersion(), s.getVendorName())
								.equals(key(ca.getSoftwareName(), ca.getSoftwareVersion(), ca.getVendorName()))))
				.collect(Collectors.toList());
		logger.info("Identified {} computer applications mapping to be activated", computerApplicationsToActivate.size());

		if (!computerApplicationsToActivate.isEmpty()) {
			computerApplicationsToActivate.forEach(ca -> {
				ca.setDeleted(false);
				ca.setUpdatedAt(LocalDateTime.now());
			});
			logger.debug("Activating computer applications that were previously deleted");
			activatedStatus = applicationDao.deleteOrActivateComputerApplications(computerApplicationsToActivate);
		}
		List<String> newApplicationKeys = software.stream()
				.map(s -> key(s.getSoftwareName(), s.getSoftwareVersion(), s.getVendorName()))
				.filter(appKey -> !existingComputerApplicationKeys.contains(appKey)
						&& existingApplicationKeys.contains(appKey))
				.collect(Collectors.toList());
		logger.info("Identified {} new computer applications to be mapped", newApplicationKeys.size());

		List<Application> newComputerApplications = existingApplications.stream()
				.filter(app -> newApplicationKeys
						.contains(key(app.getSoftwareName(), app.getSoftwareVersion(), app.getVendorName())))
				.collect(Collectors.toList());
		logger.debug("Mapping {} new applications to computer UUID: {}", newComputerApplications.size(), computerUuid);

		if (!newComputerApplications.isEmpty()) {
			List<ComputerApplication> computerApplications = mapApplicationsToComputer(computerUuid,
					newComputerApplications);
			computerApplications.forEach(a -> {
				String aKey = key(a.getSoftwareName(), a.getSoftwareVersion(), a.getVendorName());
				software.stream()
						.filter(s -> key(s.getSoftwareName(), s.getSoftwareVersion(), s.getVendorName()).equals(aKey))
						.findFirst().ifPresent(s -> {
							a.setInstalledDate(s.getInstalledDate());
						});
			});
			logger.debug("Inserting computer applications mappings for computer UUID: {}", computerUuid);
			mappingStatus = applicationDao.insertComputerApplications(computerApplications);
		}

		if (mappingStatus == 0 && deletedStatus == 0 && activatedStatus == 0) {
			return 0; // Indicate failure to create or update applications
		}
		if (mappingStatus == 1 && deletedStatus == 0 && activatedStatus == 0) {
			return 1; // Indicate successful creation of new applications
		}
		if (mappingStatus == 1 || deletedStatus == 1 || activatedStatus == 1) {
			return 2; // Indicate successful update of existing applications
		}
		return -1;

	}

	private List<Application> createNewApplications(List<SoftwarePayloadDto> newSoftware) {
		List<Application> applications = new ArrayList<>();
		for (SoftwarePayloadDto software : newSoftware) {
			Application application = new Application();
			application.setUuid(UUID.randomUUID().toString());
			application.setSoftwareName(software.getSoftwareName());
			application.setSoftwareVersion(software.getSoftwareVersion());
			application.setVendorName(software.getVendorName());
			application.setCreatedAt(LocalDateTime.now());
			applications.add(application);
		}
		logger.debug("Creating {} new applications in the database", applications.size());
		int status = applicationDao.insertApplications(applications);
		if (status == 0) {
//			throw new RuntimeException("Failed to insert new applications");
			logger.error("Failed to insert new applications into the database");
			return null;
		}
		logger.info("Successfully inserted {} new applications into the database", applications.size());
		return applications;

	}

	private List<ComputerApplication> mapApplicationsToComputer(String computerUuid, List<Application> applications) {
		LocalDateTime now = LocalDateTime.now();

		return applications.stream().map(app -> {
			ComputerApplication ca = new ComputerApplication();
			ca.setUuid(UUID.randomUUID().toString());
			ca.setComputerUuid(computerUuid);
			ca.setApplicationUuid(app.getUuid());
			ca.setSoftwareName(app.getSoftwareName());
			ca.setSoftwareVersion(app.getSoftwareVersion());
			ca.setVendorName(app.getVendorName());
			ca.setCreatedAt(now);
			ca.setDeleted(false);
			return ca;
		}).collect(Collectors.toList());
	}

	private String key(String softwareName, String softwareVersion, String vendorName) {
		return softwareName + "|" + softwareVersion + "|" + vendorName;
	}

}
