package com.isteer.vms.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.isteer.vms.model.Application;
import com.isteer.vms.response.ResponseCode;
import com.isteer.vms.response.ResponseUtil;
import com.isteer.vms.service.ApplicationService;

import lombok.extern.log4j.Log4j2;

@RestController
@Log4j2
@RequestMapping("/api")
public class ApplicationController {
	
	private ApplicationService applicationService;

	public ApplicationController(ApplicationService applicationService) {
		super();
		this.applicationService = applicationService;
	}
	
	@GetMapping("/applications")
	public ResponseEntity<List<Application>> getAllApplications(@RequestParam(required = false)String isVulnerable) {
		log.info("Recived request to get all applicaitions");
		List<Application> applications = applicationService.getAllApplications(isVulnerable);
		if(applications.isEmpty()) {
			log.info("No applicaitons found");
			return ResponseEntity.noContent().build();
		}
		log.info("Returning {} applicaitons", applications.size());
		return ResponseEntity.ok(applications);
	}
	
	@GetMapping("/applications/unresolved")
	public ResponseEntity<Object> getUnresolvedApplications() {
		log.info("Recieved request to get unresolved applications");
		List<Application> applications = applicationService.getUnresolvedApplications();
		if(applications.isEmpty()) {
			log.info("No unresolved applications found");
			return ResponseUtil.message(ResponseCode.NO_DATA_FOUND);
		}
		log.info("Returning {} unresolved applications", applications.size());
		return ResponseUtil.data(applications);
	}

}
