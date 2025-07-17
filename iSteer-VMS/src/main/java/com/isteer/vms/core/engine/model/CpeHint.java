package com.isteer.vms.core.engine.model;

import java.time.LocalDateTime;

import com.isteer.vms.core.engine.enums.HintAddedBy;

public class CpeHint {
	private Integer id;
	private String type;
	private String matchKey;
	private String standardizedName;
	private String confidence;
	private String description;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
	private String evidenceType;
	private HintAddedBy addedBy;
	public Integer getId() {
		return id;
	}
	public void setId(Integer id) {
		this.id = id;
	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public String getMatchKey() {
		return matchKey;
	}
	public void setMatchKey(String matchKey) {
		this.matchKey = matchKey;
	}
	public String getStandardizedName() {
		return standardizedName;
	}
	public void setStandardizedName(String standardizedName) {
		this.standardizedName = standardizedName;
	}
	public String getConfidence() {
		return confidence;
	}
	public void setConfidence(String confidence) {
		this.confidence = confidence;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public LocalDateTime getCreatedAt() {
		return createdAt;
	}
	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}
	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}
	public void setUpdatedAt(LocalDateTime updatedAt) {
		this.updatedAt = updatedAt;
	}
	public String getEvidenceType() {
		return evidenceType;
	}
	public void setEvidenceType(String evidenceType) {
		this.evidenceType = evidenceType;
	}
	public HintAddedBy getAddedBy() {
		return addedBy;
	}
	public void setAddedBy(HintAddedBy addedBy) {
		this.addedBy = addedBy;
	}
}
