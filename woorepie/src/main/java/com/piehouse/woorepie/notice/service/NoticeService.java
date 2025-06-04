package com.piehouse.woorepie.notice.service;

import com.piehouse.woorepie.notice.dto.request.CreateNoticeRequest;
import com.piehouse.woorepie.notice.dto.request.ModifyNoticeRequest;
import com.piehouse.woorepie.notice.dto.response.GetNoticeDetailsResponse;
import com.piehouse.woorepie.notice.dto.response.GetNoticeSimpleResponse;

import java.util.List;

public interface NoticeService {

    // 공시 리스트 조회
    List<GetNoticeSimpleResponse> getNoticeList();

    // 공시 상세 조회
    GetNoticeDetailsResponse getNoticeDetails(Long noticeId);

}
