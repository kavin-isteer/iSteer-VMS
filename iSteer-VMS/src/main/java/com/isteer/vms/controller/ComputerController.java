package com.isteer.vms.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.isteer.vms.core.engine.fuzzysearch.FuzzySearchTool;
import com.isteer.vms.core.engine.model.CpeName;
import com.isteer.vms.dto.ComputerPayloadDto;
import com.isteer.vms.dto.DashboardMetricsDto;
import com.isteer.vms.model.Application;
import com.isteer.vms.model.Computer;
import com.isteer.vms.response.ResponseCode;
import com.isteer.vms.response.ResponseUtil;
import com.isteer.vms.service.ComputerService;
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

	public ComputerController(ComputerService computerService, VulnerabilityService vulnerabilityService,
			FuzzySearchTool fuzzySearchTool) {
		super();
		this.computerService = computerService;
		this.vulnerabilityService = vulnerabilityService;
		this.fuzzySearchTool = fuzzySearchTool;
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
			return ResponseEntity.noContent().build();
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
			return ResponseEntity.noContent().build();
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
			Map<String, String> responseMessage = new HashMap<>();
			responseMessage.put("Status", "Vendor and Product cannot be empty!!");
			return new ResponseEntity<>(responseMessage, HttpStatus.BAD_REQUEST);
		}
		likelyCpeNames = fuzzySearchTool.searchForLikelyCpeName(vendor, product, version);
		return new ResponseEntity<>(likelyCpeNames, HttpStatus.OK);
	}
	
	@PostMapping("/hint/addHint")
	public ResponseEntity<Object> addApplicationHint(@RequestParam String cpeName,
			@RequestBody Application application) {
		int status = vulnerabilityService.addApplicationHint(cpeName, application);
		log.info("Status: {}", status);
		String statusMessage = "";
		switch (status) {
		case 1: {
			statusMessage = "Hint added Successfully!!";
			break;
		}
		case -1: {
			statusMessage = "CPE name is not valid!!";
			break;
		}
		case -2: {
			statusMessage = "Error while adding product hint!!";
			break;
		}
		case -3: {
			statusMessage = "Error while adding vendor hint!!";
			break;
		}
		default: {
			statusMessage = "Error while adding dependnecy hint!!";
			break;
		}
		}
		Map<String, String> responseMessage = new HashMap<>();
		responseMessage.put("Status", statusMessage);
		return new ResponseEntity<>(responseMessage, HttpStatus.OK);

	}
	
}
