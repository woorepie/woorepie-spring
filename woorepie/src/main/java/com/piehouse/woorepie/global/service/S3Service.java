package com.piehouse.woorepie.global.service;

import com.piehouse.woorepie.global.dto.request.S3AgentRequest;
import com.piehouse.woorepie.global.dto.request.S3CustomerRequest;
import com.piehouse.woorepie.global.dto.request.S3EstateRequest;
import com.piehouse.woorepie.global.dto.response.S3UrlResponse;

import java.util.List;

public interface S3Service {

    String getPublicS3Url(String key);

    S3UrlResponse generateCustomerPresignedUrl(String domain, S3CustomerRequest s3request);

    List<S3UrlResponse> generateAgentPresignedUrl(String domain, S3AgentRequest s3request);

    List<S3UrlResponse> generateEstatePresignedUrl(String domain, S3EstateRequest s3request);

}
