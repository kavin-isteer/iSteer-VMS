package com.isteer.vms.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.isteer.vms.model.Application;
import com.isteer.vms.response.BaseResponse;
import com.isteer.vms.response.ResponseCode;
import com.isteer.vms.response.ResponseUtil;
import com.isteer.vms.service.ApplicationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.extern.log4j.Log4j2;

@RestController
@Log4j2
public class ApplicationController {
	
	private ApplicationService applicationService;

	public ApplicationController(ApplicationService applicationService) {
		super();
		this.applicationService = applicationService;
	}
	
	@Operation(summary = "Get all applications", description = "This endpoint retrieves all applications from the database. "
			+ "It can filter applications based on their vulnerability status if the 'isVulnerable' parameter is provided. "
			+ "If no applications are found, it returns a No Content response.", responses = {
					@ApiResponse(responseCode = "200", description = "Applications retrieved successfully", content =@Content(mediaType = "application/json", schema = @Schema(implementation = Application.class))),
					@ApiResponse(responseCode = "204", description = "No applications found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = BaseResponse.class))),
					@ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(mediaType = "application/json", schema = @Schema(implementation = BaseResponse.class)))
			})
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
	
	@Operation(summary = "Get unresolved applications", description = "This endpoint retrieves all unresolved applications from the database. "
			+ "An unresolved application is one that has not been processed or has not been resolved yet. "
			+ "If no unresolved applications are found, it returns a message indicating no data found", responses = {
				@ApiResponse(responseCode = "200", description = "Unresolved applications retrieved successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Application.class))),
				@ApiResponse(responseCode = "404", description = "No unresolved applications found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = BaseResponse.class))),
				@ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(mediaType = "application/json", schema = @Schema(implementation = BaseResponse.class)))
			})
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
