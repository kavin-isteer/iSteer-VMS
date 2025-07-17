package com.isteer.vms.core.engine.lucene;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.springframework.stereotype.Service;

import com.isteer.vms.core.engine.model.CpeEntry;

/**
 * Service for indexing CPE entries into a Lucene index.
 */
@Service
public class LuceneCpeIndexer {

    private static final Logger logger = LogManager.getLogger(LuceneCpeIndexer.class);
    private static final String INDEX_DIR = "lucene-index";
    private IndexWriter indexWriter;

    /**
     * Initializes the Lucene IndexWriter.
     * This method must be called before indexing documents.
     */
    public void open() {
        try {
            if (indexWriter == null) {
                Directory directory = FSDirectory.open(Paths.get(INDEX_DIR));
                Analyzer analyzer = new StandardAnalyzer();
                IndexWriterConfig config = new IndexWriterConfig(analyzer);
                config.setOpenMode(IndexWriterConfig.OpenMode.CREATE); // Creates new index
                indexWriter = new IndexWriter(directory, config);
                logger.info("Lucene IndexWriter initialized at directory '{}'", INDEX_DIR);
            }
        } catch (IOException e) {
            logger.error("Failed to initialize Lucene IndexWriter: {}", e.getMessage(), e);
        }
    }

    /**
     * Indexes a batch of CPE entries into Lucene.
     *
     * @param entries List of CpeEntry objects to be indexed
     */
    public void indexBatch(List<CpeEntry> entries) {
        if (indexWriter == null) {
            logger.warn("IndexWriter is not initialized. Call open() before indexing.");
            return;
        }

        for (CpeEntry entry : entries) {
            try {
                Document doc = new Document();

                doc.add(new StringField("entryId", String.valueOf(entry.getEntryId()), Field.Store.YES));
                doc.add(new TextField("cpeName", entry.getCpeName(), Field.Store.YES));
                doc.add(new TextField("cpeTitle", entry.getCpeTitle(), Field.Store.YES));
                doc.add(new TextField("vendor", entry.getVendor(), Field.Store.YES));
                doc.add(new TextField("product", entry.getProduct(), Field.Store.YES));
                doc.add(new TextField("version", entry.getVersion(), Field.Store.YES));

                String versionPrefixes = String.join(" ", getVersionPrefixes(entry.getVersion()));
                doc.add(new TextField("tokenizedVersion", versionPrefixes, Field.Store.YES));
                doc.add(new StringField("isDeprecated", String.valueOf(entry.isDeprecated()), Field.Store.YES));

                if (entry.getUpdatedDate() != null) {
                    doc.add(new StringField("updatedDate", entry.getUpdatedDate().toString(), Field.Store.YES));
                }

                indexWriter.addDocument(doc);
                logger.debug("Indexed entryId={} cpeName={}", entry.getEntryId(), entry.getCpeName());

            } catch (Exception e) {
                logger.error("Failed to index entry with ID {}: {}", entry.getEntryId(), e.getMessage(), e);
            }
        }
    }

    /**
     * Closes the IndexWriter and commits any pending changes.
     * Should be called after indexing is completed.
     */
    public void close() {
        try {
            if (indexWriter != null) {
                indexWriter.flush();
                indexWriter.commit();
                indexWriter.close();
                logger.info("Lucene IndexWriter successfully closed.");
                indexWriter = null;
            }
        } catch (IOException e) {
            logger.error("Error closing Lucene IndexWriter: {}", e.getMessage(), e);
        }
    }

    /**
     * Generates version prefix tokens for partial matching of versions.
     *
     * Example: 1.2.3 → [1, 1.2, 1.2.3]
     *
     * @param version Full version string
     * @return List of version prefix tokens
     */
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
}
