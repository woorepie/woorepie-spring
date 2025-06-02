package com.piehouse.woorepie.trade.service.implement;

import com.piehouse.woorepie.customer.entity.Customer;
import com.piehouse.woorepie.customer.repository.CustomerRepository;
import com.piehouse.woorepie.estate.entity.Estate;
import com.piehouse.woorepie.estate.repository.EstateRepository;
import com.piehouse.woorepie.global.exception.CustomException;
import com.piehouse.woorepie.global.exception.ErrorCode;
import com.piehouse.woorepie.global.kafka.dto.OrderCreatedEvent;
import com.piehouse.woorepie.notification.service.NotificationService;
import com.piehouse.woorepie.trade.dto.request.RedisCustomerTradeValue;
import com.piehouse.woorepie.trade.dto.request.RedisEstateTradeValue;
import com.piehouse.woorepie.trade.repository.RedisTradeRepository;
import com.piehouse.woorepie.trade.service.TradeRedisService;
import com.piehouse.woorepie.trade.service.TradeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class TradeRedisServiceImpl implements TradeRedisService {

    private final RedisTradeRepository redisRepository;
    private final TradeService tradeService;
    private final EstateRepository estateRepository;
    private final CustomerRepository customerRepository;
    private final RedissonClient redissonClient;
    private final NotificationService notificationService;

    // 매물과 고객 기준 매수 주문 동시 저장
    @Override
    public void saveBuyOrder(Long estateId, Long customerId, int tokenAmount, int tokenPrice) {
        long timestamp = System.currentTimeMillis();
        RedisEstateTradeValue estateOrder = new RedisEstateTradeValue(customerId, tokenAmount, tokenPrice, timestamp);
        RedisCustomerTradeValue customerOrder = new RedisCustomerTradeValue(estateId, tokenAmount, tokenPrice, timestamp);

        // Lua 스크립트로 원자적 저장
        redisRepository.saveOrUpdateBuyOrder(estateOrder, estateId, customerOrder, customerId);
    }

    // 매물과 고객 기준 매도 주문 동시 저장
    @Override
    public void saveSellOrder(Long estateId, Long customerId, int tokenAmount, int tokenPrice) {
        long timestamp = System.currentTimeMillis();
        RedisEstateTradeValue estateOrder = new RedisEstateTradeValue(customerId, -tokenAmount, tokenPrice, timestamp);
        RedisCustomerTradeValue customerOrder = new RedisCustomerTradeValue(estateId, -tokenAmount, tokenPrice, timestamp);

        // Lua 스크립트로 원자적 저장
        redisRepository.saveOrUpdateSellOrder(estateOrder, estateId, customerOrder, customerId);
    }

    // 매물 기준 매수 주문 전체 조회 (시간순)
    @Override
    public List<RedisEstateTradeValue> getEstateBuyOrders(Long estateId) {
        return redisRepository.getEstateBuyOrders(estateId);
    }

    // 매물 기준 매도 주문 전체 조회 (시간순)
    @Override
    public List<RedisEstateTradeValue> getEstateSellOrders(Long estateId) {
        return redisRepository.getEstateSellOrders(estateId);
    }

    // 고객 기준 매수 주문 전체 조회 (시간순)
    @Override
    public List<RedisCustomerTradeValue> getCustomerBuyOrders(Long customerId) {
        return redisRepository.getCustomerBuyOrders(customerId);
    }

    // 고객 기준 매도 주문 전체 조회 (시간순)
    @Override
    public List<RedisCustomerTradeValue> getCustomerSellOrders(Long customerId) {
        return redisRepository.getCustomerSellOrders(customerId);
    }

    // 매물 기준 가장 먼저 들어온 매수 주문 꺼내기 + 고객 기준에서도 함께 삭제
    @Override
    public RedisEstateTradeValue popOldestBuyOrderFromBoth(Long estateId) {
        return redisRepository.popOldestBuyOrderFromBoth(estateId);
    }

    // 매물 기준 가장 먼저 들어온 매도 주문 꺼내기 + 고객 기준에서도 함께 삭제
    @Override
    public RedisEstateTradeValue popOldestSellOrderFromBoth(Long estateId) {
        return redisRepository.popOldestSellOrderFromBoth(estateId);
    }

    // 매수 주문 처리
    @Override
    public void matchNewBuyOrder(OrderCreatedEvent event) {
        // Lua 스크립트로 원자적 저장
        saveBuyOrder(event.getEstateId(), event.getCustomerId(), event.getTradeTokenAmount(), event.getTokenPrice());

        // 락을 사용해 매칭 로직 실행
        processMatchingWithLock(event.getEstateId());
    }

    // 매도 주문 처리
    @Override
    public void matchNewSellOrder(OrderCreatedEvent event) {
        // Lua 스크립트로 원자적 저장
        saveSellOrder(event.getEstateId(), event.getCustomerId(), -event.getTradeTokenAmount(), event.getTokenPrice());
        
        // 락을 사용해 매칭 로직 실행
        processMatchingWithLock(event.getEstateId());
    }

    // 분산 락 적용해 매칭 로직
    void processMatchingWithLock(Long estateId) {
        String lockKey = "TRADE_LOCK:" + estateId;
        RLock lock = redissonClient.getFairLock(lockKey); // FairLock 사용 (FIFO 보장)

        boolean lockAcquired = false;
        try {
            // 500ms 동안 락 획득 시도, 10초 후 자동 해제
            lockAcquired = lock.tryLock(500, 10_000, TimeUnit.MILLISECONDS);
            if (lockAcquired) {
                matchAllPossibleOrders(estateId);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("락 획득 중단: estateId={}", estateId, e);
        } finally {
            if (lockAcquired && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Override
    public void matchAllPossibleOrders(Long estateId) {
        while (true) {
            // 1. 가장 오래된 매수/매도 주문 꺼내기
            RedisEstateTradeValue buyOrder = redisRepository.popOldestBuyOrderFromBoth(estateId);
            RedisEstateTradeValue sellOrder = redisRepository.popOldestSellOrderFromBoth(estateId);

            // 2. 둘 중 하나라도 없으면 종료
            if (buyOrder == null || sellOrder == null) {
                if (buyOrder != null) reinsertBuyOrder(estateId, buyOrder);
                if (sellOrder != null) reinsertSellOrder(estateId, sellOrder);
                break;
            }

            // 3. 실제 매칭 처리 (부분 체결 포함)
            processMatch(estateId, buyOrder, sellOrder);
        }
    }

    // 공통 매칭 로직 (매수 주문과 매도 주문 체결)
    private void processMatch(Long estateId, RedisEstateTradeValue buyOrder, RedisEstateTradeValue sellOrder) {
        // 매칭 수량 계산 (최소값)
        int buyAmount = buyOrder.getTradeTokenAmount();
        int sellAmount = -sellOrder.getTradeTokenAmount(); // 양수로 변환
        int matchAmount = Math.min(buyAmount, sellAmount);

        // 부분 체결 처리
        int buyRemaining = buyAmount - matchAmount;
        int sellRemaining = sellAmount - matchAmount;

        // 매수 주문 처리
        if (buyRemaining > 0) {
            // 부분 체결된 매수 주문 다시 저장
            RedisEstateTradeValue updatedBuyOrder = new RedisEstateTradeValue(
                    buyOrder.getCustomerId(),
                    buyRemaining,
                    buyOrder.getTokenPrice(),
                    buyOrder.getTimestamp() // 원래 타임스탬프 유지
            );

            RedisCustomerTradeValue updatedCustomerBuyOrder = new RedisCustomerTradeValue(
                    estateId,
                    buyRemaining,
                    buyOrder.getTokenPrice(),
                    buyOrder.getTimestamp() // 원래 타임스탬프 유지
            );

            redisRepository.saveOrUpdateBuyOrder(updatedBuyOrder, estateId, updatedCustomerBuyOrder, buyOrder.getCustomerId());
        }

        // 매도 주문 처리
        if (sellRemaining > 0) {
            // 부분 체결된 매도 주문 다시 저장
            RedisEstateTradeValue updatedSellOrder = new RedisEstateTradeValue(
                    sellOrder.getCustomerId(),
                    -sellRemaining, // 매도는 음수로 저장
                    sellOrder.getTokenPrice(),
                    sellOrder.getTimestamp() // 원래 타임스탬프 유지
            );

            RedisCustomerTradeValue updatedCustomerSellOrder = new RedisCustomerTradeValue(
                    estateId,
                    -sellRemaining, // 매도는 음수로 저장
                    sellOrder.getTokenPrice(),
                    sellOrder.getTimestamp() // 원래 타임스탬프 유지
            );

            redisRepository.saveOrUpdateSellOrder(updatedSellOrder, estateId, updatedCustomerSellOrder, sellOrder.getCustomerId());
        }

        saveTradeTransaction(estateId, sellOrder.getCustomerId(), buyOrder.getCustomerId(), matchAmount, sellOrder.getTokenPrice());
    }

    // 체결 내역 저장
    private void saveTradeTransaction(Long estateId, Long sellerId, Long buyerId, int amount, int price) {
        // 필요한 엔티티들 조회
        Estate estate = estateRepository.findById(estateId)
                .orElseThrow(() -> new CustomException(ErrorCode.ESTATE_NOT_FOUND));

        Customer seller = customerRepository.findById(sellerId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        Customer buyer = customerRepository.findById(buyerId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 거래 내역 저장
        tradeService.saveTrade(estate, seller, buyer, amount, price);
    }

    // pop한 매수 주문 원래 타임스탬프 유지해서 저장
    public void reinsertBuyOrder(Long estateId, RedisEstateTradeValue buyOrder) {
        Long customerId = buyOrder.getCustomerId();

        RedisCustomerTradeValue customerBuyOrder = new RedisCustomerTradeValue(
                estateId,
                buyOrder.getTradeTokenAmount(),
                buyOrder.getTokenPrice(),
                buyOrder.getTimestamp()
        );
        redisRepository.saveOrUpdateBuyOrder(buyOrder, estateId, customerBuyOrder, customerId);
    }

    // pop한 매도 주문 원래 타임스탬프 유지해서 저장
    public void reinsertSellOrder(Long estateId, RedisEstateTradeValue sellOrder) {
        Long customerId = sellOrder.getCustomerId();

        RedisCustomerTradeValue customerSellOrder = new RedisCustomerTradeValue(
                estateId,
                sellOrder.getTradeTokenAmount(),
                sellOrder.getTokenPrice(),
                sellOrder.getTimestamp()
        );

        redisRepository.saveOrUpdateSellOrder(sellOrder, estateId, customerSellOrder, customerId);
    }
}
