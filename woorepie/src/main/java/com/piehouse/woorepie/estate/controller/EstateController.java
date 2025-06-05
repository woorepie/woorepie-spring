package com.piehouse.woorepie.estate.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.piehouse.woorepie.estate.dto.response.GetEstateDetailsResponse;
import com.piehouse.woorepie.estate.dto.response.GetEstatePriceResponse;
import com.piehouse.woorepie.estate.dto.response.GetEstateSimpleResponse;
import com.piehouse.woorepie.estate.service.EstateService;
import com.piehouse.woorepie.global.response.ApiResponse;
import com.piehouse.woorepie.global.response.ApiResponseUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.shaded.com.google.protobuf.Api;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

@Slf4j
@RestController
@RequestMapping("/estate")
@RequiredArgsConstructor
public class EstateController {

    private final EstateService estateService;

    /**
     * 청약 완료된 매물 리스트 조회
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<GetEstateSimpleResponse>>> getTradableEstates(HttpServletRequest request) {
        List<GetEstateSimpleResponse> responseList = estateService.getTradableEstates();
        return ApiResponseUtil.success(responseList, request);
    }

    /**
     * 청약 완료된 매물 상세 조회
     */
    @GetMapping(params = "estateId")
    public ResponseEntity<ApiResponse<GetEstateDetailsResponse>> getEstateDetails(@RequestParam Long estateId, HttpServletRequest request) {
        GetEstateDetailsResponse response = estateService.getTradableEstateDetails(estateId);
        return ApiResponseUtil.success(response, request);
    }

    @Value("${vworld.api.key}")  // application.yml에 키 추가 필요
    private String vWorldApiKey;

    /**
     * 실시간 공시 조회
     */
    @GetMapping("/land-price")
    public ResponseEntity<ApiResponse<Long>> getLandPrice(
            @RequestParam Double lat,
            @RequestParam Double lng,
            HttpServletRequest request
    ) {
        try {
            // 기본 URL 설정
            StringBuilder urlBuilder = new StringBuilder("http://api.vworld.kr/ned/wfs/getIndvdLandPriceWFS");

            // 파라미터 설정
            StringBuilder parameter = new StringBuilder();
            parameter.append("?" + URLEncoder.encode("key","UTF-8") + "=" + vWorldApiKey);
            parameter.append("&" + URLEncoder.encode("domain","UTF-8") + "=" + "localhost");
            parameter.append("&" + URLEncoder.encode("typename","UTF-8") + "=" + URLEncoder.encode("dt_d150", "UTF-8"));

            // EPSG:4326의 경우 bbox 순서가 (ymin,xmin,ymax,xmax)
            double offset = 0.001;
            String bbox = String.format(
                    Locale.US,
                    "%.6f,%.6f,%.6f,%.6f,EPSG:4326",
                    lat - offset, // ymin
                    lng - offset, // xmin
                    lat + offset, // ymax
                    lng + offset  // xmax
            );
            parameter.append("&" + URLEncoder.encode("bbox","UTF-8") + "=" + URLEncoder.encode(bbox, "UTF-8"));

            parameter.append("&" + URLEncoder.encode("maxFeatures","UTF-8") + "=" + URLEncoder.encode("1", "UTF-8"));
            parameter.append("&" + URLEncoder.encode("resultType","UTF-8") + "=" + URLEncoder.encode("results", "UTF-8"));
            parameter.append("&" + URLEncoder.encode("srsName","UTF-8") + "=" + URLEncoder.encode("EPSG:4326", "UTF-8"));
            parameter.append("&" + URLEncoder.encode("output","UTF-8") + "=" + URLEncoder.encode("text/xml; subtype=gml/2.1.2", "UTF-8"));

            // URL 연결
            String requestUrl = urlBuilder.toString() + parameter.toString();
            log.info("V-World API 요청 URL: {}", requestUrl);

            URL url = new URL(requestUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Content-type", "application/json");

            // 응답 읽기
            StringBuilder sb = new StringBuilder();
            try (BufferedReader rd = new BufferedReader(
                    new InputStreamReader(
                            conn.getResponseCode() >= 200 && conn.getResponseCode() <= 300
                                    ? conn.getInputStream()
                                    : conn.getErrorStream()))) {
                String line;
                while ((line = rd.readLine()) != null) {
                    sb.append(line);
                }
            }

            String response = sb.toString();
            log.info("V-World API 응답: {}", response);

            // XML 파싱
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(response)));

            // 에러 응답 체크
            NodeList errorList = doc.getElementsByTagName("ServiceException");
            if (errorList.getLength() > 0) {
                String errorMessage = errorList.item(0).getTextContent();
                log.error("V-World API 에러: {}", errorMessage);
                return ApiResponseUtil.error(HttpStatus.INTERNAL_SERVER_ERROR, "공시지가 조회 실패: " + errorMessage);
            }

            // 공시지가 추출 (sop:pblntf_pclnd 태그로 수정)
            NodeList priceList = doc.getElementsByTagName("sop:pblntf_pclnd");
            if (priceList.getLength() == 0) {
                return ApiResponseUtil.error(HttpStatus.NOT_FOUND, "공시지가 정보가 없습니다.");
            }

            long landPrice = Long.parseLong(priceList.item(0).getTextContent());
            return ApiResponseUtil.success(landPrice, request);

        } catch (Exception e) {
            log.error("공시지가 조회 실패", e);
            return ApiResponseUtil.error(HttpStatus.INTERNAL_SERVER_ERROR, "공시지가 조회 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 매물 시세 조회
     */
    @GetMapping("/price")
    public ResponseEntity<ApiResponse<List<GetEstatePriceResponse>>> getEstatePrice(@RequestParam Long estateId, HttpServletRequest request) {
        List<GetEstatePriceResponse> response = estateService.getEstatePriceHistory(estateId);
        return ApiResponseUtil.success(response, request);
    }
}
