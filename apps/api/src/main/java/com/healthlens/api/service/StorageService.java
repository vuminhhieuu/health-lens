package com.healthlens.api.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import jakarta.annotation.PreDestroy;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.BucketAlreadyExistsException;
import software.amazon.awssdk.services.s3.model.BucketAlreadyOwnedByYouException;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutBucketCorsRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.CORSConfiguration;
import software.amazon.awssdk.services.s3.model.CORSRule;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.net.URI;
import java.time.Duration;
import java.util.List;

@Service
public class StorageService {

    private final String bucket;
    private final S3Client s3Client;
    private final S3Presigner presigner;
    private final S3Presigner internalPresigner;

    public StorageService(
            @Value("${app.storage.endpoint:http://localhost:9000}") String endpoint,
            @Value("${app.storage.public-endpoint:${app.storage.endpoint:http://localhost:9000}}") String publicEndpoint,
            @Value("${app.storage.access-key:minioadmin}") String accessKey,
            @Value("${app.storage.secret-key:minioadmin}") String secretKey,
            @Value("${app.storage.bucket:healthlens-dev}") String bucket,
            @Value("${app.storage.region:ap-southeast-1}") String region
    ) {
        this.bucket = bucket;
        this.s3Client = S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey)))
                .build();
        this.presigner = S3Presigner.builder()
                .endpointOverride(URI.create(publicEndpoint))
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey)))
                .build();
        this.internalPresigner = S3Presigner.builder()
                .endpointOverride(URI.create(endpoint))
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey)))
                .build();
    }

    public void ensureBucketExists(boolean allowAutoCreate) {
        HeadBucketRequest headBucketRequest = HeadBucketRequest.builder().bucket(bucket).build();
        try {
            s3Client.headBucket(headBucketRequest);
            return;
        } catch (NoSuchBucketException ex) {
            if (!allowAutoCreate) {
                throw new IllegalStateException("Storage bucket does not exist: " + bucket, ex);
            }
        } catch (S3Exception ex) {
            if (ex.statusCode() != 404) {
                throw new IllegalStateException("Storage bucket check failed: " + bucket, ex);
            }
            if (!allowAutoCreate) {
                throw new IllegalStateException("Storage bucket does not exist: " + bucket, ex);
            }
        }

        try {
            s3Client.createBucket(CreateBucketRequest.builder().bucket(bucket).build());
        } catch (BucketAlreadyOwnedByYouException | BucketAlreadyExistsException ignored) {
            // Created by another instance concurrently, safe to continue.
        } catch (S3Exception ex) {
            throw new IllegalStateException("Failed to auto-create storage bucket: " + bucket, ex);
        }
    }

    public void configureBucketCors(List<String> allowedOrigins) {
        if (allowedOrigins == null || allowedOrigins.isEmpty()) {
            return;
        }

        CORSRule corsRule = CORSRule.builder()
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "PUT", "POST", "HEAD")
                .allowedHeaders("*")
                .exposeHeaders("ETag")
                .maxAgeSeconds(3000)
                .build();

        CORSConfiguration corsConfiguration = CORSConfiguration.builder()
                .corsRules(corsRule)
                .build();

        s3Client.putBucketCors(PutBucketCorsRequest.builder()
                .bucket(bucket)
                .corsConfiguration(corsConfiguration)
                .build());
    }

    public String generateUploadUrl(String key, Duration ttl, String contentType) {
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(contentType)
                .build();

        PutObjectPresignRequest request = PutObjectPresignRequest.builder()
                .signatureDuration(ttl)
                .putObjectRequest(putObjectRequest)
                .build();

        PresignedPutObjectRequest presigned = presigner.presignPutObject(request);
        return presigned.url().toExternalForm();
    }

    public String generateDownloadUrl(String key, Duration ttl) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();

        GetObjectPresignRequest request = GetObjectPresignRequest.builder()
                .signatureDuration(ttl)
                .getObjectRequest(getObjectRequest)
                .build();

        PresignedGetObjectRequest presigned = presigner.presignGetObject(request);
        return presigned.url().toExternalForm();
    }

    public String generateInternalDownloadUrl(String key, Duration ttl) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();

        GetObjectPresignRequest request = GetObjectPresignRequest.builder()
                .signatureDuration(ttl)
                .getObjectRequest(getObjectRequest)
                .build();

        PresignedGetObjectRequest presigned = internalPresigner.presignGetObject(request);
        return presigned.url().toExternalForm();
    }

    public void deleteObject(String key) {
        s3Client.deleteObject(DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build());
    }

    @PreDestroy
    public void closeClients() {
        presigner.close();
        internalPresigner.close();
        s3Client.close();
    }
}
