package com.isteer.vms.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.isteer.vms.core.engine.fuzzysearch.FuzzySearchTool;
import com.isteer.vms.core.engine.model.CpeName;
import com.isteer.vms.dto.ComputerPayloadDto;
import com.isteer.vms.dto.ComputerResponseDto;
import com.isteer.vms.dto.DashboardMetricsDto;
import com.isteer.vms.model.Application;
import com.isteer.vms.model.Computer;
import com.isteer.vms.response.ResponseCode;
import com.isteer.vms.response.ResponseUtil;
import com.isteer.vms.service.ComputerService;
import com.isteer.vms.service.EmailService;
import com.isteer.vms.service.VulnerabilityService;

import jakarta.validation.Valid;
import lombok.extern.log4j.Log4j2;

@RestController
@Log4j2
@RequestMapping("/api")
public class ComputerController {
	
	private ComputerService computerService;
	private VulnerabilityService vulnerabilityService;
	private FuzzySearchTool fuzzySearchTool;
	private EmailService emailService;

	public ComputerController(ComputerService computerService, VulnerabilityService vulnerabilityService,
			FuzzySearchTool fuzzySearchTool, EmailService emailService) {
		super();
		this.computerService = computerService;
		this.vulnerabilityService = vulnerabilityService;
		this.fuzzySearchTool = fuzzySearchTool;
		this.emailService = emailService;
	}

	@PostMapping("/computers")
	public ResponseEntity<Object> createComputer(@Valid @RequestBody ComputerPayloadDto computer){
		log.info("Received request to create or update computer with deviceId: {}", computer.getDeviceId());
		int status = computerService.createOrUpdateComputer(computer);
		switch(status) {
			case 0:
				return ResponseUtil.message(ResponseCode.NO_CHANGES_MADE);
			case 1:
				return ResponseUtil.message(ResponseCode.COMPUTER_UPDATED);
			case 2:
				return ResponseUtil.message(ResponseCode.COMPUTER_AND_APPLICATION_UPDATED);
			case 3:
				return ResponseUtil.message(ResponseCode.APPLICATION_UPDATED);
			case 4:
				return ResponseUtil.message(ResponseCode.NEW_COMPUTER_CREATED);
			case -1:
				return ResponseUtil.message(ResponseCode.COMPUTER_DELETED, HttpStatus.BAD_REQUEST);
			case -2:
				return ResponseUtil.message(ResponseCode.COMPUTER_INACTIVE, HttpStatus.BAD_REQUEST);
			case -3:
				return ResponseUtil.message(ResponseCode.APPLICATION_ERROR);
			case -4:
				return ResponseUtil.message(ResponseCode.INTERNAL_ERROR);
			default:
				return ResponseUtil.message(ResponseCode.DEFAULT_ERROR);
		}
	}
	
	@GetMapping("/computer")
	public ResponseEntity<Object> getAllComputers(@RequestParam(required = false) String status){
		log.info("Received request to fetch all computers.");
		List<Computer> computers = computerService.getAllComnputers(status);
		if(computers.isEmpty()) {
			log.info("No computers Found.");
			return ResponseUtil.message(ResponseCode.NO_DATA_FOUND);
		}
		log.info("Returning {} computers.", computers.size());
		return ResponseUtil.data(computers);
	}
	
	@GetMapping("/getDashboardMetrics")
	public ResponseEntity<Object> getMetrics() {
		log.info("Received request to fetch dashboard metrics.");
		DashboardMetricsDto metrics = computerService.getDashboardMetrics();
		if(metrics == null) {
			log.info("No metrics found.");
			return ResponseUtil.message(ResponseCode.NO_DATA_FOUND);
		} else {
			log.info("Returning dashboard metrics");
			return ResponseUtil.data(metrics);
		}
	}
	
	@GetMapping("hint/likelyCpeNames")
	public ResponseEntity<Object> getLikelyCpeNames(@RequestParam String vendor, @RequestParam String product,
			@RequestParam(required = false) String version) {
		List<CpeName> likelyCpeNames;
		if (vendor == null || product == null || vendor.trim().isEmpty() || product.trim().isEmpty()) {
			return ResponseUtil.message(ResponseCode.MISSING_PARAMETER_FOR_LIKELY_CPE_NAME_SEARCH, HttpStatus.BAD_REQUEST);
		}
		likelyCpeNames = fuzzySearchTool.searchForLikelyCpeName(vendor, product, version);
		if(likelyCpeNames.isEmpty()) {
			return ResponseUtil.message(ResponseCode.NO_DATA_FOUND);
		}
		return ResponseUtil.data(likelyCpeNames);
	}
	
	@PostMapping("/hint/addHint")
	public ResponseEntity<Object> addApplicationHint(@RequestParam String cpeName,
			@RequestBody Application application) {
		int status = vulnerabilityService.addApplicationHint(cpeName, application);
		ResponseCode code;
		HttpStatus statusCode;
		switch (status) {
		case 1: {
			code = ResponseCode.HINT_ADDED_SUCCESSFULLY;
			statusCode = HttpStatus.OK;
			vulnerabilityService.analyzeAndSaveApplicationVulnerabilitiesAsync(List.of(application));
			break;
		}
		case -1: {
			code = ResponseCode.CPE_NAME_NOT_VALID;
			statusCode = HttpStatus.BAD_REQUEST;
			break;
		}
		case -2: {
			code = ResponseCode.ERROR_ADDING_PRODUCT_HINT;
			statusCode = HttpStatus.BAD_REQUEST;
			break;
		}
		case -3: {
			code = ResponseCode.ERROR_ADDING_VENDOR_HINT;
			statusCode = HttpStatus.BAD_REQUEST;
			break;
		}
		default: {
			code = ResponseCode.ERROR_ADDING_HINT;
			statusCode = HttpStatus.BAD_REQUEST;
			break;
		}
		}
		return ResponseUtil.message(code.getCode(), code.getMessage(), statusCode);
	}
	
	@GetMapping("/vulnerableComputers")
	public ResponseEntity<Object> getVulnerableComputers() {
		log.info("Received request to fetch all vulnerable computers.");
		List<ComputerResponseDto> vulnerableComputers = computerService.getAllComputersWithVulnerabilities();
		if (vulnerableComputers.isEmpty()) {
			log.info("No vulnerable computers found.");
			return ResponseUtil.message(ResponseCode.NO_DATA_FOUND);
		}
		log.info("Returning {} vulnerable computers.", vulnerableComputers.size());
		return ResponseUtil.data(vulnerableComputers);
	}
	
	@GetMapping({"/sendNotifications/{computerUuid}", "/sendNotifications"})
	public ResponseEntity<Object> sendNotifications(@PathVariable(required = false) String computerUuid) {
		log.info("Received request to send notifications.");
		if(computerUuid != null && !computerUuid.isEmpty()) {
			log.debug("Sending notification for computer with UUID: {}", computerUuid);
			emailService.sendVulnEmailNotification(computerService.getComputerWithVulnerabilitiesByUuid(computerUuid));
			return ResponseUtil.message(ResponseCode.NOTIFICATION_SENT_SUCCESSFULLY);
		}
		emailService.sendVulnEmailNotifications(computerService.getAllComputersWithVulnerabilities());
		return ResponseUtil.message(ResponseCode.NOTIFICATION_SENT_SUCCESSFULLY);
	}
	
}
