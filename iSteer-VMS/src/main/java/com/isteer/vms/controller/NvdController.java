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
import com.isteer.vms.core.engine.model.CpeName;
import com.isteer.vms.exception.BusinessException;
import com.isteer.vms.model.Vulnerability;
import com.isteer.vms.response.BaseResponse;
import com.isteer.vms.response.ResponseCode;
import com.isteer.vms.response.ResponseUtil;
import com.isteer.vms.service.VulnerabilityService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
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
	
	@Operation(summary = "Execute background job to update NVD CPE data", description = "This endpoint triggers a background job to update the NVD CPE data. It returns a job ID that can be used to check the status of the job later. "
			+ "The job will run asynchronously, and the response will not wait for the job to complete. "
			+ "The job will update the NVD CPE data in the database, which can be used for searching resolved product and vendor names.", responses = {
					@ApiResponse(responseCode = "200", description = "Job started successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Map.class))),
					@ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(mediaType = "application/json", schema = @Schema(implementation = BaseResponse.class)))
			})
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
	@Operation(summary = "Check status of the background job.", description = "This endpoint checks the status of a background job using the job ID returned from the job execution endpoint. It returns the current status of the job. "
			+ "If the job is still running, it will return 'RUNNIND'. If the job has completed, it will return 'COMPLETED'.", responses = {
					@ApiResponse(responseCode = "200", description = "Job status retrieved successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Map.class))),
					@ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(mediaType = "application/json", schema = @Schema(implementation = BaseResponse.class)))
			})
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
	
	@Operation(summary = "Search for vulnerabilities or CPE names", description = "This endpoint allows searching for vulnerabilities by CVE ID, CPE name, or keywords. "
			+ "And it can also search for CPE names by using keywords, or likely CPE names. "
			+ "The search type can be either 'cve' for CVE data or 'cpe' for CPE names. "
			+ "If the search type is not provided or is invalid, it will return an error message.", responses = {
				@ApiResponse(responseCode = "200", description = "CVE Search results retrieved successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Vulnerability.class))),
				@ApiResponse(responseCode = "200", description = "CPE Search results retrieved successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = CpeName.class))),
				@ApiResponse(responseCode = "400", description = "Bad request - search type is missing or invalid", content = @Content(mediaType = "application/json", schema = @Schema(implementation = BaseResponse.class))),
				@ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(mediaType ="application/json", schema = @Schema(implementation = BaseResponse.class))),
				
			})
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
		return ResponseUtil.message(ResponseCode.SEARCH_TYPE_MISSING, HttpStatus.BAD_REQUEST);
	}

}
