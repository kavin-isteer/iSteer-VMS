package com.isteer.vms.core.engine.lucene;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.isteer.vms.core.engine.dao.CPEEntriesDao;
import com.isteer.vms.core.engine.model.CpeEntry;

@Service
public class LuceneIndexRunner {
	@Autowired
	CPEEntriesDao cpeEntriesDao;
	
	private static final Logger logger = LogManager.getLogger(LuceneIndexRunner.class);

	public void createIndexFromCvssDb() throws IOException, SQLException {
		// File indexDir = new File("lucene-index");
		// boolean indexExists = indexDir.exists() && indexDir.isDirectory() &&
		// indexDir.list().length > 0;

		logger.info("Fetching CPE entries from DB and indexing to lucene index...");

		// indexer
		LuceneCpeIndexer indexer = new LuceneCpeIndexer();
		indexer.open(); // open once

		int offset = 0;
		int limit = 100000;
		int totalIndexed = 0;
		while (true) {
			List<CpeEntry> batch = cpeEntriesDao.getAllCpeEntriesWithOffset(offset, limit);
			if (batch.isEmpty())
				break;

			indexer.indexBatch(batch);
			totalIndexed += batch.size();
			logger.info("Indexed batch of " + batch.size() + " records. Total: " + totalIndexed);

			offset += limit;
		}
		indexer.close(); // close once at the end
		logger.info("Indexing complete. Total records indexed: " + totalIndexed);
	}
}
