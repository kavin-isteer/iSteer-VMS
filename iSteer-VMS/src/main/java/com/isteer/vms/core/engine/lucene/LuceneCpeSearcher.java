package com.isteer.vms.core.engine.lucene;

import java.io.IOException;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.springframework.stereotype.Service;

import com.isteer.vms.core.engine.model.CpeEntry;

@Service
public class LuceneCpeSearcher {

    private static final Logger logger = LogManager.getLogger(LuceneCpeSearcher.class);
    private static final String INDEX_DIR = "lucene-index";
    private int EDIT_DISTANCE = 10;
    private int TOP_MATCHES_THRESHOLD = 10;

    public LuceneCpeSearcher withEditDistance(int editDistance) {
        this.EDIT_DISTANCE = editDistance;
        return this;
    }

    public LuceneCpeSearcher withTopMatchesThreshold(int matchThreshold) {
        if (matchThreshold > 0) {
            this.TOP_MATCHES_THRESHOLD = matchThreshold;
        }
        return this;
    }

    /**
     * Performs a fuzzy search on a given field.
     *
     * @param field   Lucene field to search in
     * @param keyword Keyword to search for
     * @return List of matching CpeEntry
     */
    public List<CpeEntry> fuzzySearch(String field, String keyword) {
        List<CpeEntry> results = new ArrayList<>();
        logger.info("Performing fuzzy search on field '{}' for keyword '{}'", field, keyword);

        try (Directory dir = FSDirectory.open(Paths.get(INDEX_DIR));
             DirectoryReader reader = DirectoryReader.open(dir)) {

            IndexSearcher searcher = new IndexSearcher(reader);
            Analyzer analyzer = new StandardAnalyzer();

            QueryParser parser = new QueryParser(field, analyzer);
            Query query = parser.parse(QueryParser.escape(keyword) + "~" + this.EDIT_DISTANCE);

            TopDocs topDocs = searcher.search(query, TOP_MATCHES_THRESHOLD);

            for (ScoreDoc sd : topDocs.scoreDocs) {
                results.add(mapDocumentToCpeEntry(searcher.doc(sd.doc)));
            }

        } catch (Exception e) {
            logger.error("Error during fuzzySearch: {}", e.getMessage(), e);
        }

        return results;
    }

    /**
     * Performs a multi-field search for vendor and product.
     */
    public List<CpeEntry> multiFieldSearch(String vendorValue, String productValue) {
        logger.info("Searching for vendor='{}', product='{}'", vendorValue, productValue);
        List<CpeEntry> results = new ArrayList<>();

        try (Directory dir = FSDirectory.open(Paths.get(INDEX_DIR));
             DirectoryReader reader = DirectoryReader.open(dir)) {

            IndexSearcher searcher = new IndexSearcher(reader);
            Analyzer analyzer = new StandardAnalyzer();

            BooleanQuery.Builder builder = new BooleanQuery.Builder();
            builder.add(new QueryParser("vendor", analyzer).parse(QueryParser.escape(vendorValue)), BooleanClause.Occur.MUST);
            builder.add(new QueryParser("product", analyzer).parse(QueryParser.escape(productValue)), BooleanClause.Occur.MUST);

            TopDocs topDocs = searcher.search(builder.build(), TOP_MATCHES_THRESHOLD);
            for (ScoreDoc sd : topDocs.scoreDocs) {
                results.add(mapDocumentToCpeEntry(searcher.doc(sd.doc)));
            }

        } catch (Exception e) {
            logger.error("Error during multiFieldSearch(vendor, product): {}", e.getMessage(), e);
        }

        return results;
    }

    /**
     * Performs a multi-field search for a list of vendors and a product.
     */
    public List<CpeEntry> multiFieldSearch(List<String> vendorList, String productValue) {
        List<CpeEntry> results = new ArrayList<>();
        logger.info("Searching for vendors='{}', product='{}'", vendorList, productValue);

        try (Directory dir = FSDirectory.open(Paths.get(INDEX_DIR));
             DirectoryReader reader = DirectoryReader.open(dir)) {

            IndexSearcher searcher = new IndexSearcher(reader);
            Analyzer analyzer = new StandardAnalyzer();

            BooleanQuery.Builder vendorQuery = new BooleanQuery.Builder();
            for (String vendor : vendorList) {
                vendorQuery.add(new QueryParser("vendor", analyzer).parse(QueryParser.escape(vendor)), BooleanClause.Occur.SHOULD);
            }

            Query productQuery = new QueryParser("product", analyzer).parse(QueryParser.escape(productValue));

            BooleanQuery finalQuery = new BooleanQuery.Builder()
                    .add(vendorQuery.build(), BooleanClause.Occur.MUST)
                    .add(productQuery, BooleanClause.Occur.MUST)
                    .build();

            TopDocs topDocs = searcher.search(finalQuery, TOP_MATCHES_THRESHOLD);
            for (ScoreDoc sd : topDocs.scoreDocs) {
                results.add(mapDocumentToCpeEntry(searcher.doc(sd.doc)));
            }

        } catch (Exception e) {
            logger.error("Error during multiFieldSearch(List<String>, String): {}", e.getMessage(), e);
        }

        return results;
    }

    /**
     * Searches with multiple vendors, products and versions.
     */
    public List<CpeEntry> multiFieldSearch(List<String> vendorList, List<String> productList, List<String> versionList) {
        List<CpeEntry> results = new ArrayList<>();
        logger.info("Searching for vendors={}, products={}, versions={}", vendorList, productList, versionList);

        try (Directory dir = FSDirectory.open(Paths.get(INDEX_DIR));
             DirectoryReader reader = DirectoryReader.open(dir)) {

            IndexSearcher searcher = new IndexSearcher(reader);
            Analyzer analyzer = new StandardAnalyzer();
            BooleanQuery.Builder queryBuilder = new BooleanQuery.Builder();

            addTermsToQuery("vendor", vendorList, analyzer, queryBuilder);
            addTermsToQuery("product", productList, analyzer, queryBuilder);
            addTermsToQuery("version", versionList, analyzer, queryBuilder);

            Query query = queryBuilder.build();

            TopDocs topDocs = searcher.search(query, TOP_MATCHES_THRESHOLD);
            for (ScoreDoc sd : topDocs.scoreDocs) {
                results.add(mapDocumentToCpeEntry(searcher.doc(sd.doc)));
            }

        } catch (Exception e) {
            logger.error("Error in multiFieldSearch(List, List, List): {}", e.getMessage(), e);
        }

        return results;
    }

    /**
     * Search using normalized token-based search (vendor, product, version).
     */
    public List<CpeEntry> multiFieldSearch(String vendorValue, String productValue, String versionValue) {
        List<CpeEntry> results = new ArrayList<>();
        logger.info("Token-based search for vendor='{}', product='{}', version='{}'", vendorValue, productValue, versionValue);

        try (Directory dir = FSDirectory.open(Paths.get(INDEX_DIR));
             DirectoryReader reader = DirectoryReader.open(dir)) {

            IndexSearcher searcher = new IndexSearcher(reader);
            Analyzer analyzer = new StandardAnalyzer();
            BooleanQuery.Builder finalQueryBuilder = new BooleanQuery.Builder();

            addAnalyzedFieldQuery("vendor", vendorValue, analyzer, finalQueryBuilder);
            addAnalyzedFieldQuery("product", productValue, analyzer, finalQueryBuilder);
            addVersionQuery("tokenizedVersion", versionValue, finalQueryBuilder);

            Query finalQuery = finalQueryBuilder.build();

            TopDocs topDocs = searcher.search(finalQuery, TOP_MATCHES_THRESHOLD);
            for (ScoreDoc sd : topDocs.scoreDocs) {
                results.add(mapDocumentToCpeEntry(searcher.doc(sd.doc)));
            }

        } catch (Exception e) {
            logger.error("Error during tokenized multiFieldSearch: {}", e.getMessage(), e);
        }

        return results;
    }

    private void addTermsToQuery(String field, List<String> values, Analyzer analyzer, BooleanQuery.Builder builder) {
        if (values != null && !values.isEmpty()) {
            BooleanQuery.Builder inner = new BooleanQuery.Builder();
            for (String val : values) {
            	try {
                inner.add(new QueryParser(field, analyzer).parse(QueryParser.escape(val)), BooleanClause.Occur.SHOULD);
            	} catch (Exception e) {
            		logger.warn("Error parsing query for field '{}': {}", field, e.getMessage(), e);
            	}
            }
            builder.add(inner.build(), BooleanClause.Occur.MUST);
        }
    }

    private void addAnalyzedFieldQuery(String field, String value, Analyzer analyzer, BooleanQuery.Builder builder) throws IOException {
        if (value != null && !value.isBlank()) {
            List<String> tokens = getAnalyzedTokens(field, normalize(value), analyzer);
            BooleanQuery.Builder inner = new BooleanQuery.Builder();
            for (String token : tokens) {
                inner.add(new TermQuery(new Term(field, token)), BooleanClause.Occur.SHOULD);
            }
            builder.add(inner.build(), BooleanClause.Occur.MUST);
        }
    }

    private void addVersionQuery(String field, String versionValue, BooleanQuery.Builder builder) {
        if (versionValue != null && !versionValue.isBlank()) {
            List<String> tokens = getVersionPrefixes(versionValue);
            BooleanQuery.Builder versionBuilder = new BooleanQuery.Builder();
            for (String token : tokens) {
                versionBuilder.add(new TermQuery(new Term(field, token)), BooleanClause.Occur.SHOULD);
            }
            builder.add(versionBuilder.build(), BooleanClause.Occur.MUST);
        }
    }

    private List<String> getAnalyzedTokens(String fieldName, String input, Analyzer analyzer) throws IOException {
        List<String> tokens = new ArrayList<>();
        try (TokenStream tokenStream = analyzer.tokenStream(fieldName, input)) {
            CharTermAttribute attr = tokenStream.addAttribute(CharTermAttribute.class);
            tokenStream.reset();
            while (tokenStream.incrementToken()) {
                tokens.add(attr.toString());
            }
            tokenStream.end();
        }
        return tokens;
    }

    private String normalize(String input) {
        return input.toLowerCase().replaceAll("[^a-z0-9\\s]", "").trim();
    }

    private List<String> getVersionPrefixes(String version) {
        List<String> tokens = new ArrayList<>();
        if (version == null || version.isBlank()) return tokens;

        String[] parts = version.split("\\.");
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) builder.append(".");
            builder.append(parts[i]);
            tokens.add(builder.toString());
        }
        return tokens;
    }

    private CpeEntry mapDocumentToCpeEntry(Document doc) {
        CpeEntry entry = new CpeEntry();
        try {
            entry.setEntryId(Integer.parseInt(doc.get("entryId")));
            entry.setCpeName(doc.get("cpeName"));
            entry.setCpeTitle(doc.get("cpeTitle"));
            entry.setVendor(doc.get("vendor"));
            entry.setProduct(doc.get("product"));
            entry.setVersion(doc.get("version"));
            entry.setDeprecated(Boolean.parseBoolean(doc.get("isDeprecated")));
            if (doc.get("updatedDate") != null) {
                entry.setUpdatedDate(LocalDateTime.parse(doc.get("updatedDate")));
            }
        } catch (Exception e) {
            logger.warn("Error mapping Lucene document to CpeEntry: {}", e.getMessage(), e);
        }
        return entry;
    }
}
