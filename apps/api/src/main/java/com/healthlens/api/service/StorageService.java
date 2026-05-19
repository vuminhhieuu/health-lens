package com.healthlens.api.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import jakarta.annotation.PreDestroy;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.BucketAlreadyExistsException;
import software.amazon.awssdk.services.s3.model.BucketAlreadyOwnedByYouException;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.Delete;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectsResponse;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutBucketCorsRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.model.CORSConfiguration;
import software.amazon.awssdk.services.s3.model.CORSRule;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;
import software.amazon.awssdk.core.sync.RequestBody;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Slf4j
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

    public void uploadObject(String key, byte[] bytes, String contentType) {
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(contentType)
                .build();

        s3Client.putObject(putObjectRequest, RequestBody.fromBytes(bytes));
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

    /**
     * Delete every object whose key starts with the given prefix (quiet batch deletes).
     * Delegates to {@link #deleteObjectsByPrefix(String, boolean)} with {@code quiet=true}.
     */
    public int deleteObjectsByPrefix(String prefix) {
        return deleteObjectsByPrefix(prefix, true);
    }

    /**
     * Delete every object whose key starts with the given prefix.
     * Used by Story 1.6 right-to-delete to wipe a user's uploaded files.
     *
     * Returns the number of objects deleted (best-effort; logs but does not
     * throw when MinIO/S3 is unavailable so the rest of the deletion sequence
     * can still complete).
     *
     * @param quiet passed through to S3 batch delete ({@link Delete.Builder#quiet(Boolean)})
     */
    public int deleteObjectsByPrefix(String prefix, boolean quiet) {
        if (prefix == null || prefix.isBlank()) {
            return 0;
        }

        int totalDeleted = 0;
        String continuationToken = null;
        try {
            do {
                ListObjectsV2Request.Builder listBuilder = ListObjectsV2Request.builder()
                        .bucket(bucket)
                        .prefix(prefix);
                if (continuationToken != null) {
                    listBuilder.continuationToken(continuationToken);
                }

                ListObjectsV2Response listResponse = s3Client.listObjectsV2(listBuilder.build());
                List<S3Object> contents = listResponse.contents();
                if (contents == null || contents.isEmpty()) {
                    break;
                }

                List<ObjectIdentifier> identifiers = new ArrayList<>(contents.size());
                for (S3Object obj : contents) {
                    identifiers.add(ObjectIdentifier.builder().key(obj.key()).build());
                }

                DeleteObjectsResponse deleteResponse = s3Client.deleteObjects(DeleteObjectsRequest.builder()
                        .bucket(bucket)
                        .delete(Delete.builder().objects(identifiers).quiet(quiet).build())
                        .build());

                totalDeleted += identifiers.size();
                if (deleteResponse.hasErrors() && !deleteResponse.errors().isEmpty()) {
                    log.warn("Some objects failed to delete for prefix {}: {} errors",
                            prefix, deleteResponse.errors().size());
                }

                continuationToken = Boolean.TRUE.equals(listResponse.isTruncated())
                        ? listResponse.nextContinuationToken()
                        : null;
            } while (continuationToken != null);

            log.info("Deleted {} object(s) under prefix '{}'", totalDeleted, prefix);
            return totalDeleted;
        } catch (S3Exception ex) {
            log.error("S3 error while deleting objects under prefix '{}': {}", prefix, ex.getMessage(), ex);
            return totalDeleted;
        } catch (SdkException ex) {
            log.error("S3 client error while deleting objects under prefix '{}': {}", prefix, ex.getMessage(), ex);
            return totalDeleted;
        } catch (RuntimeException ex) {
            log.error("Unexpected error while deleting objects under prefix '{}': {}", prefix, ex.getMessage(), ex);
            return totalDeleted;
        }
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

    public byte[] downloadObjectBytes(String key) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();

        return s3Client.getObjectAsBytes(getObjectRequest).asByteArray();
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
