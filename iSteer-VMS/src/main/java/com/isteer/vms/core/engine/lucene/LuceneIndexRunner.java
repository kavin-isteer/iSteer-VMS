package com.isteer.vms.core.engine.lucene;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import org.springframework.stereotype.Service;

import com.isteer.vms.core.engine.dao.CPEEntriesDao;
import com.isteer.vms.core.engine.model.CpeEntry;

import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
public class LuceneIndexRunner {

	private CPEEntriesDao cpeEntriesDao;
	
	public LuceneIndexRunner(CPEEntriesDao cpeEntriesDao) {
		this.cpeEntriesDao = cpeEntriesDao;
	}

	public void createIndexFromCvssDb() throws IOException, SQLException {
		
		log.info("Fetching CPE entries from DB and indexing to lucene index...");

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
			log.info("Indexed batch of " + batch.size() + " records. Total: " + totalIndexed);

			offset += limit;
		}
		indexer.close(); // close once at the end
		log.info("Indexing complete. Total records indexed: " + totalIndexed);
	}
}
