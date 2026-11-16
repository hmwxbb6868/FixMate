package com.fixmate.service.user.serviceImpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fixmate.dto.user.RepairFormMetaVO;
import com.fixmate.entity.ApplianceBrandEntity;
import com.fixmate.entity.ApplianceModelEntity;
import com.fixmate.entity.SafetyRuleEntity;
import com.fixmate.mapper.SafetyRuleMapper;
import com.fixmate.service.common.DeviceCatalogService;
import com.fixmate.service.user.UserMetaService;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class UserMetaServiceImpl implements UserMetaService {

    private static final List<RepairFormMetaVO.EnumOptionVO> APPLIANCE_TYPES = List.of(
            new RepairFormMetaVO.EnumOptionVO("AIR_CONDITIONER", "空调"),
            new RepairFormMetaVO.EnumOptionVO("REFRIGERATOR", "冰箱"),
            new RepairFormMetaVO.EnumOptionVO("WASHING_MACHINE", "洗衣机"),
            new RepairFormMetaVO.EnumOptionVO("AIR_SOURCE_HEAT_PUMP", "空气能"));

    private final DeviceCatalogService deviceCatalogService;
    private final SafetyRuleMapper safetyRuleMapper;

    public UserMetaServiceImpl(DeviceCatalogService deviceCatalogService, SafetyRuleMapper safetyRuleMapper) {
        this.deviceCatalogService = deviceCatalogService;
        this.safetyRuleMapper = safetyRuleMapper;
    }

    @Override
    public RepairFormMetaVO getRepairFormMeta() {
        List<ApplianceBrandEntity> brands = deviceCatalogService.listEnabledBrands();
        Map<Long, String> brandCodeById = brands.stream()
                .collect(Collectors.toMap(ApplianceBrandEntity::getId, ApplianceBrandEntity::getCode));

        List<RepairFormMetaVO.BrandOptionVO> brandOptions = brands.stream()
                .map(b -> new RepairFormMetaVO.BrandOptionVO(b.getId(), b.getCode(), b.getName()))
                .toList();

        List<RepairFormMetaVO.ModelOptionVO> modelOptions = deviceCatalogService.listSelectableModels().stream()
                .map(m -> new RepairFormMetaVO.ModelOptionVO(m.getId(), m.getBrandId(),
                        brandCodeById.get(m.getBrandId()), m.getApplianceType(), m.getModelCode(),
                        m.getDisplayName(), m.getCoverageStatus()))
                .toList();

        List<RepairFormMetaVO.EnumOptionVO> safetyQuestions = safetyRuleMapper.selectList(
                        new LambdaQueryWrapper<SafetyRuleEntity>()
                                .eq(SafetyRuleEntity::getStatus, "ENABLED")
                                .orderByAsc(SafetyRuleEntity::getCode))
                .stream()
                .map(r -> new RepairFormMetaVO.EnumOptionVO(r.getCode(), r.getTitle()))
                .toList();

        return new RepairFormMetaVO(APPLIANCE_TYPES, brandOptions, modelOptions, safetyQuestions);
    }
}
