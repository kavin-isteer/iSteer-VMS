package com.isteer.vms.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.isteer.vms.dto.ComputerPayloadDto;
import com.isteer.vms.dto.DashboardMetricsDto;
import com.isteer.vms.model.Computer;
import com.isteer.vms.response.ResponseCode;
import com.isteer.vms.response.ResponseUtil;
import com.isteer.vms.service.ComputerService;

import jakarta.validation.Valid;
import lombok.extern.log4j.Log4j2;

@RestController
@Log4j2
@RequestMapping("/api")
public class ComputerController {
	
	private ComputerService computerService;
	
	public ComputerController(ComputerService computerService) {
		this.computerService = computerService;
	}

	@PostMapping("/computers")
	public ResponseEntity<?> createComputer(@Valid @RequestBody ComputerPayloadDto computer){
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
	public ResponseEntity<List<Computer>> getAllComputers(@RequestParam(required = false) String status){
		log.info("Received request to fetch all computers.");
		List<Computer> computers = computerService.getAllComnputers(status);
		if(computers.isEmpty()) {
			log.info("No computers Found.");
			return ResponseEntity.noContent().build();
		}
		log.info("Returning {} computers.", computers.size());
		return ResponseEntity.ok(computers);
	}
	
	@GetMapping("/getDashboardMetrics")
	public ResponseEntity<DashboardMetricsDto> getMetrics() {
		log.info("Received request to fetch dashboard metrics.");
		DashboardMetricsDto metrics = computerService.getDashboardMetrics();
		if(metrics == null) {
			log.info("No metrics found.");
			return ResponseEntity.noContent().build();
		} else {
			log.info("Returning dashboard metrics");
			return ResponseEntity.ok(metrics);
		}
	}
}
