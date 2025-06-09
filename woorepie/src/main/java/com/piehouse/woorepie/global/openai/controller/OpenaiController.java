package com.piehouse.woorepie.global.openai.controller;

import com.piehouse.woorepie.global.openai.dto.AnalyzeRequest;
import com.piehouse.woorepie.global.openai.dto.NewsRequest;
import com.piehouse.woorepie.global.openai.service.OpenaiService;
import com.piehouse.woorepie.global.response.ApiResponse;
import com.piehouse.woorepie.global.response.ApiResponseUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/news")
@RequiredArgsConstructor
public class OpenaiController {

    private final OpenaiService openAiService;

    @PostMapping("/summarize")
    public ResponseEntity<ApiResponse<String>> summarize(@RequestBody AnalyzeRequest request, HttpServletRequest httpRequest) {
        String result = openAiService.summarize(
                request.getEstateName(),
                request.getAddress(),
                request.getLat(),
                request.getLng()
        );
        return ApiResponseUtil.success(result, httpRequest);
    }

    @PostMapping("/findnews")
    public ResponseEntity<ApiResponse<String>> findNews(@RequestBody NewsRequest request, HttpServletRequest httpRequest) {
        String result = openAiService.findNews(request.getEstateName(), request.getAddress());
        return ApiResponseUtil.success(result, httpRequest);
    }
}