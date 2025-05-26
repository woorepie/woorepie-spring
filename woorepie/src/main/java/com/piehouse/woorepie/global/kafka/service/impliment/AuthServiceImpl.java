package com.piehouse.woorepie.global.kafka.service.impliment;

import com.piehouse.woorepie.global.kafka.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    public Map<String, Object> getAuthStatus(Object session) {
        Map<String, Object> result = new HashMap<>();

        if (session != null) {
            result.put("authenticated", true);
            result.put("user", session);
        } else {
            result.put("authenticated", false);
        }

        return result;

    }

}
