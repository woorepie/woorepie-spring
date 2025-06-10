package com.piehouse.woorepie.global.service.implement;

import com.piehouse.woorepie.global.dto.request.SmsCodeRequest;
import com.piehouse.woorepie.global.dto.request.SmsVerifyRequest;
import com.piehouse.woorepie.global.exception.CustomException;
import com.piehouse.woorepie.global.exception.ErrorCode;
import com.piehouse.woorepie.global.service.SmsService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import net.nurigo.sdk.NurigoApp;
import net.nurigo.sdk.message.model.Message;
import net.nurigo.sdk.message.request.SingleMessageSendingRequest;
import net.nurigo.sdk.message.response.SingleMessageSentResponse;
import net.nurigo.sdk.message.service.DefaultMessageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;

@Service
@RequiredArgsConstructor
public class SmsServiceImpl implements SmsService {

    @Value("${coolsms.fromnumber}")
    private String fromNumber;

    @Value("${coolsms.apikey}")
    private String apiKey;

    @Value("${coolsms.apisecret}")
    private String apiSecret;

    private static final String REDIS_SMS_AUTH_KEY_PREFIX = "sms:auth:";
    private final RedisTemplate<String, String> redisStringTemplate;
    private DefaultMessageService messageService;
    private final SecureRandom secureRandom = new SecureRandom();

    @PostConstruct
    public void init() {
        this.messageService = NurigoApp.INSTANCE
                .initialize(apiKey, apiSecret, "https://api.coolsms.co.kr");
    }


    @Override
    public void createSmsAuth(SmsCodeRequest smsCodeRequest) {
        try {
            String code = String.valueOf(secureRandom.nextInt(900_000) + 100_000);

            // 인증번호 redis에 저장
            redisSmsCode(smsCodeRequest.getPhoneNumber(), code);

            // 문자 전송
            String content = "[woorepie]\n 인증번호는 "+code+" 입니다.";
            sendSms(smsCodeRequest.getPhoneNumber(), content);
        } catch (Exception e) {
            throw new CustomException(ErrorCode.INTERNAL_ERROR);
        }
    }

    //문자 전송
    @Override
    public void sendSms(String toNumber, String content) {
        try {
            Message message = new Message();
            message.setFrom(fromNumber);
            message.setTo(toNumber);
            message.setText(content);

            SingleMessageSentResponse response = this.messageService.sendOne(new SingleMessageSendingRequest(message));
            System.out.println(response);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            throw new CustomException(ErrorCode.INTERNAL_ERROR);
        }
    }

    // 인증번호 redis에 3분동안 저장
    @Override
    public void redisSmsCode(String phoneNumber, String code) {
        try {
            String key = REDIS_SMS_AUTH_KEY_PREFIX + phoneNumber;
            redisStringTemplate.opsForValue().set(key, code, Duration.ofMinutes(30));
        }catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    @Override
    public Boolean isSmsCodeValid(SmsVerifyRequest smsVerifyRequest) {
        try {
            String key = REDIS_SMS_AUTH_KEY_PREFIX + smsVerifyRequest.getPhoneNumber();
            ValueOperations<String, String> ops = redisStringTemplate.opsForValue();

            // Redis에서 꺼내기
            String cached = ops.get(key);

            System.out.println(cached);
            // 없거나 만료
            if (cached == null) {
                throw new CustomException(ErrorCode.SMS_CODE_EXPIRED);
            }

            // 일치
            if (!cached.equals(smsVerifyRequest.getCode())) {
                throw new CustomException(ErrorCode.SMS_CODE_INVALID);
            }

            redisStringTemplate.delete(key);
            return true;
        } catch (CustomException ce) {
            throw ce;
        }catch (Exception e) {
            throw new CustomException(ErrorCode.INTERNAL_ERROR);
        }

    }

}
