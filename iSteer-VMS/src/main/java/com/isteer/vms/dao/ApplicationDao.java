package com.isteer.vms.dao;

import java.util.List;
import java.util.Map;

import com.isteer.vms.model.Application;
import com.isteer.vms.model.ComputerApplication;

public interface ApplicationDao {

	List<Application> getAllApplications();

	List<Application> getAllApplications(String isVulnerable);

	List<ComputerApplication> getApplicationsByComputerUuid(String computerUuid);

	int insertApplications(List<Application> applications);

	int insertComputerApplications(List<ComputerApplication> computerApplications);

	int deleteOrActivateComputerApplications(List<ComputerApplication> computerApplications);

	Map<String, Integer> getInstalledVulnerableAppCounts();

	List<Application> getUnresolvedApplications();

	void updateProcessIdsBatch(List<ComputerApplication> compAppsToUpdate);

}
