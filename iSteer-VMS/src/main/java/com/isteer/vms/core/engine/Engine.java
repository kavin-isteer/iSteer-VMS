package com.isteer.vms.core.engine;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.isteer.vms.core.engine.dao.CpeHintDao;
import com.isteer.vms.core.engine.enums.CpeField;
import com.isteer.vms.core.engine.enums.EvidenceType;
import com.isteer.vms.core.engine.enums.ResolveMethod;
import com.isteer.vms.core.engine.model.BaseApplication;
import com.isteer.vms.core.engine.model.CpeName;
import com.isteer.vms.core.engine.model.Evidence;
import com.isteer.vms.model.Application;

@Service
public class Engine {
	
	@Autowired
	CpeHintDao cpeHintDao;

	public Map<String, BaseApplication> collectEvidencesAndFetchVulnerabilities(List<Application> applications){
		Map<String, BaseApplication> insertedApplications = new HashMap<>();
		
		for(Application application : applications) {
			if(!application.getUuid().isEmpty() && application.getUuid() != null) {
				BaseApplication resolvedApplication = resolveSoftwareNames(application);
				System.out.println(resolvedApplication);
			}
		}
		return null;
	}

	private BaseApplication resolveSoftwareNames(Application application) {
		
		BaseApplication resolvedApplication = new BaseApplication();
		
		Evidence productEvidence = new Evidence();
		Evidence vendorEvidence = new Evidence();
		Evidence versionEvidence = new Evidence();
		
		CpeName cpe = new CpeName();

		String evidenceTitle = "Application-Metadata";
		
		resolvedApplication.setApplication(application);

		vendorEvidence.setEvidenceType(EvidenceType.APPLICATION);
		vendorEvidence.setEvidenceTitle(evidenceTitle);
		vendorEvidence.setEvidence(application.getVendorName());

		productEvidence.setEvidenceType(EvidenceType.APPLICATION);
		productEvidence.setEvidenceTitle(evidenceTitle);
		productEvidence.setEvidence(application.getSoftwareName());

		versionEvidence.setEvidenceType(EvidenceType.APPLICATION);
		versionEvidence.setEvidenceTitle(evidenceTitle);
		versionEvidence.setEvidence(application.getSoftwareVersion());
		versionEvidence.setResolvedValue(application.getSoftwareVersion());
		cpe.setVersion(application.getSoftwareVersion());
		cpe.addResolveMethod(CpeField.VERSION, ResolveMethod.ARBITRARY);
		
		String resolvedVendor = cpeHintDao.getStandardisedNameForMatchKey(application.getVendorName(), "vendor");
		
		if(resolvedVendor != null && !resolvedVendor.isEmpty()) {
			vendorEvidence.setResolvedValue(resolvedVendor);
			cpe.setVendor(resolvedVendor);
			cpe.addResolveMethod(CpeField.VENDOR, ResolveMethod.HINT_BY_DEVELOPER);
		}
		
		String resolvedProduct = cpeHintDao.getStandardisedNameForMatchKey(application.getSoftwareName(), "product");
		if(resolvedProduct != null && !resolvedProduct.isEmpty()) {
			productEvidence.setResolvedValue(resolvedProduct);
			cpe.setProduct(resolvedProduct);
			cpe.addResolveMethod(CpeField.PRODUCT, ResolveMethod.HINT_BY_DEVELOPER);
		}
		
		resolvedApplication.addVendorEvidence(vendorEvidence);
		resolvedApplication.addProductEvidence(productEvidence);
		resolvedApplication.addVersionEvidence(versionEvidence);
		resolvedApplication.setCpeEnumeration(cpe);
		
		return resolvedApplication;
	}
}
