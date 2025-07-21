package com.isteer.vms.core.engine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.isteer.vms.core.engine.dao.CpeHintDao;
import com.isteer.vms.core.engine.enums.CpeField;
import com.isteer.vms.core.engine.enums.EvidenceType;
import com.isteer.vms.core.engine.enums.ResolveMethod;
import com.isteer.vms.core.engine.model.BaseApplication;
import com.isteer.vms.core.engine.model.CpeName;
import com.isteer.vms.core.engine.model.Evidence;
import com.isteer.vms.core.engine.nvdclient.NvdClient;
import com.isteer.vms.model.Application;

import lombok.extern.log4j.Log4j2;

@Log4j2
@Service
public class Engine {

	private CpeHintDao cpeHintDao;
	private NvdClient nvdClient;

	public Engine(CpeHintDao cpeHintDao, NvdClient nvdClient) {
		super();
		this.cpeHintDao = cpeHintDao;
		this.nvdClient = nvdClient;
	}

	public Map<String, BaseApplication> collectEvidencesAndFetchVulnerabilities(List<Application> applications) {
	    List<CompletableFuture<BaseApplication>> futures = new ArrayList<>();

	    for (Application app : applications) {
	        BaseApplication baseApp = resolveSoftwareNames(app);
	        CompletableFuture<BaseApplication> future = nvdClient.fetchVulnerabilitiesRateLimited(baseApp);
	        futures.add(future);
	    }

	    // Wait for all to complete
	    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

	    // Now safely collect results
	    return futures.stream()
	        .map(CompletableFuture::join)
	        .collect(Collectors.toMap(
	            app -> app.getApplication().getUuid(),
	            app -> app
	        ));
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
		vendorEvidence.setEvidences(application.getVendorName());

		productEvidence.setEvidenceType(EvidenceType.APPLICATION);
		productEvidence.setEvidenceTitle(evidenceTitle);
		productEvidence.setEvidences(application.getSoftwareName());

		versionEvidence.setEvidenceType(EvidenceType.APPLICATION);
		versionEvidence.setEvidenceTitle(evidenceTitle);
		versionEvidence.setEvidences(application.getSoftwareVersion());
		versionEvidence.setResolvedValue(application.getSoftwareVersion());
		cpe.setVersion(application.getSoftwareVersion());
		cpe.addResolveMethod(CpeField.VERSION, ResolveMethod.ARBITRARY);

		String resolvedVendor = cpeHintDao.getStandardisedNameForMatchKey(application.getVendorName(), "vendor");

		if (resolvedVendor != null && !resolvedVendor.isEmpty()) {
			vendorEvidence.setResolvedValue(resolvedVendor);
			cpe.setVendor(resolvedVendor);
			cpe.addResolveMethod(CpeField.VENDOR, ResolveMethod.HINT_BY_DEVELOPER);
		}

		String resolvedProduct = cpeHintDao.getStandardisedNameForMatchKey(application.getSoftwareName(), "product");
		if (resolvedProduct != null && !resolvedProduct.isEmpty()) {
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
