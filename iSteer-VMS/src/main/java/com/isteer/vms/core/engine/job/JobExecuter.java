package com.isteer.vms.core.engine.job;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.isteer.vms.core.engine.Engine;

import lombok.extern.log4j.Log4j2;

@Component
@Log4j2
public class JobExecuter {
	
	private NVDDataProcessor nvdDataProcessor;
	
	public JobExecuter(NVDDataProcessor nvdDataProcessor) {
		this.nvdDataProcessor = nvdDataProcessor;
	}

	// A map to store the status of jobs based on the job ID
	private final Map<String, String> jobStatusMap = new HashMap<>();

	// Method to start a job in a new thread and return the job ID
	public String startupdateCpeDictionaryJob() {
		// Generate a unique Job ID
		String jobId = UUID.randomUUID().toString();

		// Create a new thread to execute the job
		new Thread(() -> {
			long startTime = System.currentTimeMillis(); // Record start time
			try {
				// Set the job status to "In Progress"
				jobStatusMap.put(jobId, "In Progress");

				// Execute the updateCpeDictionary method of NVDDataProcessor
				log.info("[JOB STARTED] Job " + jobId + " started...");
				nvdDataProcessor.updateCpeDictionary(); // Actual job execution
				Engine.initializeLuceneIndex(); // Reinitialize Lucene index after updating CPE dictionary

				// Calculate total execution time
				long endTime = System.currentTimeMillis(); // Record end time
				long executionTimeMs = endTime - startTime; // Total execution time in milliseconds

				// Convert milliseconds to minutes and seconds
				long minutes = (executionTimeMs / 1000) / 60;
				long seconds = (executionTimeMs / 1000) % 60;
				long milliseconds = executionTimeMs % 1000;

				// Set the job status to "Completed" once the job is finished
				jobStatusMap.put(jobId, "Completed");
				log.info("[JOB COMPLETED] Job " + jobId + " completed. Total execution time: "+executionTimeMs+" ms ("+ 
                        + minutes + " minutes, " + seconds + " seconds, " + milliseconds + " milliseconds)");

			} catch (Exception e) {
				// In case of an error, set status to "Failed"
				jobStatusMap.put(jobId, "Failed");
				log.error("[JOB FAILED] Job " + jobId + " failed due to an error: " + e.getMessage());
			}
		}).start();

		// Return the generated job ID for tracking
		return jobId;
	}

	// Method to get the current status of a job based on the job ID
	public String getJobStatus(String jobId) {
		// Return the status of the job if available, else return a default status
		return jobStatusMap.getOrDefault(jobId, "Job ID not found");
	}
	
	// Scheduled method to execute the job every 3 days at midnight (00:00)
    @Scheduled(cron = "0 0 1 * * MON")
    public void scheduleJob() {
        log.info("[SCHEDULED JOB] Starting scheduled job ........");
        startupdateCpeDictionaryJob();  // Trigger the job
    }
}
