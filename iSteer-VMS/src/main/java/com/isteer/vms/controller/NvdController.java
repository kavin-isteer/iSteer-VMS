package com.isteer.vms.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.isteer.vms.core.engine.job.JobExecuter;

@RestController
@RequestMapping("/api")
public class NvdController {
	@Autowired
	JobExecuter jobExecutor;

	@GetMapping("/nvd/executeJob/updateCpeDictionary")
	public ResponseEntity<?> updateCpeDictionary() {
		Map<String, String> status = new HashMap<>();
		try {
			String jobId = jobExecutor.startupdateCpeDictionaryJob();
			status.put("jobId", jobId);
			return ResponseEntity.ok(status);
		} catch (Exception e) {
			return ResponseEntity.status(500).body("Error starting the job" + e.getMessage());
		}
	}
	@GetMapping("/nvd/jobStatus")
	public ResponseEntity<?> getJobStatus(String jobId) {
		Map<String, String> status = new HashMap<>();
		try {
			String jobStatus = jobExecutor.getJobStatus(jobId);
			status.put("jobId", jobId);
			status.put("status", jobStatus);
			return ResponseEntity.ok(status);
		} catch (Exception e) {
			return ResponseEntity.status(500).body("Error fetching the job status: " + e.getMessage());
		}
	}

}
