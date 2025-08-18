package com.isteer.vms.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.isteer.vms.core.engine.job.JobExecuter;
import com.isteer.vms.exception.BusinessException;
import com.isteer.vms.response.ResponseCode;
import com.isteer.vms.response.ResponseUtil;
import com.isteer.vms.service.VulnerabilityService;

import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.log4j.Log4j2;

@RestController
@Log4j2
@RequestMapping("/api")
public class NvdController {
	private JobExecuter jobExecutor;
	private VulnerabilityService vulnerabilityService;

	public NvdController(JobExecuter jobExecutor, VulnerabilityService vulnerabilityService) {
		this.jobExecutor = jobExecutor;
		this.vulnerabilityService = vulnerabilityService;
	}
	
	@Operation(summary = "Trigger NVD CPE dictionary data update job", description = "This endpoint triggers a background job to update the NVD CPE dictionary data. It returns the unique id for the job that can be used to check the status of the job later.")
	@GetMapping("/nvd/executeJob/updateCpeDictionary")
	public ResponseEntity<?> updateCpeDictionary() {
		Map<String, String> status = new HashMap<>();
		try {
			String jobId = jobExecutor.startUpdateCpeDictionaryJob();
			status.put("jobId", jobId);
			return ResponseEntity.ok(status);
		} catch (Exception e) {
			throw new BusinessException(e.getMessage(), 500);
		}
	}
	@Operation(summary = "Check status of the background job.", description = "This endpoint checks the status of a background job using the job ID returned from the job execution endpoint. It returns the current status of the job.")
	@GetMapping("/nvd/jobStatus")
	public ResponseEntity<?> getJobStatus(String jobId) {
		Map<String, String> status = new HashMap<>();
		try {
			String jobStatus = jobExecutor.getJobStatus(jobId);
			status.put("jobId", jobId);
			status.put("status", jobStatus);
			return ResponseEntity.ok(status);
		} catch (Exception e) {
			throw new BusinessException(e.getMessage(), 500);
		}
	}
	
	@Operation(summary = "Get NVD CVE data", description = "This endpoint retrieves the latest NVD CVE data.")
	@GetMapping("/search/vulnerability/{searchType}")
	public ResponseEntity<Object> searchVulnerability(@PathVariable String searchType,
			@RequestParam(required = false) String searchKeyword, @RequestParam(required = false) String searchCveId,
			@RequestParam(required = false) String searchCpeName) {
		log.info("Received request to search vulnerability or CPE name...");
		if (searchType == null || searchType.isBlank()) {
			log.error("Search type is missing or empty");
			return ResponseUtil.message(ResponseCode.SEARCH_TYPE_MISSING, HttpStatus.BAD_REQUEST);
		}
		if (searchType.trim().equalsIgnoreCase("cve")) {
			log.info("Searching for CVE data...");
			return ResponseUtil.data(vulnerabilityService.searchForCve(searchCveId, searchKeyword, searchCpeName));
		}
		else if(searchType.trim().equalsIgnoreCase("cpe")){
			log.info("Searching for CPE name...");
			return ResponseUtil.data(vulnerabilityService.searchForCpe(searchKeyword, searchCpeName));
		}
		log.error("Invalid search type provided: {}", searchType);
		return ResponseUtil.message(ResponseCode.SEARCH_TYPE_MISSING);
	}

}
