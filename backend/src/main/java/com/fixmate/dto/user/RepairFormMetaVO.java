package com.fixmate.dto.user;

import java.util.List;

public record RepairFormMetaVO(
        List<EnumOptionVO> applianceTypes,
        List<BrandOptionVO> brands,
        List<ModelOptionVO> models,
        List<EnumOptionVO> safetyQuestions) {

    public record EnumOptionVO(String code, String label) {
    }

    public record BrandOptionVO(Long id, String code, String name) {
    }

    public record ModelOptionVO(Long id, Long brandId, String brandCode, String applianceType,
                                String modelCode, String displayName, String coverageStatus) {
    }
}
