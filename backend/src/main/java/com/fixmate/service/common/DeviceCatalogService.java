package com.fixmate.service.common;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fixmate.entity.ApplianceBrandEntity;
import com.fixmate.entity.ApplianceModelEntity;
import com.fixmate.mapper.ApplianceBrandMapper;
import com.fixmate.mapper.ApplianceModelMapper;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class DeviceCatalogService {

    private static final Set<String> SELECTABLE_COVERAGE = Set.of("REGISTERED", "PENDING_SOURCE", "COVERED");

    private final ApplianceBrandMapper brandMapper;
    private final ApplianceModelMapper modelMapper;

    public DeviceCatalogService(ApplianceBrandMapper brandMapper, ApplianceModelMapper modelMapper) {
        this.brandMapper = brandMapper;
        this.modelMapper = modelMapper;
    }

    public List<ApplianceBrandEntity> listEnabledBrands() {
        return brandMapper.selectList(new LambdaQueryWrapper<ApplianceBrandEntity>()
                .eq(ApplianceBrandEntity::getStatus, "ENABLED")
                .orderByAsc(ApplianceBrandEntity::getSortOrder));
    }

    /**
     * 用户可选型号：品牌启用、型号启用、覆盖状态属于可选集合。
     */
    public List<ApplianceModelEntity> listSelectableModels() {
        Set<Long> enabledBrandIds = listEnabledBrands().stream()
                .map(ApplianceBrandEntity::getId)
                .collect(Collectors.toSet());
        if (enabledBrandIds.isEmpty()) {
            return List.of();
        }
        return modelMapper.selectList(new LambdaQueryWrapper<ApplianceModelEntity>()
                        .eq(ApplianceModelEntity::getStatus, "ENABLED")
                        .in(ApplianceModelEntity::getCoverageStatus, SELECTABLE_COVERAGE)
                        .in(ApplianceModelEntity::getBrandId, enabledBrandIds)
                        .orderByAsc(ApplianceModelEntity::getBrandId)
                        .orderByAsc(ApplianceModelEntity::getModelCode));
    }

    public ApplianceModelEntity requireSelectableModel(Long modelId) {
        ApplianceModelEntity model = modelMapper.selectById(modelId);
        if (model == null || !"ENABLED".equals(model.getStatus())
                || !SELECTABLE_COVERAGE.contains(model.getCoverageStatus())) {
            throw com.fixmate.exception.BusinessException.validation("型号不可选");
        }
        return model;
    }
}
