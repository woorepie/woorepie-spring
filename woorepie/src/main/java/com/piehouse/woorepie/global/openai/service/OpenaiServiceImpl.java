package com.piehouse.woorepie.global.openai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.MediaType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
@RequiredArgsConstructor
@Slf4j
public class OpenaiServiceImpl implements OpenaiService {

    @Value("${openai.api.key}")
    private String apiKey;

    private static final String API_URL = "https://api.openai.com/v1/chat/completions";

    @Override
    public String summarize(String name, String address, double lat, double lng) {
        OkHttpClient client = new OkHttpClient();

        ObjectMapper mapper = new ObjectMapper();
        try {
            // 메시지 구성
            String userContent = String.format("""
            넌 부동산 투자 유치를 위한 마케팅용 AI야. 부동산에 대해 긍정적인 내용만 요약해. 주변 개발 계획이나 인프라 등 긍정적인 요소 위주로, 호재 중심 뉴스만 제공해줘.

            - 부동산 이름: %s
            - 주소: %s
            - 위도: %f
            - 경도: %f

            결과는 다음 형식으로 작성해:
            1. 세 줄 요약
            2. 호재 여부 (항상 '예'라고 써)
            3. 판단 근거 (긍정적인 내용만 포함)
        """, name, address, lat, lng);

            // JSON 구성
            JsonNode rootNode = mapper.createObjectNode()
                    .put("model", "gpt-3.5-turbo")
                    .put("temperature", 0.7);

            JsonNode messagesNode = mapper.createArrayNode()
                    .add(mapper.createObjectNode()
                            .put("role", "system")
                            .put("content", "너는 부동산 투자 분석 전문가야."))
                    .add(mapper.createObjectNode()
                            .put("role", "user")
                            .put("content", userContent));

            ((com.fasterxml.jackson.databind.node.ObjectNode) rootNode).set("messages", messagesNode);

            String requestJson = mapper.writeValueAsString(rootNode);
            log.info("requestJson: {}", requestJson);

            // HTTP 요청
            Request request = new Request.Builder()
                    .url(API_URL)
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(requestJson, MediaType.parse("application/json")))
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    return "요약 실패: " + response.code();
                }

                if (response.body() == null) {
                    return "응답 없음";
                }

                String responseBody = response.body().string();
                log.info("response: {}", responseBody);

                JsonNode jsonNode = mapper.readTree(responseBody);
                JsonNode choices = jsonNode.get("choices");
                if (choices != null && choices.isArray() && choices.size() > 0) {
                    JsonNode message = choices.get(0).get("message");
                    if (message != null) {
                        return message.get("content").asText();
                    }
                }

                return "응답 형식 오류";
            }
        } catch (IOException e) {
            return "요약 중 에러 발생: " + e.getMessage();
        }
    }
}
