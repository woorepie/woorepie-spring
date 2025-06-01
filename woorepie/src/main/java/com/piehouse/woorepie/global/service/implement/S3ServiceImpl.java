package com.piehouse.woorepie.global.service.implement;

import com.piehouse.woorepie.global.dto.request.S3AgentRequest;
import com.piehouse.woorepie.global.dto.request.S3CustomerRequest;
import com.piehouse.woorepie.global.dto.request.S3EstateRequest;
import com.piehouse.woorepie.global.dto.response.S3UrlResponse;
import com.piehouse.woorepie.global.service.S3Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;

import java.net.URL;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
public class S3ServiceImpl implements S3Service {

    private final S3Presigner presigner;
    private final String bucketName;
    private final String region;

    private final Duration validFor = Duration.ofMinutes(5);

    public S3ServiceImpl(S3Presigner presigner, @Value("${aws.s3.bucket}") String bucketName, @Value("${aws.region}") String region) {
        this.presigner = presigner;
        this.bucketName = bucketName;
        this.region = region;
    }

    @Override
    public String getPublicS3Url(String key) {
        System.out.println(String.format("https://%s.s3.%s.amazonaws.com/%s",
                bucketName,
                region,
                key
        ));
        return String.format("https://%s.s3.%s.amazonaws.com/%s",
                bucketName,
                region,
                key
        );
    }

    @Override
    public S3UrlResponse generateCustomerPresignedUrl(String domain, S3CustomerRequest s3request) {

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String objectKey = String.format("%s/%s/%s", domain+"/identification", s3request.getCustomerEmail(), timestamp);

        PutObjectRequest putReq = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .contentType(s3request.getFileType())
                .acl("public-read") // ✅ 퍼블릭 읽기 권한 추가
                .build();

        PresignedPutObjectRequest presignedRequest = presigner.presignPutObject(p -> p
                .signatureDuration(validFor)
                .putObjectRequest(putReq)
        );

        URL url = presignedRequest.url();

        return S3UrlResponse.builder()
                .url(url.toString())
                .key(objectKey)
                .expiresIn(validFor.getSeconds())
                .build();

    }

    @Override
    public List<S3UrlResponse> generateAgentPresignedUrl(String domain, S3AgentRequest s3request) {

        Map<String, String> fileTypes = Map.of(
                "identification", s3request.getIdentificationFileType(),
                "cert", s3request.getCertFileType(),
                "warrant", s3request.getWarrantFileType()
        );

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));

        return fileTypes.entrySet().stream()
                .map(entry -> {
                    String fileType = entry.getKey();
                    String contentType = entry.getValue();
                    String objectKey = String.format("%s/%s/%s-%s", domain, s3request.getAgentEmail(), fileType, timestamp);

                    PutObjectRequest putReq = PutObjectRequest.builder()
                            .bucket(bucketName)
                            .key(objectKey)
                            .contentType(contentType)
                            .acl("public-read") // ✅ 퍼블릭 읽기 권한 추가
                            .build();

                    PresignedPutObjectRequest presignedRequest = presigner.presignPutObject(p -> p
                            .signatureDuration(validFor)
                            .putObjectRequest(putReq)
                    );

                    return S3UrlResponse.builder()
                            .url(presignedRequest.url().toString())
                            .key(objectKey)
                            .expiresIn(validFor.getSeconds())
                            .build();
                })
                .toList();
    }

    @Override
    public List<S3UrlResponse> generateEstatePresignedUrl(String domain, S3EstateRequest s3request) {
        System.out.println(s3request.toString());
        Map<String, String> fileTypeToContentType = Map.of(
                "estate-image", s3request.getImageFileType(),
                "sub-guide", s3request.getSubGuideFileType(),
                "securities-report", s3request.getSecuritiesReportFileType(),
                "investment-explanation", s3request.getInvestmentExplanationFileType(),
                "property-mng-contract", s3request.getPropertyMngContractFileType(),
                "appraisal-report", s3request.getAppraisalReportFileType()
        );

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));

        return fileTypeToContentType.entrySet().stream()
                .map(entry -> {
                    String fileType = entry.getKey();
                    String contentType = entry.getValue();

                    String objectKey = String.format(
                            "%s/%s/%s-%s", domain, s3request.getEstateAddress(), fileType, timestamp
                    );

                    PutObjectRequest putReq = PutObjectRequest.builder()
                            .bucket(bucketName)
                            .key(objectKey)
                            .contentType(contentType)
                            .acl("public-read") // ✅ 퍼블릭 읽기 권한 추가
                            .build();

                    PresignedPutObjectRequest presignedRequest = presigner.presignPutObject(p -> p
                            .signatureDuration(validFor)
                            .putObjectRequest(putReq)
                    );

                    return S3UrlResponse.builder()
                            .url(presignedRequest.url().toString())
                            .key(objectKey)
                            .expiresIn(validFor.getSeconds())
                            .build();
                })
                .toList();

    }

}
