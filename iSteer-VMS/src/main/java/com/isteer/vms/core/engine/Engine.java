package com.isteer.vms.core.engine;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.isteer.vms.core.engine.dao.CpeHintDao;
import com.isteer.vms.core.engine.enums.CpeField;
import com.isteer.vms.core.engine.enums.EvidenceType;
import com.isteer.vms.core.engine.enums.ResolveMethod;
import com.isteer.vms.core.engine.lucene.LuceneIndexRunner;
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
	private static LuceneIndexRunner luceneIndexRunner;

	public Engine(CpeHintDao cpeHintDao, NvdClient nvdClient,LuceneIndexRunner luceneIndexRnr) {
		this.cpeHintDao = cpeHintDao;
		this.nvdClient = nvdClient;
		luceneIndexRunner = luceneIndexRnr;
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
		String resolvedProduct = cpeHintDao.getStandardisedNameForMatchKey(application.getSoftwareName(), "product");
		
		if (resolvedVendor != null && !resolvedVendor.isEmpty() && resolvedProduct != null && !resolvedProduct.isEmpty()) {
			vendorEvidence.setResolvedValue(resolvedVendor);
			cpe.setVendor(resolvedVendor);
			cpe.addResolveMethod(CpeField.VENDOR, ResolveMethod.HINT_BY_DEVELOPER);
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
	
	public static void initializeLuceneIndex() {
		Engine.log.info("Initializing lucene index....");
		try {
			File indexDir = new File("lucene-index");
			boolean indexExists = indexDir.exists() && indexDir.isDirectory() && indexDir.list().length > 0;

			if (!indexExists) {
				log.info("Lucene index not exists.... Creating index");
				luceneIndexRunner.createIndexFromCvssDb();
			}else {
				log.info("Lucene index found...!!");
			}
		} catch (IOException e) {
			log.error("IO Exception occured during creating lucene index for CPE Entries!!");
		} catch (SQLException e) {
			log.error("SQL exception occured while fetcihng CPE entries from DB!!");
		}
	}
}
