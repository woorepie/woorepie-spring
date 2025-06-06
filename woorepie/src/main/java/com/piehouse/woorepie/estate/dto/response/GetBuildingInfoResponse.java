package com.piehouse.woorepie.estate.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GetBuildingInfoResponse {

    private Integer price;  // 공시지가
    private BuildingDetails buildingInfo;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BuildingDetails {
        private Double buildingCoverage;  // 건폐율 (%)
        private Double floorAreaRatio;    // 용적률 (%)
        private String completionDate;    // 준공년월
        private Double height;           // 건물높이 (m)
        private Integer grndFloor;       // 지상층수
        private Integer ugrndFloor;      // 지하층수
        private String mainPurps;        // 주용도
        private String structure;        // 구조
    }
} 