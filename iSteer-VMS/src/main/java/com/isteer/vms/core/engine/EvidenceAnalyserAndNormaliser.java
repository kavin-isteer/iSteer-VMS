package com.isteer.vms.core.engine;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;

import com.isteer.vms.core.engine.enums.CpeField;
import com.isteer.vms.core.engine.enums.EvidenceType;
import com.isteer.vms.core.engine.enums.ResolveMethod;
import com.isteer.vms.core.engine.model.CpeName;
import com.isteer.vms.core.engine.model.Evidence;
import com.isteer.vms.core.engine.nvdclient.NvdClient;

public class EvidenceAnalyserAndNormaliser {
	@Autowired
	NvdClient nvdClient;
	public Map<String, DependencyModel> softwareAnalyzerAndNormalizer(List<ApplicationModel> applications) {
		NvdClient nvdClient = new NvdClient();
		Map<String, DependencyModel> insertedApplications = new HashMap<>();

		for (ApplicationModel application : applications) {
			if (!application.isExists() && application.getApplicationUuid() != null) {
				try {
					DependencyModel resolvedApplication = resolveOsSoftwareNames(application);

					if (resolvedApplication.getCpeEnumeration() != null
							&& !(resolvedApplication.getCpeEnumeration().getResolveMethod().isEmpty())) {
						nvdClient.fetchVulnerabilitiesForDependency(resolvedApplication);
					}
					insertedApplications.put(application.getApplicationUuid(), resolvedApplication);

				} catch (Exception e) {
					Engine.logger.error("Error while resolving OS software names for application: "
							+ application.getApplicationName() + " - " + e.getMessage());
				}
			}
		}
		return insertedApplications;
	}

	public DependencyModel resolveOsSoftwareNames(ApplicationModel application) throws SQLException {
		// This method will resolve the OS software names from the list of applications
		// and return a list of DependencyModel objects.
		DependencyHintDao hintDao = new DependencyHintDao();
		DbUtil dbUtil = new DbUtil();
		DependencyModel resolvedApplication = new DependencyModel();

		Evidence productEvidence = new Evidence();
		Evidence vendorEvidence = new Evidence();
		Evidence versionEvidence = new Evidence();

		CpeName cpe = new CpeName();

		String evidenceTitle = "Application-Metadata";

		vendorEvidence.setEvidenceType(EvidenceType.APPLICATION);
		vendorEvidence.setEvidenceTitle(evidenceTitle);
		vendorEvidence.setEvidence(application.getApplicationVendor());

		productEvidence.setEvidenceType(EvidenceType.APPLICATION);
		productEvidence.setEvidenceTitle(evidenceTitle);
		productEvidence.setEvidence(application.getApplicationName());

		versionEvidence.setEvidenceType(EvidenceType.APPLICATION);
		versionEvidence.setEvidenceTitle(evidenceTitle);
		versionEvidence.setEvidence(application.getApplicationVersion());
		versionEvidence.setResolvedValue(application.getApplicationVersion());

		String resolvedVendor = hintDao.getVendorAndProductNameForApplication(dbUtil.getConnection(),
				application.getApplicationVendor(), "vendor");

		if (resolvedVendor != null && !resolvedVendor.isEmpty()) {
			vendorEvidence.setResolvedValue(resolvedVendor);
		}
		
		String resolvedProduct = hintDao.getVendorAndProductNameForApplication(dbUtil.getConnection(),
				application.getApplicationName(), "product");
		
		if (resolvedProduct != null && !resolvedProduct.isEmpty()) {
			productEvidence.setResolvedValue(resolvedProduct);
		}

		if (productEvidence.getResolvedValue() != null && vendorEvidence.getResolvedValue() != null
				&& versionEvidence.getResolvedValue() != null) {
			cpe.setProduct(productEvidence.getResolvedValue());
			cpe.setVendor(vendorEvidence.getResolvedValue());
			cpe.setVersion(versionEvidence.getResolvedValue());
			cpe.addResolveMethod(CpeField.PRODUCT, ResolveMethod.HINT_BY_DEVELOPER);
			cpe.addResolveMethod(CpeField.VENDOR, ResolveMethod.HINT_BY_DEVELOPER);
			cpe.addResolveMethod(CpeField.VERSION, ResolveMethod.ARBITRARY);
		}

		resolvedApplication.addVendorEvidence(vendorEvidence);
		resolvedApplication.addProductEvidences(productEvidence);
		resolvedApplication.addVersionEvidences(versionEvidence);
		resolvedApplication.setCpeEnumeration(cpe);

		return resolvedApplication;
	}
}
