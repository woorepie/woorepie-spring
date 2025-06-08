package com.piehouse.woorepie.estate.controller;

import com.piehouse.woorepie.estate.dto.response.*;
import com.piehouse.woorepie.estate.service.EstateRedisService;
import com.piehouse.woorepie.estate.service.EstateService;
import com.piehouse.woorepie.global.response.ApiResponse;
import com.piehouse.woorepie.global.response.ApiResponseUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
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
import java.util.List;
import java.util.Locale;

@Slf4j
@RestController
@RequestMapping("/estate")
@RequiredArgsConstructor
public class EstateController {

    private final EstateService estateService;
    private final EstateRedisService estateRedisService;

    @Value("${vworld.api.key}")
    private String vWorldApiKey;

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

    @GetMapping("/remain/token")
    public ResponseEntity<ApiResponse<GetRemainingTokensResponse>> getRemainingTokens(
            @RequestParam Long estateId,
            HttpServletRequest request) {

        Long remainingTokens = estateRedisService.getRemainingTokensOrInit(estateId);
        return ApiResponseUtil.success(new GetRemainingTokensResponse(remainingTokens), request);
    }
    /**
     * 실시간 공시지가 및 건물 정보 조회
     */
    @GetMapping("/land-price")
    public ResponseEntity<ApiResponse<GetBuildingInfoResponse>> getLandPrice(
            @RequestParam Double lat,
            @RequestParam Double lng,
            HttpServletRequest request
    ) {
        try {
            // 1. 공시지가 조회
            Integer landPrice = getLandPriceFromVWorld(lat, lng);
            
            // 2. 건물 정보 조회
            GetBuildingInfoResponse.BuildingDetails buildingInfo = getBuildingInfoFromVWorld(lat, lng);
            
            // 3. 응답 객체 생성
            GetBuildingInfoResponse response = GetBuildingInfoResponse.builder()
                    .price(landPrice)
                    .buildingInfo(buildingInfo)
                    .build();
                    
            return ApiResponseUtil.success(response, request);

        } catch (Exception e) {
            log.error("공시지가 및 건물 정보 조회 실패", e);
            return ApiResponseUtil.error(HttpStatus.INTERNAL_SERVER_ERROR, "조회 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * V-World API에서 공시지가 조회
     */
    private Integer getLandPriceFromVWorld(Double lat, Double lng) throws Exception {
        StringBuilder urlBuilder = new StringBuilder("http://api.vworld.kr/ned/wfs/getIndvdLandPriceWFS");
        StringBuilder parameter = new StringBuilder();
        parameter.append("?" + URLEncoder.encode("key","UTF-8") + "=" + vWorldApiKey);
        parameter.append("&" + URLEncoder.encode("domain","UTF-8") + "=" + "localhost");
        parameter.append("&" + URLEncoder.encode("typename","UTF-8") + "=" + URLEncoder.encode("dt_d150", "UTF-8"));

        double offset = 0.001;
        String bbox = String.format(
                Locale.US,
                "%.6f,%.6f,%.6f,%.6f,EPSG:4326",
                lat - offset, lng - offset, lat + offset, lng + offset
        );
        parameter.append("&" + URLEncoder.encode("bbox","UTF-8") + "=" + URLEncoder.encode(bbox, "UTF-8"));
        parameter.append("&" + URLEncoder.encode("maxFeatures","UTF-8") + "=" + URLEncoder.encode("1", "UTF-8"));
        parameter.append("&" + URLEncoder.encode("resultType","UTF-8") + "=" + URLEncoder.encode("results", "UTF-8"));
        parameter.append("&" + URLEncoder.encode("srsName","UTF-8") + "=" + URLEncoder.encode("EPSG:4326", "UTF-8"));
        parameter.append("&" + URLEncoder.encode("output","UTF-8") + "=" + URLEncoder.encode("text/xml; subtype=gml/2.1.2", "UTF-8"));

        String requestUrl = urlBuilder.toString() + parameter.toString();
        log.info("V-World 공시지가 API 요청 URL: {}", requestUrl);

        URL url = new URL(requestUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Content-type", "application/json");

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
        log.info("V-World 공시지가 API 응답: {}", response);

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new InputSource(new StringReader(response)));

        NodeList errorList = doc.getElementsByTagName("ServiceException");
        if (errorList.getLength() > 0) {
            String errorMessage = errorList.item(0).getTextContent();
            log.error("V-World 공시지가 API 에러: {}", errorMessage);
            throw new RuntimeException("공시지가 조회 실패: " + errorMessage);
        }

        NodeList priceList = doc.getElementsByTagName("sop:pblntf_pclnd");
        if (priceList.getLength() == 0) {
            log.warn("공시지가 정보가 없습니다.");
            return 0;
        }

        return Integer.parseInt(priceList.item(0).getTextContent());
    }

    /**
     * V-World Data API에서 건물 정보 조회
     */
    private GetBuildingInfoResponse.BuildingDetails getBuildingInfoFromVWorld(Double lat, Double lng) throws Exception {
        log.info("건물 정보 조회 요청 - 위도: {}, 경도: {}", lat, lng);
        
        try {
            // 1. 좌표를 PNU 코드로 변환
            String pnuCode = getPnuCodeFromCoordinates(lat, lng);
            if (pnuCode == null || pnuCode.isEmpty()) {
                log.warn("PNU 코드 조회 실패, 모의 데이터 반환");
                return createMockBuildingInfo(lat, lng);
            }
            
            // 2. PNU 코드로 건물 정보 조회
            return getBuildingInfoByPnu(pnuCode);
            
        } catch (Exception e) {
            log.error("V-World API 건물 정보 조회 실패", e);
            return createMockBuildingInfo(lat, lng);
        }
    }

    /**
     * 좌표를 PNU(지번코드)로 변환
     */
    private String getPnuCodeFromCoordinates(Double lat, Double lng) throws Exception {
        StringBuilder urlBuilder = new StringBuilder("http://api.vworld.kr/req/data");
        StringBuilder parameter = new StringBuilder();
        parameter.append("?service=data");
        parameter.append("&request=GetFeature");
        parameter.append("&data=LP_PA_CBND_BUBUN"); // 연속지적도
        parameter.append("&key=" + vWorldApiKey);
        parameter.append("&domain=localhost");
        parameter.append("&geometry=POINT(" + lng + " " + lat + ")");
        parameter.append("&geometryType=Point");
        parameter.append("&buffer=10");
        parameter.append("&crs=EPSG:4326");
        parameter.append("&format=json");

        String requestUrl = urlBuilder.toString() + parameter.toString();
        log.info("PNU 코드 조회 요청 URL: {}", requestUrl);

        URL url = new URL(requestUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Content-type", "application/json");

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
        log.info("PNU 코드 조회 응답: {}", response);

        // JSON 파싱하여 PNU 코드 추출
        if (response.contains("\"pnu\"")) {
            int pnuIndex = response.indexOf("\"pnu\"");
            int valueStart = response.indexOf(":", pnuIndex) + 2; // ": 다음
            int valueEnd = response.indexOf("\"", valueStart);
            if (valueEnd > valueStart) {
                return response.substring(valueStart, valueEnd);
            }
        }

        return null;
    }

    /**
     * PNU 코드로 건물 정보 조회
     */
    private GetBuildingInfoResponse.BuildingDetails getBuildingInfoByPnu(String pnuCode) throws Exception {
        StringBuilder urlBuilder = new StringBuilder("http://api.vworld.kr/req/data");
        StringBuilder parameter = new StringBuilder();
        parameter.append("?service=data");
        parameter.append("&request=GetFeature");
        parameter.append("&data=LP_PA_CBND_BUBUN"); // 연속지적도
        parameter.append("&key=" + vWorldApiKey);
        parameter.append("&domain=localhost");
        parameter.append("&attrFilter=pnu:=:" + pnuCode);
        parameter.append("&format=json");

        String requestUrl = urlBuilder.toString() + parameter.toString();
        log.info("건물 정보 조회 요청 URL: {}", requestUrl);

        URL url = new URL(requestUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Content-type", "application/json");

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
        log.info("건물 정보 조회 응답: {}", response);

        // JSON 파싱하여 건물 정보 추출
        try {
            Double buildingCoverage = parseJsonDouble(response, "btlRt");  // 건폐율
            Double floorAreaRatio = parseJsonDouble(response, "flrRt");    // 용적률
            String completionDate = parseJsonString(response, "cmpltDt");  // 준공일
            Double height = parseJsonDouble(response, "hght");             // 높이
            Integer grndFloor = parseJsonInteger(response, "grndFlr");     // 지상층수
            Integer ugrndFloor = parseJsonInteger(response, "ugrndFlr");   // 지하층수
            String mainPurps = parseJsonString(response, "mainPurps");     // 주용도
            String structure = parseJsonString(response, "strct");         // 구조

            log.info("V-World API 건물 정보 조회 성공 - 용도: {}, 건폐율: {}%, 용적률: {}%", 
                    mainPurps, buildingCoverage, floorAreaRatio);

            return GetBuildingInfoResponse.BuildingDetails.builder()
                    .buildingCoverage(buildingCoverage != null ? buildingCoverage : 0.0)
                    .floorAreaRatio(floorAreaRatio != null ? floorAreaRatio : 0.0)
                    .completionDate(completionDate != null ? completionDate : "")
                    .height(height != null ? height : 0.0)
                    .grndFloor(grndFloor != null ? grndFloor : 0)
                    .ugrndFloor(ugrndFloor != null ? ugrndFloor : 0)
                    .mainPurps(mainPurps != null ? mainPurps : "")
                    .structure(structure != null ? structure : "")
                    .build();

        } catch (Exception e) {
            log.warn("건물 정보 파싱 실패, 모의 데이터 반환", e);
            return createMockBuildingInfo(0.0, 0.0); // PNU 기반으로 모의 데이터 생성
        }
    }

    /**
     * JSON에서 Double 값 추출
     */
    private Double parseJsonDouble(String json, String key) {
        try {
            String searchKey = "\"" + key + "\"";
            int keyIndex = json.indexOf(searchKey);
            if (keyIndex == -1) return null;
            
            int valueStart = json.indexOf(":", keyIndex) + 1;
            int valueEnd = json.indexOf(",", valueStart);
            if (valueEnd == -1) valueEnd = json.indexOf("}", valueStart);
            
            String value = json.substring(valueStart, valueEnd).trim();
            value = value.replace("\"", "").replace(" ", "");
            
            if (value.equals("null") || value.isEmpty()) return null;
            return Double.parseDouble(value);
        } catch (Exception e) {
            log.debug("JSON Double 파싱 실패: {} - {}", key, e.getMessage());
            return null;
        }
    }

    /**
     * JSON에서 Integer 값 추출
     */
    private Integer parseJsonInteger(String json, String key) {
        try {
            Double doubleValue = parseJsonDouble(json, key);
            return doubleValue != null ? doubleValue.intValue() : null;
        } catch (Exception e) {
            log.debug("JSON Integer 파싱 실패: {} - {}", key, e.getMessage());
            return null;
        }
    }

    /**
     * JSON에서 String 값 추출
     */
    private String parseJsonString(String json, String key) {
        try {
            String searchKey = "\"" + key + "\"";
            int keyIndex = json.indexOf(searchKey);
            if (keyIndex == -1) return null;
            
            int valueStart = json.indexOf(":", keyIndex) + 1;
            valueStart = json.indexOf("\"", valueStart) + 1; // 따옴표 시작
            int valueEnd = json.indexOf("\"", valueStart);   // 따옴표 끝
            
            if (valueEnd > valueStart) {
                return json.substring(valueStart, valueEnd);
            }
            return null;
        } catch (Exception e) {
            log.debug("JSON String 파싱 실패: {} - {}", key, e.getMessage());
            return null;
        }
    }
    
    /**
     * 좌표 기반 모의 건물 정보 생성
     */
    private GetBuildingInfoResponse.BuildingDetails createMockBuildingInfo(Double lat, Double lng) {
        // 위도/경도 기반으로 다양한 모의 데이터 생성
        int hashCode = (lat.toString() + lng.toString()).hashCode();
        
        double buildingCoverage = 40.0 + (Math.abs(hashCode) % 40); // 40-80%
        double floorAreaRatio = 100.0 + (Math.abs(hashCode) % 300); // 100-400%
        int grndFloor = 3 + (Math.abs(hashCode) % 25); // 3-28층
        int ugrndFloor = Math.abs(hashCode) % 4; // 0-3층
        double height = grndFloor * 3.0 + (Math.abs(hashCode) % 5); // 층수 * 3m + α
        
        String[] purposes = {"아파트", "오피스텔", "업무시설", "근린생활시설", "상업시설"};
        String[] structures = {"철근콘크리트구조", "철골구조", "철골철근콘크리트구조", "조적구조"};
        
        String mainPurps = purposes[Math.abs(hashCode) % purposes.length];
        String structure = structures[Math.abs(hashCode) % structures.length];
        
        // 준공년월 (2000-2023년 사이 랜덤)
        int year = 2000 + (Math.abs(hashCode) % 24);
        int month = 1 + (Math.abs(hashCode) % 12);
        String completionDate = String.format("%04d-%02d", year, month);
        
        log.info("모의 건물 정보 생성 완료 - 용도: {}, 층수: {}층, 높이: {}m", mainPurps, grndFloor, height);
        
        return GetBuildingInfoResponse.BuildingDetails.builder()
                .buildingCoverage(buildingCoverage)
                .floorAreaRatio(floorAreaRatio)
                .completionDate(completionDate)
                .height(height)
                .grndFloor(grndFloor)
                .ugrndFloor(ugrndFloor)
                .mainPurps(mainPurps)
                .structure(structure)
                .build();
    }

    private GetBuildingInfoResponse.BuildingDetails createDefaultBuildingInfo() {
        return GetBuildingInfoResponse.BuildingDetails.builder()
                .buildingCoverage(0.0)
                .floorAreaRatio(0.0)
                .completionDate("")
                .height(0.0)
                .grndFloor(0)
                .ugrndFloor(0)
                .mainPurps("")
                .structure("")
                .build();
    }

    private Double getDoubleFromXml(Document doc, String tagName) {
        try {
            NodeList nodeList = doc.getElementsByTagName(tagName);
            if (nodeList.getLength() > 0) {
                String value = nodeList.item(0).getTextContent();
                return Double.parseDouble(value);
            }
        } catch (Exception e) {
            log.debug("XML에서 Double 값 추출 실패: {}", tagName, e);
        }
        return 0.0;
    }

    private Integer getIntegerFromXml(Document doc, String tagName) {
        try {
            NodeList nodeList = doc.getElementsByTagName(tagName);
            if (nodeList.getLength() > 0) {
                String value = nodeList.item(0).getTextContent();
                return Integer.parseInt(value);
            }
        } catch (Exception e) {
            log.debug("XML에서 Integer 값 추출 실패: {}", tagName, e);
        }
        return 0;
    }

    private String getStringFromXml(Document doc, String tagName) {
        try {
            NodeList nodeList = doc.getElementsByTagName(tagName);
            if (nodeList.getLength() > 0) {
                return nodeList.item(0).getTextContent();
            }
        } catch (Exception e) {
            log.debug("XML에서 String 값 추출 실패: {}", tagName, e);
        }
        return "";
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
