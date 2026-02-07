package com.vehicleordering.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class S3Service {

    private static final Logger logger = LoggerFactory.getLogger(S3Service.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    private final S3Client s3Client;
    private final String bucketName;

    public S3Service(S3Client s3Client, @Value("${aws.s3.bucket-name}") String bucketName) {
        this.s3Client = s3Client;
        this.bucketName = bucketName;
    }

    public String uploadPdf(String orderId, byte[] pdfBytes) {
        logger.info("Uploading PDF to S3 for order: {}", orderId);

        try {
            String currentDate = LocalDateTime.now().format(DATE_FORMATTER);
            String key = String.format("orders/%s/%s-order-summary.pdf", currentDate, orderId);

            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType("application/pdf")
                    .contentLength((long) pdfBytes.length)
                    .build();

            PutObjectResponse response = s3Client.putObject(putObjectRequest,
                    RequestBody.fromBytes(pdfBytes));

            String pdfUrl = String.format("https://%s.s3.amazonaws.com/%s", bucketName, key);

            logger.info("PDF uploaded successfully to S3: {}", pdfUrl);
            return pdfUrl;

        } catch (Exception e) {
            logger.error("Error uploading PDF to S3 for order {}: {}", orderId, e.getMessage(), e);
            throw new RuntimeException("Failed to upload PDF to S3", e);
        }
    }

    public void deletePdf(String pdfUrl) {
        logger.info("Deleting PDF from S3: {}", pdfUrl);

        try {
            // Extract key from URL
            String key = pdfUrl.replace("https://" + bucketName + ".s3.amazonaws.com/", "");

            s3Client.deleteObject(builder -> builder
                    .bucket(bucketName)
                    .key(key)
                    .build());

            logger.info("PDF deleted successfully from S3: {}", pdfUrl);

        } catch (Exception e) {
            logger.error("Error deleting PDF from S3: {}", e.getMessage(), e);
            // Don't throw exception for delete failures as it's not critical
        }
    }
}
