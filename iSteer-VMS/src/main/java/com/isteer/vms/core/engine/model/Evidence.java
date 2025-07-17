package com.isteer.vms.core.engine.model;

import com.isteer.vms.core.engine.enums.EvidenceType;

public class Evidence {
	private EvidenceType evidenceType;
	private String evidenceTitle;
	private String evidences;
	private String resolvedValue;
	public EvidenceType getEvidenceType() {
		return evidenceType;
	}
	public void setEvidenceType(EvidenceType evidenceType) {
		this.evidenceType = evidenceType;
	}
	public String getEvidences() {
		return evidences;
	}
	public void setEvidences(String evidences) {
		this.evidences = evidences;
	}
	public String getResolvedValue() {
		return resolvedValue;
	}
	public void setResolvedValue(String resolvedValue) {
		this.resolvedValue = resolvedValue;
	}
	public String getEvidenceTitle() {
		return evidenceTitle;
	}
	public void setEvidenceTitle(String evidenceTitle) {
		this.evidenceTitle = evidenceTitle;
	}
	@Override
	public String toString() {
		return "Evidence [evidenceType=" + evidenceType + ", evidence=" + evidences + ", resolvedValue=" + resolvedValue
				+ "]";
	}
}