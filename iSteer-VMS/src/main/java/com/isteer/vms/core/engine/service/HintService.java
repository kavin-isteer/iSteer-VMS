package com.isteer.vms.core.engine.service;

import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

import com.isteer.vms.core.engine.dao.CpeHintDao;
import com.isteer.vms.core.engine.enums.EvidenceType;
import com.isteer.vms.core.engine.enums.HintAddedBy;
import com.isteer.vms.core.engine.model.CpeHint;
import com.isteer.vms.model.Application;

@Service
public class HintService {
	private CpeHintDao hintDao;

	
	public HintService(CpeHintDao hintDao) {
		this.hintDao = hintDao;
	}

	public int addApplicationHint(String cpeName, Application application) {
		if (!isValidCPE(cpeName)) { 
			return -1;
		}
		
		CpeHint hintToSave = new CpeHint();
		String[] cpeNameStripped = cpeName.split(":");
		String vendor = cpeNameStripped[3];
		String product = cpeNameStripped[4];
		
		String productName = application.getSoftwareName();
		String vendorName = application.getVendorName();
		
		hintToSave.setType("vendor");
		hintToSave.setMatchKey(vendorName);
		hintToSave.setStandardizedName(vendor);
		hintToSave.setConfidence("HIGH");
		hintToSave.setDescription("Hint added through add hint api!");
		hintToSave.setEvidenceType(EvidenceType.APPLICATION.toString());
		hintToSave.setAddedBy(HintAddedBy.CLIENT_USER);
		
		int rows = hintDao.addDependencyHint(hintToSave);
		if(rows>0) {
			hintToSave.setType("product");
			hintToSave.setMatchKey(productName);
			hintToSave.setStandardizedName(product);
			hintToSave.setConfidence("HIGH");
			hintToSave.setDescription("Hint added through add hint api!");
			hintToSave.setEvidenceType(EvidenceType.APPLICATION.toString());
			hintToSave.setAddedBy(HintAddedBy.CLIENT_USER);
			rows = hintDao.addDependencyHint(hintToSave);
			if(rows>0) {
				return 1;
			}else {
				return -2;
			}
		}else {
			return -3;
		}
	}
	// Regular expression to match CPE 2.3 format
	private static final Pattern CPE_2_3_PATTERN = Pattern.compile("^cpe:2\\.3:[aho](:[^:]*){10}$");

	/**
	 * Validates if the given string is a valid CPE 2.3 formatted name.
	 *
	 * @param cpe the CPE name string to validate
	 * @return true if the string is a valid CPE 2.3 name; false otherwise
	 */
	public static boolean isValidCPE(String cpe) {
		if (cpe == null) {
			return false;
		}
		return CPE_2_3_PATTERN.matcher(cpe).matches();
	}
	
}
