package com.isteer.vms.service;

import java.util.List;

import com.isteer.vms.dto.ApplicationResponseDto;
import com.isteer.vms.dto.SoftwarePayloadDto;
import com.isteer.vms.model.Application;

public interface ApplicationService {
	/**
	 * Orchestrates the synchronization of installed software for a specific computer.
	 * It handles the creation of new applications, mapping them to the computer,
	 * marking uninstalled applications as deleted, and reactivating reinstalled ones.
	 *
	 * @param computerUuid The UUID of the computer whose software list is being processed.
	 * @param software     A list of {@link SoftwarePayloadDto} representing the currently installed software on the computer.
	 * @return An integer status code:
	 * <ul>
	 * <li>-1: An error occurred.</li>
	 * <li>0: No changes were made.</li>
	 * <li>1: Only new mappings were added.</li>
	 * <li>2: A mix of operations (mappings, deletions, activations) occurred.</li>
	 * </ul>
	 */
	int createOrUpdateApplication(String computerUuid, List<SoftwarePayloadDto> software);
	/**
	 * Retrieves a list of all applications, with an optional filter for vulnerability status.
	 *
	 * @param isVulnerable A string ("true" or "false") to filter by. If null or empty, all applications are returned.
	 * @return A list of {@link Application} objects.
	 */
	List<Application> getAllApplications(String isVulnerable);
	/**
	 * Gathers and aggregates detailed information for all applications on a given computer.
	 *
	 * @param computerUuid The UUID of the computer.
	 * @return A list of {@link ApplicationResponseDto} containing aggregated details like vulnerability counts and CPE names.
	 */
	List<ApplicationResponseDto> getApplicationDetails(String computerUuid);
	/**
	 * Retrieves a list of all applications that have an unresolved CPE name.
	 *
	 * @return A list of {@link Application} objects.
	 */
	List<Application> getUnresolvedApplications();

}
