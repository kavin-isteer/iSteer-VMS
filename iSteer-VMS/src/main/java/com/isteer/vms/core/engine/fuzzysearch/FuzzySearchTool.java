package com.isteer.vms.core.engine.fuzzysearch;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.isteer.vms.core.engine.lucene.LuceneCpeSearcher;
import com.isteer.vms.core.engine.model.CpeEntry;
import com.isteer.vms.core.engine.model.CpeName;

import lombok.extern.log4j.Log4j2;

@Log4j2
@Service
public class FuzzySearchTool {
	LuceneCpeSearcher luceneSearcher;

	public FuzzySearchTool(LuceneCpeSearcher luceneSearcher) {
		this.luceneSearcher = luceneSearcher;
	}

	public List<CpeName> searchForLikelyCpeName(String vendorSearchString, String productSearchString,
			String versionSearchString) {
		List<CpeEntry> filteredCpes = new ArrayList<>();
		List<CpeName> likelyCpeNames = new ArrayList<>();
		try {
			filteredCpes = luceneSearcher.multiFieldSearch(vendorSearchString, productSearchString,
					versionSearchString);
		} catch (Exception e) {
			log.debug("Exception occured while searching for likely cpes with error message: {}", e.getMessage());
			return List.of();
		}
		if (!filteredCpes.isEmpty()) {
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
			log.debug("Total likely CPEs found: " + filteredCpes.size());
		}
		return likelyCpeNames;
	}
}
