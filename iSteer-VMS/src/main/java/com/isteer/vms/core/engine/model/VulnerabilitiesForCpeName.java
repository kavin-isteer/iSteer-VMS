package com.isteer.vms.core.engine.model;

import java.util.List;

public class VulnerabilitiesForCpeName {

	private String cpeName;
	private List<Vulnerability> vulnerabilities;
	
	public String getCpeName() {
		return cpeName;
	}
	public void setCpeName(String cpeName) {
		this.cpeName = cpeName;
	}	
	public List<Vulnerability> getVulnerabilities() {
		return vulnerabilities;
	}
	public void setVulnerabilities(List<Vulnerability> vulnerabilities) {
		this.vulnerabilities = vulnerabilities;
	}
	
	@Override
	public String toString() {
		return "VulnerabilityModel [cpeName=" + cpeName + ", vulnerabilities=" + vulnerabilities + "]";
	}
	
}
