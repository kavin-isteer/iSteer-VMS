package com.isteer.vms.core.engine.job;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.log4j.Log4j2;

@Component
@Log4j2
public class NVDDataProcessor {

    private DataSource datasource;
    

    public NVDDataProcessor(DataSource datasource) {
		this.datasource = datasource;
	}

	@Value("${nvd.api.key}")
    private String nvdApiKey;

    private static final String DOWNLOAD_URL = "https://nvd.nist.gov/feeds/json/cpe/2.0/nvdcpe-2.0.zip";
    private static final String EXTRACT_FOLDER = "cpe_data";
    private static final String ZIP_FILE = "nvdcpe-2.0.zip";

    public void updateCpeDictionary() throws Exception {
        log.info("[TASK STARTED] Started >> Updating CPE dictionary from NVD data feed...");

        // Step 1: Check for existing zip file and extracted folder, delete if present
        checkAndCleanupExistingFiles(ZIP_FILE, EXTRACT_FOLDER);

        // Step 2: Download the ZIP file
        downloadZip(DOWNLOAD_URL, ZIP_FILE);

        // Step 3: Extract the ZIP file
        extractZip(ZIP_FILE, EXTRACT_FOLDER);

        // Step 4: Parse and insert data into the database in batches
        File[] chunkFiles = new File(EXTRACT_FOLDER + "/nvdcpe-2.0-chunks").listFiles((dir, name) -> name.endsWith(".json"));
        if (chunkFiles != null && chunkFiles.length > 0) {
            log.info("Found " + chunkFiles.length + " chunk files.");
            batchInsertData(chunkFiles);
        } else {
            log.info("No chunk files found in the directory.");
        }

        // Step 5: Clean up - Delete the zip and extracted folder after job is complete
        cleanupFiles(ZIP_FILE, EXTRACT_FOLDER);

        log.info("[TASK COMPLETED] Completed >> Updating CPE dictionary from NVD data feed...");
    }

    // Method to check for existing zip and folder, and clean up
    private void checkAndCleanupExistingFiles(String zipFile, String extractFolder) {
        // Check and delete the ZIP file if it exists
        File zip = new File(zipFile);
        if (zip.exists() && zip.delete()) {
            log.info("checkAndCleanupExistingFiles >> Deleted existing ZIP file: " + zipFile);
        } else {
            log.info("checkAndCleanupExistingFiles >> No existing ZIP file or failed to delete: " + zipFile);
        }

        // Check and delete the extracted folder if it exists
        File folder = new File(extractFolder);
        if (folder.exists() && deleteDirectory(folder)) {
            log.info("checkAndCleanupExistingFiles >> Deleted existing extracted folder: " + extractFolder);
        } else {
            log.info("checkAndCleanupExistingFiles >> No existing extracted folder or failed to delete: " + extractFolder);
        }
    }

    // Download ZIP file from the provided URL
    private void downloadZip(String urlString, String outputFile) throws IOException {
        log.info("downloadZip >> Downloading NVD data zip file.....");
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("apiKey", nvdApiKey);
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);

        try (InputStream inputStream = connection.getInputStream();
                FileOutputStream outputStream = new FileOutputStream(outputFile)) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        }
        log.info("downloadZip >> Downloaded NVD data zip file successfully to " + outputFile);
    }

    private static void extractZip(String zipFilePath, String extractFolder) throws IOException {
        log.info("extractZip >> Extracting NVD data zip file: " + zipFilePath);
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFilePath))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                File newFile = new File(extractFolder, entry.getName());
                
                // Log the extraction process
                log.info("extractZip >> Extracting: " + newFile.getPath());
                
                // Create parent directories if they don't exist
                newFile.getParentFile().mkdirs();

                try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(newFile))) {
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = zis.read(buffer)) != -1) {
                        bos.write(buffer, 0, bytesRead);
                    }
                }
            }
         log.info("extractZip >> Completed Extracting NVD data zip file: " + zipFilePath);
        }
    }

    // Parse JSON and batch insert data into the database
    private void batchInsertData(File[] chunkFiles) throws Exception {
        log.info("batchInsertData >> Starting batch insert into database...");
        log.info("batchInsertData >> Doing batch insert for "+chunkFiles.length+" files");
        Connection conn = datasource.getConnection();
        conn.setAutoCommit(false); // Set auto commit to false for batch processing
        String insertSQL = "INSERT INTO cpe_dictionary (cpe_name, cpe_title, vendor, product, version, update_date, deprecated) "
                            + "VALUES (?, ?, ?, ?, ?, NOW(), ?) ";
                    
        try (PreparedStatement pstmt = conn.prepareStatement(insertSQL)) {
            int batchSize = 10000; // Adjust the batch size if needed
            int count = 0;
            int totalResults = 0;
            pstmt.execute("TRUNCATE TABLE cpe_dictionary");

            ObjectMapper objectMapper = new ObjectMapper();

            // Iterate through each JSON chunk file
            for (File chunkFile : chunkFiles) {
                log.info("batchInsertData >> Processing file: " + chunkFile.getName());
                JsonNode rootNode = objectMapper.readTree(chunkFile);
                totalResults = rootNode.path("totalResults").asInt();
                if (rootNode.has("products")) {
                    JsonNode cpes = rootNode.path("products");

                    if (!cpes.isArray() || cpes.size() == 0) break;

                    for (JsonNode node : cpes) {
                        JsonNode cpe= node.path("cpe");
                        String cpeName = cpe.has("cpe23Uri")
                            ? cpe.path("cpe23Uri").asText()
                            : cpe.path("cpeName").asText();

                        // Extract the title for lang="en" (if exists)
                        String title = "";
                        JsonNode titles = cpe.path("titles");
                        if (titles.isArray() && titles.size() > 0) {
                            // Iterate through the titles and look for lang="en"
                            for (JsonNode titleNode : titles) {
                                if ("en".equals(titleNode.path("lang").asText())) {
                                    title = titleNode.path("title").asText();
                                    break; // Exit the loop once the English title is found
                                }
                            }
                        }

                        boolean deprecated = cpe.path("deprecated").asBoolean(false);

                        // Extract vendor/product/version
                        String[] parts = cpeName.split(":");
                        String vendor = parts.length > 3 ? parts[3] : "";
                        String product = parts.length > 4 ? parts[4] : "";
                        String version = parts.length > 5 ? parts[5] : "";
                        pstmt.setString(1, cpeName);
                        pstmt.setString(2, title);
                        pstmt.setString(3, vendor);
                        pstmt.setString(4, product);
                        pstmt.setString(5, version);
                        pstmt.setBoolean(6, deprecated);

                        // Add to batch
                        pstmt.addBatch();
                        count++;

                        // If batch size is reached, execute batch
                        if (count % batchSize == 0) {
                            pstmt.executeBatch();
                            conn.commit(); // Commit after each batch
                            log.info("batchInsertData >> Inserted batch of " + batchSize + " entries. Total records inserted so far: " + count +" out of total results: "+totalResults);
                        }
                    }
                }
            }

            // Execute any remaining records
            pstmt.executeBatch();
            conn.commit();
            log.info("batchInsertData >> Inserted remaining entries.Total records inserted so far: " + count +" out of total results: "+totalResults);

        } catch (SQLException e) {
            conn.rollback(); // Rollback in case of an error
            e.printStackTrace();
        } finally {
            conn.close();
        }
    }

    // Cleanup method to delete downloaded zip file and extracted folder
    private void cleanupFiles(String zipFile, String extractFolder) {
        log.info("cleanupFiles >> Cleaning up downloaded zip and extracted folder...");

        // Delete the ZIP file
        File zip = new File(zipFile);
        if (zip.exists() && zip.delete()) {
            log.info("cleanupFiles >> Deleted ZIP file: " + zipFile);
        } else {
            log.info("cleanupFiles >> Failed to delete ZIP file: " + zipFile);
        }

        // Delete the extracted folder
        File folder = new File(extractFolder);
        if (folder.exists() && deleteDirectory(folder)) {
            log.info("cleanupFiles >> Deleted extracted folder: " + extractFolder);
        } else {
            log.info("cleanupFiles >> Failed to delete extracted folder: " + extractFolder);
        }
    }

    // Helper method to recursively delete a directory and its contents
    private boolean deleteDirectory(File directory) {
        if (directory.isDirectory()) {
            String[] files = directory.list();
            if (files != null) {
                for (String file : files) {
                    boolean success = deleteDirectory(new File(directory, file));
                    if (!success) {
                        return false;
                    }
                }
            }
        }
        return directory.delete();
    }
}
