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
import com.isteer.vms.response.BaseResponse;
import com.isteer.vms.response.ResponseCode;
import com.isteer.vms.response.ResponseUtil;
import com.isteer.vms.service.ComputerService;
import com.isteer.vms.service.EmailService;
import com.isteer.vms.service.VulnerabilityService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
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

	@Operation(summary = "Create or update a computer", description = "This endpoint allows you to create a new computer or update an existing one. "
			+ "It accepts a ComputerPayloadDto object in the request body, "
			+ "which contains the details of the computer to be created or updated. "
			+ "The response will indicate the status of the operation, "
			+ "including whether a new computer was created, an existing computer was updated, "
			+ "or if there were no changes made. It also handles various error scenarios "
			+ "such as inactive computers, application errors, and internal errors. "
			+ "The response will include appropriate HTTP status codes and messages based on "
			+ "the outcome of the operation.", responses = {
					@ApiResponse(responseCode = "200", description = "Computer created or updated successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = BaseResponse.class))),
					@ApiResponse(responseCode = "400", description = "Bad Request - Invalid input or inactive computer", content = @Content(mediaType = "application/json", schema = @Schema(implementation = BaseResponse.class))),
					@ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(mediaType = "application/json", schema = @Schema(implementation = BaseResponse.class)))

	})
	@PostMapping("/computers")
	public ResponseEntity<Object> createComputer(@Valid @RequestBody ComputerPayloadDto computer) {
		log.info("Received request to create or update computer with deviceId: {}", computer.getDeviceId());
		int status = computerService.createOrUpdateComputer(computer);
		switch (status) {
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

	@Operation(summary = "Get all computers and their details", description = "This endpoint retrieves a list of all computers and their details. It returns a DashboardMetricsDto object containing the metrics of all computers, including the total number of computers, the number of computers with vulnerabilities, the number of vulnerable applications, and the number of vulnerabilities by severity.", responses = {
			@ApiResponse(responseCode = "200", description = "Dashboard metrics retrieved successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = DashboardMetricsDto.class))),
			@ApiResponse(responseCode = "204", description = "No data found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = BaseResponse.class))),
			@ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(mediaType = "application/json", schema = @Schema(implementation = BaseResponse.class))) })
	@GetMapping("/getDashboardMetrics")
	public ResponseEntity<Object> getMetrics() {
		log.info("Received request to fetch dashboard metrics.");
		DashboardMetricsDto metrics = computerService.getDashboardMetrics();
		if (metrics == null) {
			log.info("No metrics found.");
			return ResponseUtil.message(ResponseCode.NO_DATA_FOUND);
		} else {
			log.info("Returning dashboard metrics");
			return ResponseUtil.data(metrics);
		}
	}

	@Operation(summary = "Get likely CPE names", description = "This endpoint retrieves a list of likely CPE names based on the provided vendor and product names. "
			+ "It accepts vendor and product as request parameters, and an optional version parameter. "
			+ "If the vendor or product is missing, it returns a bad request response. "
			+ "If no likely CPE names are found, it returns a not found response. "
			+ "If successful, it returns a list of CpeName objects.", responses = {
				@ApiResponse( responseCode = "200", description = "Likely CPE names retrieved Successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = CpeName.class))),
				@ApiResponse(responseCode = "400", description = "Bad Request - Missing vendor or product parameter", content = @Content(mediaType = "application/json", schema = @Schema(implementation = BaseResponse.class))),
				@ApiResponse(responseCode = "404", description = "No Data Found - No likely CPE names found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = BaseResponse.class))),
				@ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content(mediaType = "application/json", schema = @Schema(implementation = BaseResponse.class)))
			})
	@GetMapping("hint/likelyCpeNames")
	public ResponseEntity<Object> getLikelyCpeNames(@RequestParam String vendor, @RequestParam String product,
			@RequestParam(required = false) String version) {
		List<CpeName> likelyCpeNames;
		if (vendor == null || product == null || vendor.trim().isEmpty() || product.trim().isEmpty()) {
			return ResponseUtil.message(ResponseCode.MISSING_PARAMETER_FOR_LIKELY_CPE_NAME_SEARCH,
					HttpStatus.BAD_REQUEST);
		}
		likelyCpeNames = fuzzySearchTool.searchForLikelyCpeName(vendor, product, version);
		if (likelyCpeNames.isEmpty()) {
			return ResponseUtil.message(ResponseCode.NO_DATA_FOUND, HttpStatus.NOT_FOUND);
		}
		return ResponseUtil.data(likelyCpeNames);
	}

	@Operation(summary = "Add a hint for an application", description = "This endpoint allows you to add a hint for an application based on its CPE name. "
			+ "It accepts a CPE name as a request parameter and an Application object in the request body. "
			+ "The response will indicate the status of the operation, "
			+ "including whether the hint was added successfully, "
			+ "if the CPE name is not valid, or if there was an error during the operation. "
			+ "It also triggers an asynchronous analysis of the application vulnerabilities if the hint is added successfully.", responses = {
				@ApiResponse(responseCode = "200", description = "Hint added successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = BaseResponse.class))),
				@ApiResponse(responseCode = "400", description = "Bad Request - Invalid CPE name or error adding hint", content = @Content(mediaType = "application/json", schema = @Schema(implementation = BaseResponse.class))),
				@ApiResponse(responseCode = "500", description = "Internal Server Error - Error adding hint", content = @Content(mediaType = "application/json", schema = @Schema(implementation = BaseResponse.class))),
				@ApiResponse(responseCode = "404", description = "Not Found - CPE name not valid", content = @Content(mediaType = "application/json", schema = @Schema(implementation = BaseResponse.class)))
			})
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

	@Operation(summary = "Get all vulnerable computers", description = "This endpoint retrieves a list of all computers that have vulnerabilities. "
			+ "It returns a list of ComputerResponseDto objects containing the details of each vulnerable computer. "
			+ "If no vulnerable computers are found, it returns a not found response. "
			+ "If successful, it returns a list of vulnerable computers.", responses = {
				@ApiResponse(responseCode = "200", description = "Vulnerable computers retrieved successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ComputerResponseDto.class))),
				@ApiResponse(responseCode = "404", description = "No Data Found - No vulnerable computers found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = BaseResponse.class))),
				@ApiResponse(responseCode = "500", description = "Internal Server Error", content =@Content(mediaType = "application/json", schema = @Schema(implementation = BaseResponse.class)))
			})
	@GetMapping("/vulnerableComputers")
	public ResponseEntity<Object> getVulnerableComputers() {
		log.info("Received request to fetch all vulnerable computers.");
		List<ComputerResponseDto> vulnerableComputers = computerService.getAllComputersWithVulnerabilities();
		if (vulnerableComputers.isEmpty()) {
			log.info("No vulnerable computers found.");
			return ResponseUtil.message(ResponseCode.NO_DATA_FOUND, HttpStatus.NOT_FOUND);
		}
		log.info("Returning {} vulnerable computers.", vulnerableComputers.size());
		return ResponseUtil.data(vulnerableComputers);
	}

	@Operation(summary = "Send notifications for vulnerable computers", description = "This endpoint sends email notifications for vulnerable computers. "
			+ "You can specify a computer UUID to send a notification for a specific computer, "
			+ "or leave it empty to send notifications for all vulnerable computers. "
			+ "The response will indicate whether the notifications were sent successfully or not.", responses = {
				@ApiResponse(responseCode = "200", description = "Notifications sent successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = BaseResponse.class))),
				@ApiResponse(responseCode = "400", description = "Bad request - Invalid computer UUID", content = @Content(mediaType = "application/json", schema = @Schema(implementation = BaseResponse.class))),
				@ApiResponse(responseCode = "500", description = "Internal Server Error - Error sending notifications", content = @Content(mediaType = "application/json", schema = @Schema(implementation = BaseResponse.class)))
			})
	@GetMapping({ "/sendNotifications/{computerUuid}", "/sendNotifications" })
	public ResponseEntity<Object> sendNotifications(@PathVariable(required = false) String computerUuid) {
		log.info("Received request to send notifications.");
		if (computerUuid != null && !computerUuid.isEmpty()) {
			log.debug("Sending notification for computer with UUID: {}", computerUuid);
			emailService.sendVulnEmailNotification(computerService.getComputerWithVulnerabilitiesByUuid(computerUuid));
			return ResponseUtil.message(ResponseCode.NOTIFICATION_SENT_SUCCESSFULLY);
		}
		emailService.sendVulnEmailNotifications(computerService.getAllComputersWithVulnerabilities());
		return ResponseUtil.message(ResponseCode.NOTIFICATION_SENT_SUCCESSFULLY);
	}

}
