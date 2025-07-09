package com.isteer.vms.core.engine.model;

import java.util.HashMap;
import java.util.Map;

import com.isteer.vms.core.engine.enums.CpeField;
import com.isteer.vms.core.engine.enums.ResolveMethod;

public class CpeName {
	private static final String CPE23_URI_FORMAT = "cpe:2.3:a:%s:%s:%s:%s:*:*:*:*:*:*";
	private String vendor;
	private String product;
	private String version;
	private String update="*";
	private boolean isValidCpe;
	private Map<CpeField,ResolveMethod> resolveMethod=new HashMap<>();

	public String getVendor() {
		return vendor;
	}



	public void setVendor(String vendor) {
		this.vendor = vendor;
	}



	public String getProduct() {
		return product;
	}



	public void setProduct(String product) {
		this.product = product;
	}



	public String getVersion() {
		return version;
	}



	public void setVersion(String version) {
		this.version = version;
	}



	public String getUpdate() {
		return update;
	}



	public void setUpdate(String update) {
		this.update = update;
	}


	public boolean isValidCpe() {
		return isValidCpe;
	}



	public void setValidCpe(boolean isValidCpe) {
		this.isValidCpe = isValidCpe;
	}


	public Map<CpeField, ResolveMethod> getResolveMethod() {
		return resolveMethod;
	}



	public void addResolveMethod(CpeField cpeField, ResolveMethod resolveMethod) {
		this.resolveMethod.put(cpeField, resolveMethod);
	}



	@Override
	public String toString() {
		return getCPE23Uri();
	}

	public String getCPE23Uri() {
			return String.format(CPE23_URI_FORMAT, this.vendor,this.product,this.version,this.update);
	}
}
