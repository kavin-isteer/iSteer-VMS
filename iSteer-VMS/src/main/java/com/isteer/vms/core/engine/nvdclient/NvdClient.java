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
import org.springframework.web.client.HttpStatusCodeException;
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
import com.jayway.jsonpath.PathNotFoundException;

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
	private static final String CPE_BASE_URL = "https://services.nvd.nist.gov/rest/json/cpes/2.0";
	@Value("${nvd.api.key}")
	private String apiKey;

	public CompletableFuture<BaseApplication> fetchVulnerabilitiesRateLimited(BaseApplication application) {
		return rateLimiter.submit(() -> fetchVulnerabilitiesForDependency(application));
	}

	/**
	 * Fetches and attaches vulnerability data for a given dependency based on its
	 * CPE enumeration.
	 *
	 * @param application The dependency model containing CPE info.
	 * @return The updated dependency model with vulnerabilities, if any.
	 */
	public BaseApplication fetchVulnerabilitiesForDependency(BaseApplication application) {
		log.info("Fetching vulnerabilities for application: {}", application.getApplication().getSoftwareName());
		String cveUrl = null;
		if (application == null || application.getCpeEnumeration() == null) {
			// No dependency or CPE info available; return as is or null
			log.warn("Application or CPE enumeration is null for application: {}",
					application.getApplication().getSoftwareName());
			return application;
		}

		CpeName cpeNameModel = application.getCpeEnumeration();
		String cpeName = cpeNameModel.getCPE23Uri();
		if (cpeName == null || cpeName.isEmpty()) {
			log.warn("CPE name is null or empty for application: {}", application.getApplication().getSoftwareName());
			return application;
		}

		Object cveApiResponse = null;

		try {
			String encodedCpe = URLEncoder.encode(cpeName, StandardCharsets.UTF_8.toString());
			cveUrl = String.format("%s?cpeName=%s", CVE_BASE_URL, encodedCpe);
			log.debug("Url for fetching CVE for application {}: {}", application.getApplication().getSoftwareName(),
					cveUrl);
			URI uri = new URI(cveUrl);
			ResponseEntity<Object> cveResponse = restTemplate.exchange(uri, HttpMethod.GET, getHeaders(), Object.class);
			if (cveResponse.getBody() != null && cveResponse.getStatusCode().is2xxSuccessful()) {
				log.debug("Got successful response from NVD API for application: {}",
						application.getApplication().getSoftwareName());
				cveApiResponse = cveResponse.getBody();
				if (cveApiResponse != null) {
					VulnerabilitiesForCpeName parsedVulnerability = new VulnerabilitiesForCpeName();
					parsedVulnerability.setVulnerabilities(parseCveApiResponse(cveApiResponse));
					for (Vulnerability vulnerabilityDetail : parsedVulnerability.getVulnerabilities()) {
						application.addVulnerabilities(vulnerabilityDetail);
					}
					application.getCpeEnumeration().setValidCpe(true);
				} else {
					log.warn("No vulnerabilities found for CPE: {}", cpeName);
				}
			}
		} catch (RestClientException e) {
			log.error("Error fetching CVE data from NVD API for CPE name: {}, with error message: {}", cpeName,
					e.getMessage());
			throw new NvdApiException(e.getMessage(), 502);
		} catch (UnsupportedEncodingException e) {
			log.error("Error encoding CPE name: {}, with error message: {}", cpeName, e.getMessage());
			throw new NvdApiException(e.getMessage(), 500);
		} catch (URISyntaxException e) {
			log.error("Error creating URI for CPE name: {}, with error message: {}", cpeName, e.getMessage());
			throw new NvdApiException(e.getMessage(), 400);
		}

		log.info("Completed fetching vulnerabilities for application: {}",
				application.getApplication().getSoftwareName());
		return application;
	}

	/**
	 * Fetches vulnerability details for a given CVE ID.
	 *
	 * @param cveId The CVE ID to search for (e.g., "CVE-2021-44228")
	 * @return Parsed CVE data for the given CVE ID.
	 * @throws NvdApiException if the API call fails or returns an error.
	 */
	public List<Vulnerability> getVulnerabilitiesByCveId(String cveId) throws NvdApiException {
		try {
			String url = String.format("%s?cveId=%s", CVE_BASE_URL, cveId);
			log.debug("Fetching CVE data for CVE ID: {} with URL: {}", cveId, url);
			ResponseEntity<Object> response = restTemplate.exchange(url, HttpMethod.GET, getHeaders(), Object.class);

			if (!response.getStatusCode().is2xxSuccessful()) {
				throw new NvdApiException("NVD API returned non-success status", response.getStatusCode().value());
			}
			log.info("Successfully fetched CVE data for ID: {}", cveId);
			return parseCveApiResponse(response.getBody());

		} catch (HttpStatusCodeException ex) {
			throw new NvdApiException(ex.getMessage(), ex.getStatusCode().value());
		} catch (Exception ex) {
			throw new NvdApiException(ex.getMessage(), 500);
		}
	}

	/**
	 * Searches for CVE vulnerabilities using a keyword (e.g., product name or
	 * version).
	 *
	 * @param keywords The search keyword(s).
	 * @return Parsed CVE data matching the keywords.
	 * @throws NvdApiException if the API call fails or returns an error.
	 */
	public List<Vulnerability> getVulnerabilitiesByKeywords(String keywords) throws NvdApiException {
		try {
			String url = String.format("%s?keywordSearch=%s", CVE_BASE_URL, keywords);
			log.debug("Fetching CVE data for keyword: {} with URL: {}", keywords, url);
			ResponseEntity<Object> response = restTemplate.exchange(url, HttpMethod.GET, getHeaders(), Object.class);

			if (!response.getStatusCode().is2xxSuccessful()) {
				throw new NvdApiException("NVD API returned non-success status", response.getStatusCode().value());
			}
			log.info("Successfully fetched CVE data for keyword: {}", keywords);
			return parseCveApiResponse(response.getBody());

		} catch (HttpStatusCodeException ex) {
			throw new NvdApiException(ex.getMessage(), ex.getStatusCode().value());
		} catch (Exception ex) {
			throw new NvdApiException(ex.getMessage(), 500);
		}
	}

	/**
	 * Retrieves vulnerabilities based on a specific CPE name.
	 *
	 * @param cpe The full CPE name (e.g., "cpe:2.3:a:apache:log4j:2.14.1").
	 * @return Parsed CVE data for the specified CPE.
	 * @throws NvdApiException if the API call fails or returns an error.
	 */
	public List<Vulnerability> getVulnerabilitiesByCpe(String cpe) throws NvdApiException {
		try {
			String encodedCpe = URLEncoder.encode(cpe, StandardCharsets.UTF_8.toString());
			String url = String.format("%s?cpeName=%s", CVE_BASE_URL, encodedCpe);
			log.debug("Fetching CVE data for CPE name: {} with URL: {}", cpe, url);
			URI uri = new URI(url);
			ResponseEntity<Object> response = restTemplate.exchange(uri, HttpMethod.GET, getHeaders(), Object.class);

			if (!response.getStatusCode().is2xxSuccessful()) {
				throw new NvdApiException("NVD API returned non-success status", response.getStatusCode().value());
			}
			log.info("Successfully fetched CVE data for CPE: {}", cpe);
			return parseCveApiResponse(response.getBody());

		} catch (HttpStatusCodeException ex) {
			throw new NvdApiException(ex.getMessage(), ex.getStatusCode().value());
		} catch (Exception ex) {
			throw new NvdApiException(ex.getMessage(), 500);
		}
	}

	/**
	 * Retrieves a list of CPE names that match the given string.
	 *
	 * @param cpeName A partial or full CPE name string.
	 * @return Parsed list of matching CPE names.
	 * @throws NvdApiException if the API call fails or returns an error.
	 */
	public List<CpeName> getCpeNameList(String cpeName) throws NvdApiException {
		try {
			String encodedCpe = URLEncoder.encode(cpeName, StandardCharsets.UTF_8.toString());
			String url = String.format("%s?cpeMatchString=%s", CPE_BASE_URL, encodedCpe);
			log.debug("Fetching CPE names for match string: {} with URL: {}", cpeName, url);
			URI uri = new URI(url);
			ResponseEntity<Object> response = restTemplate.exchange(uri, HttpMethod.GET, getHeaders(), Object.class);

			if (!response.getStatusCode().is2xxSuccessful()) {
				throw new NvdApiException("NVD API returned non-success status", response.getStatusCode().value());
			}
			log.info("Successfully fetched CPE names for match string: {}", cpeName);
			return parseCpeApiResponse(response.getBody());

		} catch (HttpStatusCodeException ex) {
			throw new NvdApiException(ex.getMessage(), ex.getStatusCode().value());
		} catch (Exception ex) {
			throw new NvdApiException(ex.getMessage(), 500);
		}
	}

	/**
	 * Retrieves a list of CPE names based on a keyword search.
	 *
	 * @param keywords The keyword to search CPEs (e.g., "log4j").
	 * @return Parsed list of matching CPE names.
	 * @throws NvdApiException if the API call fails or returns an error.
	 */
	public List<CpeName> getCpeNameListByKeywords(String keywords) throws NvdApiException {
		try {
			String url = String.format("%s?keywordSearch=%s", CPE_BASE_URL, keywords);
			log.debug("Fetching CPE names for keyword: {} with URL: {}", keywords, url);
			ResponseEntity<Object> response = restTemplate.exchange(url, HttpMethod.GET, getHeaders(), Object.class);

			if (!response.getStatusCode().is2xxSuccessful()) {
				throw new NvdApiException("NVD API returned non-success status", response.getStatusCode().value());
			}
			log.info("Successfully  fetched CPE names for keyword: {}", keywords);
			return parseCpeApiResponse(response.getBody());

		} catch (HttpStatusCodeException ex) {
			throw new NvdApiException(ex.getMessage(), ex.getStatusCode().value());
		} catch (Exception ex) {
			throw new NvdApiException(ex.getMessage(), 500);
		}
	}

	/**
	 * Parses the CPE API response and converts it into a list of
	 * {@link CPENameModel}.
	 *
	 * @param cpeApiResponse The raw JSON response from the NVD CPE API.
	 * @return A list of CPE name models.
	 */
	public List<CpeName> parseCpeApiResponse(Object cpeApiResponse) {
		log.debug("Parsing CPE API response to extract CPE names");
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
		} catch (PathNotFoundException e) {
			log.warn("{}", e.getMessage());
		} catch (Exception e) {
			log.error("Error parsing CPE API response: {}", e.getMessage());
		}
		log.debug("Parsed {} CPE names from API response.", cpeNames.size());
		return cpeNames;
	}

	/**
	 * Parses the CVE API response into a list of {@link VulnerabilityDetailsModel}.
	 *
	 * @param cveApiResponse The raw JSON response from the NVD CVE API.
	 * @return A list of vulnerability detail models.
	 */
	public List<Vulnerability> parseCveApiResponse(Object cveApiResponse) {
		log.debug("Parsing CVE API response to extract vulnerability details...");
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
		} catch (PathNotFoundException e) {
			log.warn("{}", e.getMessage());
		} catch (Exception e) {
			log.error("Error parsing CVE API response: {}, {}", e.getMessage(), cveApiResponse);
		}
		log.debug("Parsed {} vulnerability details from API response.", vulnerabilityDetails.size());
		return vulnerabilityDetails;
	}

	/**
	 * Processes CVSS metrics of a vulnerability from the parsed metrics map.
	 *
	 * @param metricMap The metrics section from the CVE API.
	 * @return A list of parsed CVSS metric models.
	 */
	private List<VulnerabilityCvssMetrics> processCvssMetrics(Map<String, Object> metricMap) {
		log.debug("Processing CVSS metrics from the CVE API response...");
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
									cvssMetricModel
											.setAttackVector(JsonPath.read(cvssMetric, "$.cvssData.accessVector"));
									cvssMetricModel.setAttackComplexity(
											JsonPath.read(cvssMetric, "$.cvssData.accessComplexity"));
									cvssMetricModel.setPrivilegesRequired(
											JsonPath.read(cvssMetric, "$.cvssData.authentication"));
								} else {
									cvssMetricModel
											.setBaseSeverity(JsonPath.read(cvssMetric, "$.cvssData.baseSeverity"));
									cvssMetricModel
											.setAttackVector(JsonPath.read(cvssMetric, "$.cvssData.attackVector"));
									cvssMetricModel.setAttackComplexity(
											JsonPath.read(cvssMetric, "$.cvssData.attackComplexity"));
									cvssMetricModel.setPrivilegesRequired(
											JsonPath.read(cvssMetric, "$.cvssData.privilegesRequired"));
									cvssMetricModel.setUserInteraction(
											JsonPath.read(cvssMetric, "$.cvssData.userInteraction"));
									cvssMetricModel.setScope(JsonPath.read(cvssMetric, "$.cvssData.scope"));
								}

								cvssMetricModel.setConfidentiality(
										JsonPath.read(cvssMetric, "$.cvssData.confidentialityImpact"));
								cvssMetricModel.setIntegrity(JsonPath.read(cvssMetric, "$.cvssData.integrityImpact"));
								cvssMetricModel
										.setAvailability(JsonPath.read(cvssMetric, "$.cvssData.availabilityImpact"));
								cvssMetricModel
										.setExploitabilityScore(JsonPath.read(cvssMetric, "$.exploitabilityScore"));
								cvssMetricModel.setImpactScore(JsonPath.read(cvssMetric, "$.impactScore"));
								parsedCvssMetrics.add(cvssMetricModel);
							} else {
								VulnerabilityCvssMetrics cvssMetricModel = new VulnerabilityCvssMetrics();
								cvssMetricModel.setVersion(JsonPath.read(cvssMetric, "$.cvssData.version"));
								cvssMetricModel.setBaseScore(JsonPath.read(cvssMetric, "$.cvssData.baseScore"));
								cvssMetricModel.setVectorString(JsonPath.read(cvssMetric, "$.cvssData.vectorString"));
								if ("cvssMetricV2".equals(metricType)) {
									cvssMetricModel.setBaseSeverity(JsonPath.read(cvssMetric, "$.baseSeverity"));
									cvssMetricModel
											.setAttackVector(JsonPath.read(cvssMetric, "$.cvssData.accessVector"));
									cvssMetricModel.setAttackComplexity(
											JsonPath.read(cvssMetric, "$.cvssData.accessComplexity"));
									cvssMetricModel.setPrivilegesRequired(
											JsonPath.read(cvssMetric, "$.cvssData.authentication"));
								} else {
									cvssMetricModel
											.setBaseSeverity(JsonPath.read(cvssMetric, "$.cvssData.baseSeverity"));
									cvssMetricModel
											.setAttackVector(JsonPath.read(cvssMetric, "$.cvssData.attackVector"));
									cvssMetricModel.setAttackComplexity(
											JsonPath.read(cvssMetric, "$.cvssData.attackComplexity"));
									cvssMetricModel.setPrivilegesRequired(
											JsonPath.read(cvssMetric, "$.cvssData.privilegesRequired"));
									cvssMetricModel.setUserInteraction(
											JsonPath.read(cvssMetric, "$.cvssData.userInteraction"));
									cvssMetricModel.setScope(JsonPath.read(cvssMetric, "$.cvssData.scope"));
								}
								cvssMetricModel.setConfidentiality(
										JsonPath.read(cvssMetric, "$.cvssData.confidentialityImpact"));
								cvssMetricModel.setIntegrity(JsonPath.read(cvssMetric, "$.cvssData.integrityImpact"));
								cvssMetricModel
										.setAvailability(JsonPath.read(cvssMetric, "$.cvssData.availabilityImpact"));
								cvssMetricModel
										.setExploitabilityScore(JsonPath.read(cvssMetric, "$.exploitabilityScore"));
								cvssMetricModel.setImpactScore(JsonPath.read(cvssMetric, "$.impactScore"));
								parsedCvssMetrics.add(cvssMetricModel);
							}
						}
					}
				}
			}
		} catch (PathNotFoundException e) {
			log.warn("{}", e.getMessage());
		} catch (Exception e) {
			log.error("Error processing CVSS metrics: {}, {}", e.getMessage(), metricMap);
		}
		log.debug("Processed {} CVSS metrics from API response.", parsedCvssMetrics.size());
		return parsedCvssMetrics;
	}

	/**
	 * Processes affected product configurations from a list of CPE match entries.
	 *
	 * @param configurations List of CPE match JSON entries.
	 * @return List of affected product models.
	 */
	public List<VulnerabilityAffectedProduct> processAffectedProducts(List<Object> configurations) {
		log.debug("Processing affected products from CVE API response...");
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
		} catch (PathNotFoundException e) {
			log.warn("{}", e.getMessage());
		} catch (Exception e) {
			log.error("Error processing affected products: {}", e.getMessage());
		}
		log.debug("Processed {} affected products from API response.", affectedProducts.size());
		return affectedProducts;
	}

	/**
	 * Processes reference URLs and tags related to a CVE vulnerability.
	 *
	 * @param references List of reference JSON objects.
	 * @return List of parsed reference models.
	 */
	private List<VulnerabilityReference> processReferences(List<Object> references) {
		log.debug("Processing references from CVE API response...");
		List<VulnerabilityReference> mitigationReferences = new ArrayList<>();
		try {
			if (references != null) {
				for (Object reference : references) {
					VulnerabilityReference mitigationReference = new VulnerabilityReference();
					mitigationReference.setReferenceUrl(JsonPath.read(reference, "$.url"));
					List<String> tags = JsonPath.read(reference, "$.tags[*]");
					mitigationReference.setReferenceTags(tags);
					mitigationReferences.add(mitigationReference);
				}
			}
		} catch (PathNotFoundException e) {
			log.warn("{}", e.getMessage());
		} catch (Exception e) {
			log.error("Error processing references: {} for CVE ID: {}", e.getMessage());
		}
		log.debug("Processed {} references from API response.", mitigationReferences.size());
		return mitigationReferences;
	}

	private HttpEntity<String> getHeaders() {
		HttpHeaders headers = new HttpHeaders();
		headers.set("apiKey", apiKey);
		return new HttpEntity<>(headers);
	}

}
