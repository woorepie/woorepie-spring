package com.piehouse.woorepie.notice.service.implement;

import com.piehouse.woorepie.estate.entity.Estate;
import com.piehouse.woorepie.estate.repository.EstateRepository;
import com.piehouse.woorepie.global.exception.CustomException;
import com.piehouse.woorepie.global.exception.ErrorCode;
import com.piehouse.woorepie.notice.dto.request.CreateNoticeRequest;
import com.piehouse.woorepie.notice.dto.request.ModifyNoticeRequest;
import com.piehouse.woorepie.notice.dto.response.GetNoticeDetailsResponse;
import com.piehouse.woorepie.notice.dto.response.GetNoticeSimpleResponse;
import com.piehouse.woorepie.notice.entity.Notice;
import com.piehouse.woorepie.notice.repository.NoticeRepository;
import com.piehouse.woorepie.notice.service.NoticeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NoticeServiceImpl implements NoticeService {

    private final NoticeRepository noticeRepository;

    @Override
    @Transactional(readOnly = true)
    public List<GetNoticeSimpleResponse> getNoticeList() {

        List<Notice> noticeList = noticeRepository.findAllWithEstateOrderByNoticeDateDesc();

        return noticeList.stream()
                .map(notice -> GetNoticeSimpleResponse.builder()
                        .noticeId(notice.getNoticeId())
                        .estateId(notice.getEstate().getEstateId())
                        .estateName(notice.getEstate().getEstateName())
                        .noticeTitle(notice.getNoticeTitle())
                        .noticeDate(notice.getNoticeDate())
                        .build()
                )
                .collect(Collectors.toList());

    }


    @Override
    @Transactional(readOnly = true)
    public GetNoticeDetailsResponse getNoticeDetails(Long noticeId) {

        Notice notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));

        return GetNoticeDetailsResponse.builder()
                .noticeId(notice.getNoticeId())
                .estateId(notice.getEstate().getEstateId())
                .estateName(notice.getEstate().getEstateName())
                .noticeTitle(notice.getNoticeTitle())
                .noticeContent(notice.getNoticeContent())
                .noticeFileUrl(notice.getNoticeFileUrl())
                .noticeDate(notice.getNoticeDate())
                .build();

    }

}
