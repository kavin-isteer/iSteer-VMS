package com.isteer.vms.controller;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.isteer.vms.dto.ComputerPayloadDto;
import com.isteer.vms.model.Computer;
import com.isteer.vms.service.ComputerService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api")
public class ComputerController {
	
	private static final Logger logger = LogManager.getLogger(ComputerController.class);
	private ComputerService computerService;
	
	public ComputerController(ComputerService computerService) {
		this.computerService = computerService;
	}

	@PostMapping("/computer")
	public ResponseEntity<?> createComputer(@Valid @RequestBody ComputerPayloadDto computer){
		logger.info("Received request to create or update computer with deviceId: {}", computer.getDeviceId());
		int status = computerService.createOrUpdateComputer(computer);
		switch(status) {
			case 0:
				return ResponseEntity.ok("No changes made to the computer");
			case 1:
				return ResponseEntity.ok("Computer updated successfully");
			case 2:
				return ResponseEntity.ok("Computer and application updated successfully");
			case 3:
				return ResponseEntity.ok("Application data updated successfully.");
			case 4:
				return ResponseEntity.ok("Computer created successfully");
			case -1:
				return ResponseEntity.status(400).body("Cannot update computer as it is deleted. Please restore it first.");
			case -2:
				return ResponseEntity.status(400).body("Cannot update computer as it is inactive. Please activate it first.");
			case -3:
				return ResponseEntity.status(400).body("Error while processing application data. Please check the input data");
			case -4:
				return ResponseEntity.status(404).body("Internal error while processing request. Please try again later.");
			default:
				return ResponseEntity.status(500).body("Error processing request");
		}
	}
	
	@GetMapping("/computer")
	public ResponseEntity<List<Computer>> getAllComputers(){
		logger.info("Received request to fetch all computers.");
		List<Computer> computers = computerService.getAllComnputers();
		if(computers.isEmpty()) {
			logger.info("No computers Found.");
			return ResponseEntity.noContent().build();
		}
		logger.info("Returning {} computers.", computers.size());
		return ResponseEntity.ok(computers);
	}
}
