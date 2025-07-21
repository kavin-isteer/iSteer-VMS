package com.isteer.vms.core.engine.nvdclient;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.isteer.vms.core.engine.model.BaseApplication;
import com.isteer.vms.core.engine.model.CpeName;
import com.isteer.vms.core.engine.model.VulnerabilitiesForCpeName;
import com.isteer.vms.core.engine.model.Vulnerability;
import com.isteer.vms.core.engine.model.VulnerabilityAffectedProduct;
import com.isteer.vms.core.engine.model.VulnerabilityCvssMetrics;
import com.isteer.vms.core.engine.model.VulnerabilityReference;
import com.isteer.vms.exception.NvdApiException;
import com.jayway.jsonpath.JsonPath;

import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
public class NvdClient {

	private RestTemplate restTemplate;
	private RateLimiter rateLimiter;
	
	
	public NvdClient(RestTemplate restTemplate, RateLimiter rateLimiter) {
		super();
		this.restTemplate = restTemplate;
		this.rateLimiter = rateLimiter;
	}

	private static final String CVE_BASE_URL = "https://services.nvd.nist.gov/rest/json/cves/2.0";
	@Value("${nvd.api.key}")
	private String apiKey;
	
	 public CompletableFuture<BaseApplication> fetchVulnerabilitiesRateLimited(BaseApplication application) {
	        return rateLimiter.submit(() -> fetchVulnerabilitiesForDependency(application));
	    }
	
	/**
     * Fetches and attaches vulnerability data for a given dependency based on its CPE enumeration.
     *
     * @param application The dependency model containing CPE info.
     * @return The updated dependency model with vulnerabilities, if any.
     */
	public BaseApplication fetchVulnerabilitiesForDependency(BaseApplication application) {
		String cveUrl = null;
		if (application == null || application.getCpeEnumeration() == null) {
			// No dependency or CPE info available; return as is or null
			return application;
		}

		CpeName cpeNameModel = application.getCpeEnumeration();
		String cpeName = cpeNameModel.getCPE23Uri();
		if (cpeName == null || cpeName.isEmpty()) {
			return application;
		}

		Object cveApiResponse = null;

		HttpHeaders headers = new HttpHeaders();
		headers.set("apiKey", apiKey);
		HttpEntity<String> entity = new HttpEntity<>(headers);

		try {
			String encodedCpe = URLEncoder.encode(cpeName, StandardCharsets.UTF_8.toString());
			cveUrl = String.format("%s?cpeName=%s", CVE_BASE_URL, encodedCpe);
			log.info("Url for fetching CVE: {}", cveUrl);
			URI uri = new URI(cveUrl);
			ResponseEntity<Object> cveResponse = restTemplate.exchange(uri, HttpMethod.GET, entity, Object.class);
			if (cveResponse.getBody() != null && cveResponse.getStatusCode().is2xxSuccessful()) {
				cveApiResponse = cveResponse.getBody();
				if (cveApiResponse != null) {
					VulnerabilitiesForCpeName parsedVulnerability = new VulnerabilitiesForCpeName();
					parsedVulnerability.setVulnerabilities(parseCveApiResponse(cveApiResponse));
					for (Vulnerability vulnerabilityDetail : parsedVulnerability.getVulnerabilities()) {
						application.addVulnerabilities(vulnerabilityDetail);
					}
					application.getCpeEnumeration().setValidCpe(true);
				} else {
					log.error("Error for this url: {}.", cveUrl);
					throw new NvdApiException("CVE API failed", cveResponse.getStatusCode().value());
				}
			}
		} catch (RestClientException e) {
			log.error("Error for this url: {}. Application: {}", cveUrl, application.getApplication());
			throw new NvdApiException("CVE API error: " + e.getMessage(), 500);
		} catch (UnsupportedEncodingException e) {
			throw new NvdApiException("CVE Encoding error: " + e.getMessage(), 400);
		} catch (URISyntaxException e) {
			throw new NvdApiException("CVE URI error: " + e.getMessage(), 400);
		}

		log.info("Fetched vulnerabilities for CPE: {}", application);
		return application;
	}
	
	/**
     * Parses the CPE API response and converts it into a list of {@link CPENameModel}.
     *
     * @param cpeApiResponse The raw JSON response from the NVD CPE API.
     * @return A list of CPE name models.
     */
	public List<CpeName> parseCpeApiResponse(Object cpeApiResponse) {
		List<CpeName> cpeNames = new ArrayList<>();
		ObjectMapper objectMapper = new ObjectMapper();

		try {
			String jsonResponse = objectMapper.writeValueAsString(cpeApiResponse);
			List<Object> cpeItems = JsonPath.read(jsonResponse, "$.products[*].cpe");
			if (cpeItems != null && !cpeItems.isEmpty()) {
				for (Object cpeItem : cpeItems) {
					CpeName cpeName = new CpeName();
					String cpeNameStr = JsonPath.read(cpeItem, "$.cpeName").toString();
					if (cpeNameStr != null && !cpeNameStr.isEmpty()) {
						String[] cpeNameUri = cpeNameStr.split(":");
						if (cpeNameUri.length >= 7) {
							cpeName.setVendor(cpeNameUri[3]);
							cpeName.setProduct(cpeNameUri[4]);
							cpeName.setVersion(cpeNameUri[5]);
							cpeName.setUpdate(cpeNameUri[6]);
							cpeName.setValidCpe(true);
							cpeNames.add(cpeName);
						}
					}
				}
			}
		} catch (Exception e) {
			log.error("Error parsing CPE API response: " + e.getMessage());
		}
		return cpeNames;
	}
	
	/**
     * Parses the CVE API response into a list of {@link VulnerabilityDetailsModel}.
     *
     * @param cveApiResponse The raw JSON response from the NVD CVE API.
     * @return A list of vulnerability detail models.
     */
	public List<Vulnerability> parseCveApiResponse(Object cveApiResponse) {
		List<Vulnerability> vulnerabilityDetails = new ArrayList<>();
		ObjectMapper objectMapper = new ObjectMapper();

		try {
			String jsonResponse = objectMapper.writeValueAsString(cveApiResponse);
			List<Object> cveItems = JsonPath.read(jsonResponse, "$.vulnerabilities[*]");

			if (cveItems != null && !cveItems.isEmpty()) {
				for (Object cveItem : cveItems) {
					Vulnerability vulnerabilityDetail = new Vulnerability();
					vulnerabilityDetail.setCveId(JsonPath.read(cveItem, "$.cve.id"));
					vulnerabilityDetail.setCveDescription(JsonPath.read(cveItem, "$.cve.descriptions[0].value"));
					vulnerabilityDetail.setSourceIdentifier(JsonPath.read(cveItem, "$.cve.sourceIdentifier"));

					Map<String, Object> cvssMetricsMap = JsonPath.read(cveItem, "$.cve.metrics");
					if (cvssMetricsMap != null) {
						vulnerabilityDetail.setCvssMetrics(processCvssMetrics(cvssMetricsMap));
					}

					List<Object> affectedProducts = JsonPath.read(cveItem, "$.cve.configurations[*].nodes[*].cpeMatch");
					if (affectedProducts != null) {
						vulnerabilityDetail.setAffectedProducts(processAffectedProducts(affectedProducts));
					}

					List<Object> references = JsonPath.read(cveItem, "$.cve.references[*]");
					if (references != null) {
						vulnerabilityDetail.setReferences(processReferences(references));
					}

					vulnerabilityDetails.add(vulnerabilityDetail);
				}
			}
		} catch (Exception e) {
			log.error("Error parsing CVE API response: " + e.getMessage());
		}
		return vulnerabilityDetails;
	}
	
	/**
     * Processes CVSS metrics of a vulnerability from the parsed metrics map.
     *
     * @param metricMap The metrics section from the CVE API.
     * @return A list of parsed CVSS metric models.
     */
	private List<VulnerabilityCvssMetrics> processCvssMetrics(Map<String, Object> metricMap) {
		String[] metricTypes = { "cvssMetricV31", "cvssMetricV4", "cvssMetricV2" };
		List<VulnerabilityCvssMetrics> parsedCvssMetrics = new ArrayList<>();
		try {
			for (String metricType : metricTypes) {
				if (metricMap != null && metricMap.containsKey(metricType)) {
					List<Object> cvssMetrics = (List<Object>) metricMap.get(metricType);
					if (cvssMetrics != null) {
						for (Object cvssMetric : cvssMetrics) {
							String cvssMetricType = JsonPath.read(cvssMetric, "$.type");
							if ("Primary".equals(cvssMetricType)) {
								VulnerabilityCvssMetrics cvssMetricModel = new VulnerabilityCvssMetrics();
								cvssMetricModel.setVersion(JsonPath.read(cvssMetric, "$.cvssData.version"));
								cvssMetricModel.setBaseScore(JsonPath.read(cvssMetric, "$.cvssData.baseScore"));
								cvssMetricModel.setVectorString(JsonPath.read(cvssMetric, "$.cvssData.vectorString"));

								if ("cvssMetricV2".equals(metricType)) {
									cvssMetricModel.setBaseSeverity(JsonPath.read(cvssMetric, "$.baseSeverity"));
									cvssMetricModel.setAttackVector(JsonPath.read(cvssMetric, "$.cvssData.accessVector"));
									cvssMetricModel.setAttackComplexity(JsonPath.read(cvssMetric, "$.cvssData.accessComplexity"));
									cvssMetricModel.setPrivilegesRequired(JsonPath.read(cvssMetric, "$.cvssData.authentication"));
								} else {
									cvssMetricModel.setBaseSeverity(JsonPath.read(cvssMetric, "$.cvssData.baseSeverity"));
									cvssMetricModel.setAttackVector(JsonPath.read(cvssMetric, "$.cvssData.attackVector"));
									cvssMetricModel.setAttackComplexity(JsonPath.read(cvssMetric, "$.cvssData.attackComplexity"));
									cvssMetricModel.setPrivilegesRequired(JsonPath.read(cvssMetric, "$.cvssData.privilegesRequired"));
									cvssMetricModel.setUserInteraction(JsonPath.read(cvssMetric, "$.cvssData.userInteraction"));
									cvssMetricModel.setScope(JsonPath.read(cvssMetric, "$.cvssData.scope"));
								}

								cvssMetricModel.setConfidentiality(JsonPath.read(cvssMetric, "$.cvssData.confidentialityImpact"));
								cvssMetricModel.setIntegrity(JsonPath.read(cvssMetric, "$.cvssData.integrityImpact"));
								cvssMetricModel.setAvailability(JsonPath.read(cvssMetric, "$.cvssData.availabilityImpact"));
								cvssMetricModel.setExploitabilityScore(JsonPath.read(cvssMetric, "$.exploitabilityScore"));
								cvssMetricModel.setImpactScore(JsonPath.read(cvssMetric, "$.impactScore"));
								parsedCvssMetrics.add(cvssMetricModel);
							} else {
								VulnerabilityCvssMetrics cvssMetricModel = new VulnerabilityCvssMetrics();
								cvssMetricModel.setVersion(JsonPath.read(cvssMetric, "$.cvssData.version"));
								cvssMetricModel.setBaseScore(JsonPath.read(cvssMetric, "$.cvssData.baseScore"));
								cvssMetricModel.setVectorString(JsonPath.read(cvssMetric, "$.cvssData.vectorString"));
								cvssMetricModel.setBaseSeverity(JsonPath.read(cvssMetric, "$.cvssData.baseSeverity"));
								cvssMetricModel.setAttackVector(JsonPath.read(cvssMetric, "$.cvssData.attackVector"));
								cvssMetricModel.setAttackComplexity(JsonPath.read(cvssMetric, "$.cvssData.attackComplexity"));
								cvssMetricModel.setPrivilegesRequired(JsonPath.read(cvssMetric, "$.cvssData.privilegesRequired"));
								cvssMetricModel.setUserInteraction(JsonPath.read(cvssMetric, "$.cvssData.userInteraction"));
								cvssMetricModel.setScope(JsonPath.read(cvssMetric, "$.cvssData.scope"));
								cvssMetricModel.setConfidentiality(JsonPath.read(cvssMetric, "$.cvssData.confidentialityImpact"));
								cvssMetricModel.setIntegrity(JsonPath.read(cvssMetric, "$.cvssData.integrityImpact"));
								cvssMetricModel.setAvailability(JsonPath.read(cvssMetric, "$.cvssData.availabilityImpact"));
								cvssMetricModel.setExploitabilityScore(JsonPath.read(cvssMetric, "$.exploitabilityScore"));
								cvssMetricModel.setImpactScore(JsonPath.read(cvssMetric, "$.impactScore"));
								parsedCvssMetrics.add(cvssMetricModel);
							}
						}
					}
				}
			}
		} catch (Exception e) {
			log.error("Error processing CVSS metrics: " + e.getMessage());
		}
		return parsedCvssMetrics;
	}
	
	/**
     * Processes affected product configurations from a list of CPE match entries.
     *
     * @param configurations List of CPE match JSON entries.
     * @return List of affected product models.
     */
	public List<VulnerabilityAffectedProduct> processAffectedProducts(List<Object> configurations) {
		List<VulnerabilityAffectedProduct> affectedProducts = new ArrayList<>();

		try {
			if (configurations != null) {
				for (Object cpeMatchList : configurations) {
					if (cpeMatchList instanceof List) {
						List<?> cpeMatches = (List<?>) cpeMatchList;
						for (Object cpeMatch : cpeMatches) {
							VulnerabilityAffectedProduct product = new VulnerabilityAffectedProduct();
							product.setCpeName(JsonPath.read(cpeMatch, "$.criteria"));
							String cpeMatchStr = JsonPath.read(cpeMatch, "$").toString();

							if (cpeMatchStr.contains("versionStartIncluding")) {
								product.setVersionStartIncluding(JsonPath.read(cpeMatch, "$.versionStartIncluding"));
							}
							if (cpeMatchStr.contains("versionStartExcluding")) {
								product.setVersionStartExcluding(JsonPath.read(cpeMatch, "$.versionStartExcluding"));
							}
							if (cpeMatchStr.contains("versionEndIncluding")) {
								product.setVersionEndIncluding(JsonPath.read(cpeMatch, "$.versionEndIncluding"));
							}
							if (cpeMatchStr.contains("versionEndExcluding")) {
								product.setVersionEndExcluding(JsonPath.read(cpeMatch, "$.versionEndExcluding"));
							}

							affectedProducts.add(product);
						}
					}
				}
			}
		} catch (Exception e) {
			log.error("Error processing affected products: " + e.getMessage());
		}

		return affectedProducts;
	}
	
	 /**
     * Processes reference URLs and tags related to a CVE vulnerability.
     *
     * @param references List of reference JSON objects.
     * @return List of parsed reference models.
     */
	private List<VulnerabilityReference> processReferences(List<Object> references) {
		List<VulnerabilityReference> mitigationReferences = new ArrayList<>();
		try {
			if (references != null) {
				for (Object reference : references) {
					VulnerabilityReference mitigationReference = new VulnerabilityReference();
					mitigationReference.setReferenceUrl(JsonPath.read(reference, "$.url"));
					String referenceStr = JsonPath.read(reference, "$").toString();
					if (referenceStr.contains("tags")) {
						mitigationReference.setReferenceTags(JsonPath.read(reference, "$.tags[*]"));
					}
					mitigationReferences.add(mitigationReference);
				}
			}
		} catch (Exception e) {
			log.error("Error processing references: " + e.getMessage());
		}

		return mitigationReferences;
	}
}
