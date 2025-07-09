package com.isteer.vms.core.engine;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;

import com.isteer.vms.core.engine.dao.DependencyHintDao;
import com.isteer.vms.core.engine.model.BaseApplication;
import com.isteer.vms.model.Application;

public class Engine {
	
	@Autowired
	DependencyHintDao dependencyHintDao;

	public Map<String, BaseApplication> softwareNormalizer(List<Application> applications){
		Map<String, BaseApplication> insertedApplications = new HashMap<>();
		
		for(Application application : applications) {
			if(!application.getUuid().isEmpty() && application.getUuid() != null) {
				BaseApplication resolvedApplication = resolveSoftwareNames(application);
			}
		}
		return null;
	}

	private BaseApplication resolveSoftwareNames(Application application) {
		
		return null;
	}
}
