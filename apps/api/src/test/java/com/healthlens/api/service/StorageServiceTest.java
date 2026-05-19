package com.healthlens.api.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectsResponse;

import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StorageServiceTest {

    @Mock
    private S3Client s3Client;

    @Test
    void deleteObjects_chunksExplicitDeletesAtS3Limit() {
        StorageService storageService = storageServiceWithMockS3();
        when(s3Client.deleteObjects(any(DeleteObjectsRequest.class)))
                .thenReturn(DeleteObjectsResponse.builder().build());
        List<String> keys = IntStream.range(0, 1001)
                .mapToObj(i -> "health-records/user/profile/record-" + i + ".pdf")
                .toList();

        int deleted = storageService.deleteObjects(keys);

        assertThat(deleted).isEqualTo(1001);
        ArgumentCaptor<DeleteObjectsRequest> captor = ArgumentCaptor.forClass(DeleteObjectsRequest.class);
        verify(s3Client, times(2)).deleteObjects(captor.capture());
        assertThat(captor.getAllValues().get(0).delete().objects()).hasSize(1000);
        assertThat(captor.getAllValues().get(1).delete().objects()).hasSize(1);
    }

    @Test
    void deleteObjects_throwsWhenS3ReportsPartialDeleteErrors() {
        StorageService storageService = storageServiceWithMockS3();
        when(s3Client.deleteObjects(any(DeleteObjectsRequest.class)))
                .thenReturn(DeleteObjectsResponse.builder()
                        .errors(software.amazon.awssdk.services.s3.model.S3Error.builder()
                                .key("health-records/user/profile/record.pdf")
                                .message("denied")
                                .build())
                        .build());

        assertThatThrownBy(() -> storageService.deleteObjects(List.of("health-records/user/profile/record.pdf")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Failed to delete 1 explicit storage object");
    }

    private StorageService storageServiceWithMockS3() {
        StorageService storageService = new StorageService(
                "http://localhost:9000",
                "http://localhost:9000",
                "minioadmin",
                "minioadmin",
                "healthlens-test",
                "ap-southeast-1"
        );
        ReflectionTestUtils.setField(storageService, "s3Client", s3Client);
        return storageService;
    }
}
