package com.piehouse.woorepie.global.openai.service;

public interface OpenaiService {
    String summarize(String name, String address, double lat, double lng);

    String findNews(String name, String address);
}
