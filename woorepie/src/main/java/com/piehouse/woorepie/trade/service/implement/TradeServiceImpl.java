package com.piehouse.woorepie.trade.service.implement;

import com.piehouse.woorepie.customer.entity.Account;
import com.piehouse.woorepie.customer.entity.Customer;
import com.piehouse.woorepie.customer.repository.AccountRepository;
import com.piehouse.woorepie.customer.repository.CustomerRepository;
import com.piehouse.woorepie.estate.dto.RedisEstatePrice;
import com.piehouse.woorepie.estate.entity.Estate;
import com.piehouse.woorepie.estate.entity.EstateStatus;
import com.piehouse.woorepie.estate.repository.EstateRepository;
import com.piehouse.woorepie.global.exception.CustomException;
import com.piehouse.woorepie.global.exception.ErrorCode;
import com.piehouse.woorepie.global.kafka.dto.SubscriptionRequestEvent;
import com.piehouse.woorepie.global.kafka.dto.TransactionCreatedEvent;
import com.piehouse.woorepie.global.kafka.dto.OrderCreatedEvent;
import com.piehouse.woorepie.global.kafka.service.KafkaProducerService;
import com.piehouse.woorepie.subscription.entity.SubStatus;
import com.piehouse.woorepie.subscription.entity.Subscription;
import com.piehouse.woorepie.trade.dto.request.*;
import com.piehouse.woorepie.notification.service.NotificationService;
import com.piehouse.woorepie.trade.dto.request.BuyEstateRequest;
import com.piehouse.woorepie.trade.dto.request.RedisCustomerTradeValue;
import com.piehouse.woorepie.trade.dto.request.RedisEstateTradeValue;
import com.piehouse.woorepie.trade.dto.request.SellEstateRequest;
import com.piehouse.woorepie.trade.entity.Trade;
import com.piehouse.woorepie.trade.repository.RedisTradeRepository;
import com.piehouse.woorepie.trade.repository.TradeRepository;
import com.piehouse.woorepie.trade.service.TradeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.piehouse.woorepie.subscription.repository.SubscriptionRepository;
import com.piehouse.woorepie.estate.service.EstateRedisService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class TradeServiceImpl implements TradeService {

    private final TradeRepository tradeRepository;
    private final AccountRepository accountRepository;
    private final CustomerRepository customerRepository;
    private final RedisTradeRepository redisOrderRepository;
    private final KafkaProducerService kafkaProducerService;
    private final NotificationService notificationService;
    private final EstateRepository estateRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final EstateRedisService estateRedisService;

    @Override
    @Transactional
    public Trade saveTrade(Estate estate, Customer seller, Customer buyer, long tradeTokenAmount, long tokenPrice) {
        Customer persistedSeller = customerRepository.findById(seller.getCustomerId())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        Customer persistedBuyer = customerRepository.findById(buyer.getCustomerId())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // PostgreSQL 저장
        LocalDateTime tradeTime = LocalDateTime.now();
        // 1. PostgreSQL에 거래 내역 저장
        Trade trade = Trade.builder()
                .estate(estate)
                .seller(seller)
                .buyer(buyer)
                .tradeTokenAmount(tradeTokenAmount)
                .tokenPrice(tokenPrice)
                .tradeDate(tradeTime)
                .build();
        Trade savedTrade = tradeRepository.save(trade);

        // 2. Kafka로 거래 체결 이벤트 비동기 전송
        TransactionCreatedEvent event = createEvent(savedTrade);
        kafkaProducerService.sendTransactionCreated(event);

        // 3. 사용자에게 알림 전송
        // 매수자에게 알림 전송
        notificationService.sendTradeNotification(buyer, estate.getEstateName(), tokenPrice, tradeTokenAmount, tradeTime, true);
        // 매도자에게 알림 전송
        notificationService.sendTradeNotification(seller, estate.getEstateName(), tokenPrice, tradeTokenAmount, tradeTime, false);


        // 4. 판매자 계좌 업데이트
        Account sellerAccount = accountRepository.findByCustomerAndEstate(seller, estate)
                .orElseThrow(() -> new CustomException(ErrorCode.ACCOUNT_NON_EXIST));

        // 거래 금액 계산
        long tradeAmount = tradeTokenAmount * tokenPrice;

        long newTokenAmount = sellerAccount.getAccountTokenAmount() - tradeTokenAmount;
        long newTotalAmount = sellerAccount.getTotalAccountAmount() - tradeAmount;

        // 판매자 계좌 업데이트 - 토큰과 금액 모두 감소
        sellerAccount.updateTokenAmount(newTokenAmount)
                .updateTotalAmount(newTotalAmount);

        // 판매자 계좌 잔액 증가
        persistedSeller.increaseAccountBalance(tradeAmount);

        // 5. 구매자 계좌 업데이트
        Account buyerAccount = accountRepository.findByCustomerAndEstate(buyer, estate)
                .orElseGet(() -> {
                    // 새 계좌 생성 후 저장
                    Account newAccount = Account.builder()
                            .customer(buyer)
                            .estate(estate)
                            .accountTokenAmount(0L)
                            .totalAccountAmount(0L)
                            .build();
                    return accountRepository.save(newAccount);
                });

        // 구매자 계좌 업데이트 - 토큰과 금액 모두 증가
        buyerAccount.updateTokenAmount(buyerAccount.getAccountTokenAmount() + tradeTokenAmount)
                .updateTotalAmount(buyerAccount.getTotalAccountAmount() + tradeAmount);

        // 구매자 계좌 잔액 차감
        persistedBuyer.decreaseAccountBalance(tradeAmount);

        return savedTrade;

    }

    private TransactionCreatedEvent createEvent(Trade trade) {

        return TransactionCreatedEvent.builder()
                .estateId(trade.getEstate().getEstateId())
                .tradeId(trade.getTradeId())
                .sellerId(trade.getSeller().getCustomerId())
                .buyerId(trade.getBuyer().getCustomerId())
                .tokenPrice(trade.getTokenPrice())
                .tradeTokenAmount(trade.getTradeTokenAmount())
                .tradeDate(trade.getTradeDate())
                .build();

    }

    @Override
    public void buy(BuyEstateRequest request, Long customerId) {

        long amount = request.getTradeTokenAmount();
        long price = request.getTokenPrice();

        if (!isValidBuyRequest(customerId, amount, price)) {
            throw new CustomException(ErrorCode.INSUFFICIENT_CASH);
        }

        OrderCreatedEvent msg = OrderCreatedEvent.builder()
                .estateId(request.getEstateId())
                .customerId(customerId)
                .tokenPrice(price)
                .tradeTokenAmount(amount)
                .build();
        kafkaProducerService.sendOrderCreated(msg);
        log.info("[매수 Kafka 전송 완료] 고객: {}, 수량: {}, 가격: {}", customerId, amount, price);

    }

    private boolean isValidBuyRequest(Long customerId, long newTokenAmount, long newTokenPrice) {

        long newCost = newTokenAmount * newTokenPrice;
        long cumCost = getCumulativeBuyCost(customerId);

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new CustomException(ErrorCode.ACCOUNT_NON_EXIST));
        long balance = customer.getAccountBalance();

        log.info("[매수검증] 고객: {}, 기존: {}, 신규: {}, 합계: {}, 잔액: {}",
                customerId, cumCost, newCost, cumCost + newCost, balance);

        return balance >= cumCost + newCost;

    }

    private long getCumulativeBuyCost(Long customerId) {

        List<RedisCustomerTradeValue> orders = redisOrderRepository.getCustomerBuyOrders(customerId);
        if (orders == null || orders.isEmpty()) {
            log.info("[누적매수] 데이터없음 - customer:{}", customerId);
            return 0;
        }
        return orders.stream()
                .filter(Objects::nonNull)
                .mapToLong(o -> o.getTradeTokenAmount() * o.getTokenPrice())
                .sum();

    }

    @Override
    public void sell(SellEstateRequest request, Long customerId) {

        Long estateId = request.getEstateId();

        // 입력값이 양수더라도 매도(-)기 때문에 음수로 변환해줌
        long sellAmt = -Math.abs(request.getTradeTokenAmount());

        if (!isValidSellRequest(customerId, estateId, sellAmt)) {
            throw new CustomException(ErrorCode.INTERNAL_ERROR);
        }

        OrderCreatedEvent msg = OrderCreatedEvent.builder()
                .estateId(estateId)
                .customerId(customerId)
                .tokenPrice(request.getTokenPrice())
                .tradeTokenAmount(sellAmt)
                .build();
        kafkaProducerService.sendOrderCreated(msg);
        log.info("[매도 Kafka 전송 완료] 고객: {}, 부동산: {}, 수량: {}", customerId, estateId, sellAmt);

    }

    private boolean isValidSellRequest(Long customerId, Long estateId, long newSell) {

        log.info("inValidSellRequest는 들어옴");
        long cumSell = getCumulativeSellAmount(customerId, estateId);
        long owned = accountRepository
                .findByCustomer_CustomerIdAndEstate_EstateId(customerId, estateId)
                .orElseThrow(() -> new CustomException(ErrorCode.TOKEN_NON_EXIST))
                .getAccountTokenAmount();

        log.info("[매도검증] customerId={}, estateId={}, 보유량={}, 누적매도량={}, 신규매도량={}, 검증합계={}",
                customerId,
                estateId,
                owned,
                cumSell,
                newSell,
                owned + cumSell + newSell
        );

        return owned + cumSell + newSell >= 0;

    }

    private long getCumulativeSellAmount(Long customerId, Long estateId) {

        log.info("getCumulativeSell 들어옴");
        List<RedisEstateTradeValue> orders = redisOrderRepository.getEstateSellOrders(estateId);
        log.info("매도 요청 누적합 계산을 위한 주문 리스트 확인: {}", orders);

        if (orders == null || orders.isEmpty()) {
            log.info("[누적매도] 데이터없음 - estate:{}, customer:{}", estateId, customerId);
            return 0;
        }

        return orders.stream()
                .filter(Objects::nonNull)
                .filter(o -> o.getCustomerId() != null)
                .filter(o -> o.getCustomerId().equals(customerId) && o.getTradeTokenAmount() < 0)
                .mapToLong(RedisEstateTradeValue::getTradeTokenAmount)
                .sum();

    }

    @Override
    @Transactional
    public void createSubscription(CreateSubscriptionTradeRequest request, Long customerId) {
        log.info("청약 신청 serviceimpl createSubscription 들어옴");
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        Long estateId = request.getEstateId();
        long requestAmount = request.getSubAmount();
        LocalDateTime now = LocalDateTime.now();

        log.info("[청약 신청 시작] customerId: {}, estateId: {}, 신청수량: {}", customerId, estateId, requestAmount);

        // 1. PostgreSQL 조회해서 청약 기간 검증
        Estate estate = estateRepository.findById(estateId)
                .orElseThrow(() -> new CustomException(ErrorCode.ESTATE_NOT_FOUND));
        LocalDateTime subStart = estate.getSubStartDate();
        LocalDateTime subEnd = estate.getSubEndDate();
        if (subStart == null || subEnd == null || now.isBefore(subStart) || now.isAfter(subEnd)) {
            throw new CustomException(ErrorCode.SUBSCRIPTION_PERIOD_INVALID);
        }

        // Redis에서 1토큰당 가격 조회
        RedisEstatePrice redisPrice = estateRedisService.getRedisEstatePrice(estateId);
        long tokenPrice = redisPrice.getEstateTokenPrice();
        long subscriptionCost = requestAmount * tokenPrice;

        // Redis에서 고객의 기존 매수 요청 금액 조회
        List<RedisCustomerTradeValue> orders = redisOrderRepository.getCustomerBuyOrders(customerId);
        long cumulativeBuyCost = orders == null ? 0 :
                orders.stream()
                        .filter(Objects::nonNull)
                        .mapToLong(o -> o.getTradeTokenAmount() * o.getTokenPrice())
                        .sum();

        long userBalance = customer.getAccountBalance();

        log.info("[청약 검증] customerId: {}, 기존매수금액: {}, 청약금액: {}, 총합: {}, 잔액: {}",
                customerId, cumulativeBuyCost, subscriptionCost, cumulativeBuyCost + subscriptionCost, userBalance);

        if (userBalance < cumulativeBuyCost + subscriptionCost) {
            throw new CustomException(ErrorCode.INSUFFICIENT_CASH);
        }

        // Kafka로 청약 요청 전송
        kafkaProducerService.sendSubscriptionRequest(
                SubscriptionRequestEvent.builder()
                        .customerId(customerId)
                        .estateId(estateId)
                        .tokenPrice(tokenPrice)
                        .amount(requestAmount)
                        .subscribeDate(now)
                        .build()
        );
    }

    // 청약 신청 처리 로직
    @Override
    @Transactional
    public void processSubscriptionRequest(Long estateId, Long customerId, long requestedAmount, long tokenPrice) {
        // 1. 매물 상태 확인 (RUNNING 상태만 허용)
        Estate estate = estateRepository.findById(estateId)
                .orElseThrow(() -> new CustomException(ErrorCode.ESTATE_NOT_FOUND));

        if (estate.getEstateStatus() != EstateStatus.RUNNING) {
            throw new CustomException(ErrorCode.ESTATE_NOT_RUNNING);
        }

        // 2. 사용자 존재 확인
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 3. 고객 계좌 차감
        long totalPrice = requestedAmount * tokenPrice;
        int updatedRows = customerRepository.decreaseBalance(customerId, totalPrice);

        if (updatedRows == 0) {
            throw new CustomException(ErrorCode.INSUFFICIENT_CASH);
        }

        // 4. 청약 기록 저장
        Subscription subscription = Subscription.builder()
                .estate(estate)
                .customer(customer)
                .subTokenAmount(requestedAmount)
                .subDate(LocalDateTime.now()) // 현재 시각 저장
                .subStatus(SubStatus.PENDING)
                .build();
        subscriptionRepository.save(subscription);
        log.info("청약 요청 DB에 저장 성공 - estateId: {}, customerId: {}", estateId, customerId);

        estateRedisService.decrementButNotNegative(estateId, requestedAmount);
    }

}

