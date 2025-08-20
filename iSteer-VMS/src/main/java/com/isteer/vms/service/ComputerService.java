package com.isteer.vms.service;

import java.util.List;

import com.isteer.vms.dto.ComputerPayloadDto;
import com.isteer.vms.dto.ComputerResponseDto;
import com.isteer.vms.dto.DashboardMetricsDto;
import com.isteer.vms.model.Computer;

public interface ComputerService {

	/**
     * Creates a new computer record or updates an existing one based on the provided payload.
     * <p>
     * The operation includes handling the associated application data. The method determines
     * whether the computer already exists and performs the appropriate action:
     * <ul>
     *   <li>If the computer is marked as deleted or inactive, the update is rejected with an error code.</li>
     *   <li>If the computer does not exist, a new record is created.</li>
     *   <li>If the computer exists, the record and/or associated application data are updated as necessary.</li>
     * </ul>
     *
     * @param computer The payload containing details of the computer and related applications.
     *                 Must not be {@code null}.
     * @return An integer status code indicating the result of the operation:
     * <ul>
     *   <li>{@code 0}  - No changes were made.</li>
     *   <li>{@code 1}  - Computer record updated successfully.</li>
     *   <li>{@code 2}  - Computer and application data updated successfully.</li>
     *   <li>{@code 3}  - Application data updated successfully.</li>
     *   <li>{@code 4}  - New computer record created successfully.</li>
     *   <li>{@code -1} - Update rejected: computer is marked as deleted.</li>
     *   <li>{@code -2} - Update rejected: computer is inactive.</li>
     *   <li>{@code -3} - Error occurred while processing application data.</li>
     *   <li>{@code -4} - Internal error occurred during processing.</li>
     * </ul>
     * @throws IllegalArgumentException if the provided payload is invalid or {@code null}.
     * @throws RuntimeException if an unexpected error occurs during processing.
     */
	int createOrUpdateComputer(ComputerPayloadDto computer);

	DashboardMetricsDto getDashboardMetrics();
	
	List<ComputerResponseDto> getAllComputersWithVulnerabilities();

	ComputerResponseDto getComputerWithVulnerabilitiesByUuid(String computerUuid);

}
