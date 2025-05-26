package com.piehouse.woorepie.global.kafka.service;


import java.util.Map;

public interface AuthService {

    Map<String, Object> getAuthStatus(Object session);

}
