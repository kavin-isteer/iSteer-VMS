package com.isteer.vms.core.engine.fuzzysearch;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.isteer.vms.core.engine.lucene.LuceneCpeSearcher;
import com.isteer.vms.core.engine.model.CpeEntry;
import com.isteer.vms.core.engine.model.CpeName;

@Service
public class FuzzySearchTool {
	@Autowired
	LuceneCpeSearcher luceneSearcher;
	
	private static final Logger logger = LogManager.getLogger(FuzzySearchTool.class);
	
	public List<CpeName> searchForLikelyCpeName(String vendorSearchString, String productSearchString, String versionSearchString) {
		List<CpeEntry> filteredCpes = new ArrayList<>();
		List<CpeName> likelyCpeNames = new ArrayList<>();
		try {
			filteredCpes = luceneSearcher.multiFieldSearch(vendorSearchString, productSearchString, versionSearchString);
			logger.info("Found likely cpes: "+filteredCpes.size());
		}catch (Exception e) {
			logger.debug(e.getMessage());
			logger.debug("Exception occured while searching for likely cpes !!");
		}
		if (filteredCpes.size() > 0) {
			for (CpeEntry entry : filteredCpes) {
				CpeName wrkCpeNameModel = new CpeName();
				wrkCpeNameModel.setProduct(entry.getProduct());
				wrkCpeNameModel.setVendor(entry.getVendor());
				wrkCpeNameModel.setVersion(entry.getVersion());
				wrkCpeNameModel.setValidCpe(true);
				String[] wrkCPELiterals = entry.getCpeName().split(":");
				if (wrkCPELiterals.length > 6) {
					wrkCpeNameModel.setUpdate(wrkCPELiterals[6]);
				}
				likelyCpeNames.add(wrkCpeNameModel);
			}
			logger.debug("Total likely CPEs found: " + filteredCpes.size());
		}
		return likelyCpeNames;
	}
}
