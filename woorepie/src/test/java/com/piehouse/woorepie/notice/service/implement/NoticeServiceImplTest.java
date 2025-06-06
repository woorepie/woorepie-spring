package com.piehouse.woorepie.notice.service.implement;

import com.piehouse.woorepie.estate.entity.Estate;
import com.piehouse.woorepie.global.exception.CustomException;
import com.piehouse.woorepie.global.exception.ErrorCode;
import com.piehouse.woorepie.notice.dto.response.GetNoticeDetailsResponse;
import com.piehouse.woorepie.notice.dto.response.GetNoticeSimpleResponse;
import com.piehouse.woorepie.notice.entity.Notice;
import com.piehouse.woorepie.notice.repository.NoticeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("NoticeServiceImpl 단위테스트")
class NoticeServiceImplTest {

    @Mock
    private NoticeRepository noticeRepository;

    @InjectMocks
    private NoticeServiceImpl noticeService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    /**
     * [정상 케이스] 공시 목록 전체 조회
     * - 공시 엔티티 리스트를 최신순 정렬로 조회, DTO 매핑이 정상 동작하는지 검증
     */
    @Test
    @DisplayName("getNoticeList - 전체 공시 목록 조회 성공")
    void getNoticeList_success() {
        // given
        Estate estate = mock(Estate.class);
        when(estate.getEstateId()).thenReturn(100L);
        when(estate.getEstateName()).thenReturn("테스트부동산");

        Notice notice = mock(Notice.class);
        when(notice.getNoticeId()).thenReturn(1L);
        when(notice.getEstate()).thenReturn(estate);
        when(notice.getNoticeTitle()).thenReturn("공시 제목");
        when(notice.getNoticeDate()).thenReturn(LocalDateTime.of(2024, 6, 1, 10, 0));

        List<Notice> noticeList = List.of(notice);
        when(noticeRepository.findAllWithEstateOrderByNoticeDateDesc()).thenReturn(noticeList);

        // when
        List<GetNoticeSimpleResponse> responseList = noticeService.getNoticeList();

        // then
        assertThat(responseList).hasSize(1);
        GetNoticeSimpleResponse res = responseList.get(0);
        assertThat(res.getNoticeId()).isEqualTo(1L);
        assertThat(res.getEstateId()).isEqualTo(100L);
        assertThat(res.getEstateName()).isEqualTo("테스트부동산");
        assertThat(res.getNoticeTitle()).isEqualTo("공시 제목");
        assertThat(res.getNoticeDate()).isEqualTo(LocalDateTime.of(2024, 6, 1, 10, 0));
        verify(noticeRepository).findAllWithEstateOrderByNoticeDateDesc();
    }

    /**
     * [정상 케이스] 공시 상세 조회
     * - 공시 ID로 조회하여 상세 DTO 변환이 정상 동작하는지 검증
     */
    @Test
    @DisplayName("getNoticeDetails - 공시 상세 조회 성공")
    void getNoticeDetails_success() {
        // given
        Estate estate = mock(Estate.class);
        when(estate.getEstateId()).thenReturn(99L);
        when(estate.getEstateName()).thenReturn("우리빌라");

        Notice notice = mock(Notice.class);
        when(notice.getNoticeId()).thenReturn(11L);
        when(notice.getEstate()).thenReturn(estate);
        when(notice.getNoticeTitle()).thenReturn("상세공시");
        when(notice.getNoticeContent()).thenReturn("내용입니다.");
        when(notice.getNoticeFileUrl()).thenReturn("http://file.url/abc.png");
        when(notice.getNoticeDate()).thenReturn(LocalDateTime.of(2024, 6, 2, 9, 0));

        when(noticeRepository.findById(11L)).thenReturn(Optional.of(notice));

        // when
        GetNoticeDetailsResponse response = noticeService.getNoticeDetails(11L);

        // then
        assertThat(response.getNoticeId()).isEqualTo(11L);
        assertThat(response.getEstateId()).isEqualTo(99L);
        assertThat(response.getEstateName()).isEqualTo("우리빌라");
        assertThat(response.getNoticeTitle()).isEqualTo("상세공시");
        assertThat(response.getNoticeContent()).isEqualTo("내용입니다.");
        assertThat(response.getNoticeFileUrl()).isEqualTo("http://file.url/abc.png");
        assertThat(response.getNoticeDate()).isEqualTo(LocalDateTime.of(2024, 6, 2, 9, 0));
        verify(noticeRepository).findById(11L);
    }

    /**
     * [예외 케이스] 공시 상세 조회 - 존재하지 않는 경우
     * - 조회 결과가 없으면 CustomException이 발생하는지 검증
     */
    @Test
    @DisplayName("getNoticeDetails - 공시 미존재 예외")
    void getNoticeDetails_notFound() {
        // given
        when(noticeRepository.findById(99L)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> noticeService.getNoticeDetails(99L))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(ErrorCode.RESOURCE_NOT_FOUND.getMessage());
    }
}
