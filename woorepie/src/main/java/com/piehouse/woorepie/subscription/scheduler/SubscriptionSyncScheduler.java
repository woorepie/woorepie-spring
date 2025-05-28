package com.piehouse.woorepie.subscription.scheduler;

import com.piehouse.woorepie.estate.entity.Estate;
import com.piehouse.woorepie.estate.entity.SubState;
import com.piehouse.woorepie.estate.repository.EstateRepository;
import com.piehouse.woorepie.subscription.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SubscriptionSyncScheduler {
    private final EstateRepository estateRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final StringRedisTemplate redisTemplate;

    @Scheduled(cron = "0 0 3 * * *") // 매일 새벽 3시 실행
    public void validateTokenConsistency() {
        // 1. 진행 중인(subState=RUNNING) 매물만 조회
        List<Estate> runningEstates = estateRepository.findBySubState(SubState.RUNNING);

        runningEstates.forEach(estate -> {
            String redisKey = String.format("subscription:%s:remaining-tokens", estate.getEstateId());
            String redisValue = redisTemplate.opsForValue().get(redisKey);

            if (redisValue == null) {
                log.warn("Redis 키 없음 - estateId: {}", estate.getEstateId());
                return;
            }

            int redisRemaining = Integer.parseInt(redisValue);
            int totalSubscribed = subscriptionRepository.sumSubTokenAmountByEstateId(estate.getEstateId());
            int totalTokens = estate.getTokenAmount(); // tokenAmount 필드 사용

            if (redisRemaining != (totalTokens - totalSubscribed)) {
                log.error("토큰 불일치 - estateId: {}, Redis: {}, PostgreSQL 계산: {}",
                        estate.getEstateId(), redisRemaining, (totalTokens - totalSubscribed));

                // 자동 보정
                redisTemplate.opsForValue().set(
                        redisKey,
                        String.valueOf(totalTokens - totalSubscribed)
                );
            }
        });
    }
}
