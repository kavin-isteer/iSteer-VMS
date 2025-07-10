package com.isteer.vms.core.engine.model;

import java.util.ArrayList;
import java.util.List;

import com.isteer.vms.model.Application;

public class BaseApplication {
	
	private Application application;
	private List<Evidence> vendorEvidence = new ArrayList<>();
	private List<Evidence> productEvidence = new ArrayList<>();
	private List<Evidence> versionEvidence = new ArrayList<>();
	private CpeName cpeEnumeration;
	private List<Vulnerability> vulnerabilities = new ArrayList<>();
	private List<CpeName> likelyCPEs=new ArrayList<>();
	
	public BaseApplication() {
		// TODO Auto-generated constructor stub
	}
	public BaseApplication(Application application) {
		this.application=application;
	}
	public Application getApplication() {
		return application;
	}
	public void setApplication(Application application) {
		this.application = application;
	}
	public List<Evidence> getVendorEvidence() {
		return vendorEvidence;
	}
	public void addVendorEvidence(Evidence vendorEvidence) {
		this.vendorEvidence.add(vendorEvidence);
	}
	public List<Evidence> getProductEvidence() {
		return productEvidence;
	}
	public void addProductEvidence(Evidence productEvidence) {
		this.productEvidence.add(productEvidence);
	}
	public List<Evidence> getVersionEvidence() {
		return versionEvidence;
	}
	public void addVersionEvidence(Evidence versionEvidence) {
		this.versionEvidence.add(versionEvidence);
	}
	public CpeName getCpeEnumeration() {
		return cpeEnumeration;
	}
	public void setCpeEnumeration(CpeName cpeEnumeration) {
		this.cpeEnumeration = cpeEnumeration;
	}
	public List<Vulnerability> getVulnerabilities() {
		return vulnerabilities;
	}
	public void addVulnerabilities(Vulnerability vulnerabilities) {
		this.vulnerabilities.add(vulnerabilities);
	}
	public List<CpeName> getLikelyCPEs() {
		return likelyCPEs;
	}
	public void addLikelyCPEs(CpeName likelyCPEs) {
		this.likelyCPEs.add(likelyCPEs);
	}
	
	@Override
	public String toString() {
		return "BaseApplication [application=" + application + ", vendorEvidence=" + vendorEvidence
				+ ", productEvidence=" + productEvidence + ", versionEvidences=" + versionEvidence
				+ ", cpeEnumeration=" + cpeEnumeration + ", vulnerabilities=" + vulnerabilities + ", likelyCPEs="
				+ likelyCPEs + "]";
	}
	
}
