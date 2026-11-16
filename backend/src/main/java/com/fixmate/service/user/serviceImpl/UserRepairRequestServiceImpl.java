package com.fixmate.service.user.serviceImpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fixmate.assembler.RepairRequestAssembler;
import com.fixmate.common.audit.AuditLogService;
import com.fixmate.dto.user.CancelRequestDTO;
import com.fixmate.dto.user.FileAssetVO;
import com.fixmate.dto.user.RepairDraftUpsertDTO;
import com.fixmate.dto.user.RepairRequestVO;
import com.fixmate.dto.user.RepairSubmitDTO;
import com.fixmate.dto.user.RepairSubmitResult;
import com.fixmate.dto.user.SafetyAnswersDTO;
import com.fixmate.dto.user.SafetyBlockVO;
import com.fixmate.entity.ApplianceBrandEntity;
import com.fixmate.entity.ApplianceModelEntity;
import com.fixmate.entity.CustomerAddressEntity;
import com.fixmate.entity.FileAssetEntity;
import com.fixmate.entity.RepairRequestEntity;
import com.fixmate.entity.RepairRequestMediaEntity;
import com.fixmate.entity.SafetyEventEntity;
import com.fixmate.entity.WorkOrderEntity;
import com.fixmate.entity.WorkOrderStatusLogEntity;
import com.fixmate.exception.BusinessException;
import com.fixmate.mapper.ApplianceBrandMapper;
import com.fixmate.mapper.ApplianceModelMapper;
import com.fixmate.mapper.CustomerAddressMapper;
import com.fixmate.mapper.FileAssetMapper;
import com.fixmate.mapper.RepairRequestMapper;
import com.fixmate.mapper.RepairRequestMediaMapper;
import com.fixmate.mapper.SafetyEventMapper;
import com.fixmate.mapper.WorkOrderMapper;
import com.fixmate.mapper.WorkOrderStatusLogMapper;
import com.fixmate.service.common.SafetyRuleEngine;
import com.fixmate.service.user.UserRepairRequestService;
import com.fixmate.storage.ObjectStorageService;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserRepairRequestServiceImpl implements UserRepairRequestService {

    private static final Set<String> APPLIANCE_TYPES = Set.of(
            "AIR_CONDITIONER", "REFRIGERATOR", "WASHING_MACHINE", "AIR_SOURCE_HEAT_PUMP");
    private static final Set<String> SELECTABLE_COVERAGE = Set.of("REGISTERED", "PENDING_SOURCE", "COVERED");
    private static final com.fixmate.service.common.BusinessNumberGenerator NUMBERS = new com.fixmate.service.common.BusinessNumberGenerator();

    private final RepairRequestMapper repairRequestMapper;
    private final RepairRequestMediaMapper mediaMapper;
    private final CustomerAddressMapper addressMapper;
    private final FileAssetMapper fileAssetMapper;
    private final ApplianceBrandMapper brandMapper;
    private final ApplianceModelMapper modelMapper;
    private final SafetyRuleEngine safetyRuleEngine;
    private final SafetyEventMapper safetyEventMapper;
    private final WorkOrderMapper workOrderMapper;
    private final WorkOrderStatusLogMapper statusLogMapper;
    private final RepairRequestAssembler assembler;
    private final ObjectMapper objectMapper;
    private final AuditLogService auditLogService;
    private final ObjectStorageService storageService;

    public UserRepairRequestServiceImpl(RepairRequestMapper repairRequestMapper,
                                        RepairRequestMediaMapper mediaMapper,
                                        CustomerAddressMapper addressMapper,
                                        FileAssetMapper fileAssetMapper,
                                        ApplianceBrandMapper brandMapper,
                                        ApplianceModelMapper modelMapper,
                                        SafetyRuleEngine safetyRuleEngine,
                                        SafetyEventMapper safetyEventMapper,
                                        WorkOrderMapper workOrderMapper,
                                        WorkOrderStatusLogMapper statusLogMapper,
                                        RepairRequestAssembler assembler,
                                        ObjectMapper objectMapper,
                                        AuditLogService auditLogService,
                                        ObjectStorageService storageService) {
        this.repairRequestMapper = repairRequestMapper;
        this.mediaMapper = mediaMapper;
        this.addressMapper = addressMapper;
        this.fileAssetMapper = fileAssetMapper;
        this.brandMapper = brandMapper;
        this.modelMapper = modelMapper;
        this.safetyRuleEngine = safetyRuleEngine;
        this.safetyEventMapper = safetyEventMapper;
        this.workOrderMapper = workOrderMapper;
        this.statusLogMapper = statusLogMapper;
        this.assembler = assembler;
        this.objectMapper = objectMapper;
        this.auditLogService = auditLogService;
        this.storageService = storageService;
    }

    @Override
    @Transactional
    public RepairRequestVO createDraft(Long userId, RepairDraftUpsertDTO dto) {
        RepairRequestEntity entity = new RepairRequestEntity();
        entity.setRepairNo(nextNo("RR"));
        entity.setUserId(userId);
        entity.setStatus("DRAFT");
        entity.setVersion(0);
        entity.setCreatedAt(now());
        entity.setUpdatedAt(now());
        applyDraft(entity, dto);
        repairRequestMapper.insert(entity);
        syncMedia(entity.getId(), userId, dto.getMediaFileIds());
        auditLogService.recordCustomer(userId, "REPAIR_DRAFT_CREATED", "REPAIR_REQUEST", entity.getId(),
                null, java.util.Map.of("repairNo", entity.getRepairNo()));
        return toVO(entity);
    }

    @Override
    @Transactional
    public RepairRequestVO updateDraft(Long userId, Long repairRequestId, RepairDraftUpsertDTO dto) {
        RepairRequestEntity entity = lockOwned(userId, repairRequestId);
        if (!"DRAFT".equals(entity.getStatus())) {
            throw BusinessException.stateConflict("仅草稿状态可修改");
        }
        if (dto.getExpectedVersion() == null) {
            throw BusinessException.validation("缺少 expectedVersion");
        }
        int expected = dto.getExpectedVersion();
        applyDraft(entity, dto);
        int rows = repairRequestMapper.update(null, new UpdateWrapper<RepairRequestEntity>()
                .eq("id", entity.getId())
                .eq("version", expected)
                .set("appliance_type", entity.getApplianceType())
                .set("brand_id", entity.getBrandId())
                .set("model_id", entity.getModelId())
                .set("model_text", entity.getModelText())
                .set("serial_number", entity.getSerialNumber())
                .set("symptom_description", entity.getSymptomDescription())
                .set("error_code_text", entity.getErrorCodeText())
                .set("occurred_at", entity.getOccurredAt())
                .set("safety_answers", entity.getSafetyAnswers())
                .set("address_id", entity.getAddressId())
                .set("coverage_status", entity.getCoverageStatus())
                .set("version", expected + 1)
                .set("updated_at", now()));
        if (rows == 0) {
            throw BusinessException.versionConflict("草稿已被修改，请刷新后重试");
        }
        if (dto.hasMediaFileIds()) {
            syncMedia(entity.getId(), userId, dto.getMediaFileIds());
        }
        auditLogService.recordCustomer(userId, "REPAIR_DRAFT_UPDATED", "REPAIR_REQUEST", entity.getId(),
                java.util.Map.of("version", expected), java.util.Map.of("version", expected + 1));
        return toVO(repairRequestMapper.selectById(entity.getId()));
    }

    @Override
    public RepairRequestVO get(Long userId, Long repairRequestId) {
        return toVO(requireOwned(userId, repairRequestId));
    }

    @Override public RepairRequestVO getWithoutMedia(Long userId,Long repairRequestId){return toVO(requireOwned(userId,repairRequestId),false);}

    @Override
    @Transactional
    public RepairSubmitResult submit(Long userId, Long repairRequestId, RepairSubmitDTO dto) {
        // 先锁报修聚合根，再锁文件行：修改草稿也走“报修行 → 文件行”，避免交叉等待
        RepairRequestEntity entity = lockOwned(userId, repairRequestId);
        if (!"DRAFT".equals(entity.getStatus())) {
            throw BusinessException.stateConflict("仅草稿状态可提交");
        }
        validateComplete(entity);
        CustomerAddressEntity address = addressMapper.selectOne(new LambdaQueryWrapper<CustomerAddressEntity>()
                .eq(CustomerAddressEntity::getId, entity.getAddressId()).last("FOR UPDATE"));
        if(address==null||!userId.equals(address.getUserId())||!"ENABLED".equals(address.getStatus()))
            throw BusinessException.forbidden("地址不可用");
        requireOwnedFiles(userId, mediaIds(entity.getId()));

        String text = joinText(entity);
        SafetyAnswersDTO answers = parseSafety(entity.getSafetyAnswers());
        List<SafetyRuleEngine.MatchedRule> matched = safetyRuleEngine.match(answers, text);
        int expected = dto.getExpectedVersion();

        if (!matched.isEmpty()) {
            SafetyEventEntity event = createSafetyEvent(entity, matched, text);
            WorkOrderEntity order = createEmergencyWorkOrder(userId, entity, address, event);
            int rows = repairRequestMapper.update(null, new UpdateWrapper<RepairRequestEntity>()
                    .eq("id", entity.getId())
                    .eq("version", expected)
                    .set("status", "HIGH_RISK_BLOCKED")
                    .set("address_snapshot", assembler.toJson(addressSnapshot(address)))
                    .set("contact_snapshot", assembler.toJson(contactSnapshot(address)))
                    .set("service_contact_snapshot", privateContactSnapshot(address))
                    .set("work_order_id", order.getId())
                    .set("risk_flags", assembler.toJson(matched.stream()
                            .map(SafetyRuleEngine.MatchedRule::ruleCode).toList()))
                    .set("version", expected + 1)
                    .set("updated_at", now()));
            if (rows == 0) {
                throw BusinessException.versionConflict("报修已被修改，请刷新后重试");
            }
            activateFiles(mediaIds(entity.getId()));
            auditLogService.recordCustomer(userId, "REPAIR_HIGH_RISK_BLOCKED", "REPAIR_REQUEST", entity.getId(),
                    java.util.Map.of("version", expected),
                    java.util.Map.of("version", expected + 1, "matchedRules",
                            matched.stream().map(SafetyRuleEngine.MatchedRule::ruleCode).toList()));
            auditLogService.recordSystem("WORK_ORDER_CREATED", "WORK_ORDER", order.getId(), null,
                    java.util.Map.of("status", order.getStatus(), "repairRequestId", entity.getId(),
                            "emergency", true));
            SafetyBlockVO block = assembler.toBlock(event, order, matched);
            return RepairSubmitResult.blocked(block);
        }

        int rows = repairRequestMapper.update(null, new UpdateWrapper<RepairRequestEntity>()
                .eq("id", entity.getId())
                .eq("version", expected)
                .set("status", "SUBMITTED")
                .set("service_contact_snapshot", privateContactSnapshot(address))
                .set("address_snapshot", assembler.toJson(addressSnapshot(address)))
                .set("contact_snapshot", assembler.toJson(contactSnapshot(address)))
                .set("version", expected + 1)
                .set("updated_at", now()));
        if (rows == 0) {
            throw BusinessException.versionConflict("报修已被修改，请刷新后重试");
        }
        activateFiles(mediaIds(entity.getId()));
        auditLogService.recordCustomer(userId, "REPAIR_SUBMITTED", "REPAIR_REQUEST", entity.getId(),
                java.util.Map.of("version", expected), java.util.Map.of("version", expected + 1, "status", "SUBMITTED"));
        return RepairSubmitResult.submitted(toVO(repairRequestMapper.selectById(entity.getId())));
    }

    @Override
    @Transactional
    public RepairRequestVO cancel(Long userId, Long repairRequestId, CancelRequestDTO dto) {
        RepairRequestEntity entity = lockOwned(userId, repairRequestId);
        if (entity.getWorkOrderId()!=null||!Set.of("DRAFT","SUBMITTED","TRIAGED","HUMAN_HANDOFF","COLLECTING").contains(entity.getStatus())) {
            throw BusinessException.stateConflict("当前状态不可取消");
        }
        int rows = repairRequestMapper.update(null, new UpdateWrapper<RepairRequestEntity>()
                .eq("id", entity.getId())
                .eq("version", dto.getExpectedVersion())
                .set("status", "CANCELLED")
                .set("version", dto.getExpectedVersion() + 1)
                .set("updated_at", now()));
        if (rows == 0) {
            throw BusinessException.versionConflict("报修已被修改，请刷新后重试");
        }
        auditLogService.recordCustomer(userId, "REPAIR_CANCELLED", "REPAIR_REQUEST", entity.getId(),
                java.util.Map.of("status", entity.getStatus(), "version", dto.getExpectedVersion()),
                java.util.Map.of("status", "CANCELLED", "version", dto.getExpectedVersion() + 1,
                        "reason", String.valueOf(dto.getReason())));
        return toVO(repairRequestMapper.selectById(entity.getId()));
    }

    // ---------------- helpers ----------------

    private void applyDraft(RepairRequestEntity entity, RepairDraftUpsertDTO dto) {
        boolean brandExplicitlyCleared = false;
        if (dto.hasField("applianceType")) {
            String type = dto.getApplianceType();
            if (type != null && !APPLIANCE_TYPES.contains(type)) {
                throw BusinessException.validation("不支持的设备类型");
            }
            entity.setApplianceType(type);
        }
        if (dto.hasField("brandId")) {
            if (dto.getBrandId() == null) {
                brandExplicitlyCleared = true;
                entity.setBrandId(null);
            } else {
                ApplianceBrandEntity brand = brandMapper.selectById(dto.getBrandId());
                if (brand == null || !"ENABLED".equals(brand.getStatus())) {
                    throw BusinessException.validation("品牌不可用");
                }
                entity.setBrandId(brand.getId());
            }
        }
        if (dto.hasField("modelId")) {
            if (dto.getModelId() == null) {
                entity.setModelId(null);
            } else {
                ApplianceModelEntity model = modelMapper.selectById(dto.getModelId());
                if (model == null || !"ENABLED".equals(model.getStatus())
                        || !SELECTABLE_COVERAGE.contains(model.getCoverageStatus())) {
                    throw BusinessException.validation("型号不可选");
                }
                entity.setModelId(model.getId());
            }
        }
        if (dto.hasField("modelText")) {
            entity.setModelText(dto.getModelText());
        }
        if (dto.hasField("serialNumber")) {
            entity.setSerialNumber(dto.getSerialNumber());
        }
        if (dto.hasField("symptomDescription")) {
            entity.setSymptomDescription(dto.getSymptomDescription());
        }
        if (dto.hasField("errorCodeText")) {
            entity.setErrorCodeText(dto.getErrorCodeText());
        }
        if (dto.hasField("occurredAt")) {
            if (dto.getOccurredAt() == null) {
                entity.setOccurredAt(null);
            } else {
                LocalDateTime occurredAt = dto.getOccurredAt().withOffsetSameInstant(ZoneOffset.UTC)
                        .toLocalDateTime();
                if (occurredAt.isAfter(LocalDateTime.now(ZoneOffset.UTC))) {
                    throw BusinessException.validation("发生时间不能晚于当前时间");
                }
                entity.setOccurredAt(occurredAt);
            }
        }
        if (dto.hasField("safetyAnswers")) {
            entity.setSafetyAnswers(dto.getSafetyAnswers() == null
                    ? null : assembler.toJson(dto.getSafetyAnswers()));
        }
        if (dto.hasField("addressId")) {
            entity.setAddressId(dto.getAddressId());
        }
        validateAggregate(entity, brandExplicitlyCleared);
    }

    /**
     * 合并后的草稿必须整体自洽：型号仍属于当前品牌与设备类型，覆盖状态与型号保持一致。
     * 只改品牌或只改设备类型时，原型号会在这里被重新校验。
     */
    private void validateAggregate(RepairRequestEntity entity, boolean brandExplicitlyCleared) {
        if (entity.getBrandId() != null) {
            ApplianceBrandEntity brand = brandMapper.selectById(entity.getBrandId());
            if (brand == null || !"ENABLED".equals(brand.getStatus())) {
                throw BusinessException.validation("品牌不可用");
            }
        }
        if (entity.getModelId() == null) {
            entity.setCoverageStatus(entity.getModelText() != null && !entity.getModelText().isBlank()
                    ? "PENDING_SOURCE" : null);
            return;
        }
        ApplianceModelEntity model = modelMapper.selectById(entity.getModelId());
        if (model == null || !"ENABLED".equals(model.getStatus())
                || !SELECTABLE_COVERAGE.contains(model.getCoverageStatus())) {
            throw BusinessException.validation("型号不可选");
        }
        if (entity.getBrandId() == null) {
            if (brandExplicitlyCleared) {
                throw BusinessException.validation("清除品牌前请先清除型号");
            }
            entity.setBrandId(model.getBrandId());
        } else if (!entity.getBrandId().equals(model.getBrandId())) {
            throw BusinessException.validation("型号与品牌不一致");
        }
        if (entity.getApplianceType() == null) {
            entity.setApplianceType(model.getApplianceType());
        } else if (!entity.getApplianceType().equals(model.getApplianceType())) {
            throw BusinessException.validation("型号与设备类型不一致");
        }
        entity.setCoverageStatus(model.getCoverageStatus());
    }

    private void validateComplete(RepairRequestEntity entity) {
        if (entity.getApplianceType() == null) {
            throw BusinessException.validation("缺少设备类型");
        }
        if (entity.getBrandId() == null) {
            throw BusinessException.validation("缺少品牌");
        }
        String symptom = entity.getSymptomDescription();
        if (symptom == null || symptom.trim().length() < 10 || symptom.length() > 1000) {
            throw BusinessException.validation("故障描述需 10–1000 字");
        }
        if (entity.getModelId() == null && (entity.getModelText() == null || entity.getModelText().isBlank())) {
            throw BusinessException.validation("请选择型号或填写型号文本");
        }
        if (entity.getAddressId() == null) {
            throw BusinessException.validation("缺少服务地址");
        }
        SafetyAnswersDTO answers = parseSafety(entity.getSafetyAnswers());
        if (answers == null || answers.getSmokeOrBurningSmell() == null || answers.getElectricLeakage() == null
                || answers.getGasSmell() == null || answers.getSuspectedRefrigerantLeak() == null
                || answers.getSevereWaterIngress() == null) {
            throw BusinessException.validation("五项安全问题必须全部回答");
        }
    }

    private RepairRequestEntity requireOwned(Long userId, Long repairRequestId) {
        RepairRequestEntity entity = repairRequestMapper.selectById(repairRequestId);
        if (entity == null) {
            throw BusinessException.notFound("报修不存在");
        }
        if (!entity.getUserId().equals(userId)) {
            throw BusinessException.forbidden("无权访问该报修");
        }
        return entity;
    }

    /** 与 updateDraft 的 UPDATE 保持同一加锁顺序：先聚合根，再文件行。 */
    private RepairRequestEntity lockOwned(Long userId, Long repairRequestId) {
        RepairRequestEntity entity = repairRequestMapper.selectOne(
                new LambdaQueryWrapper<RepairRequestEntity>()
                        .eq(RepairRequestEntity::getId, repairRequestId)
                        .last("FOR UPDATE"));
        if (entity == null) {
            throw BusinessException.notFound("报修不存在");
        }
        if (!entity.getUserId().equals(userId)) {
            throw BusinessException.forbidden("无权访问该报修");
        }
        return entity;
    }

    private CustomerAddressEntity requireOwnedAddress(Long userId, Long addressId) {
        CustomerAddressEntity address = addressMapper.selectById(addressId);
        if (address == null || !address.getUserId().equals(userId) || !"ENABLED".equals(address.getStatus())) {
            throw BusinessException.forbidden("地址不可用");
        }
        return address;
    }

    private void syncMedia(Long repairRequestId, Long userId, List<Long> fileIds) {
        if (fileIds == null) {
            return;
        }
        if (fileIds.size() > 6) {
            throw BusinessException.validation("最多上传 6 张图片");
        }
        if (fileIds.stream().anyMatch(java.util.Objects::isNull)) {
            throw BusinessException.validation("图片 ID 不正确");
        }
        if (new java.util.HashSet<>(fileIds).size() != fileIds.size()) {
            throw BusinessException.validation("图片重复");
        }
        List<Long> previous = mediaIds(repairRequestId);
        // 释放旧文件也会写文件行；新旧并集必须在改媒体关系之前统一排序加锁。
        Set<Long> allFiles = new java.util.TreeSet<>(previous);
        allFiles.addAll(fileIds);
        Map<Long, FileAssetEntity> locked = lockFiles(new ArrayList<>(allFiles));
        validateOwnedFiles(userId, fileIds, locked);
        mediaMapper.delete(new LambdaQueryWrapper<RepairRequestMediaEntity>()
                .eq(RepairRequestMediaEntity::getRepairRequestId, repairRequestId));
        int order = 0;
        for (Long fileId : fileIds) {
            RepairRequestMediaEntity media = new RepairRequestMediaEntity();
            media.setRepairRequestId(repairRequestId);
            media.setFileId(fileId);
            media.setMediaPurpose("REPAIR_MEDIA");
            media.setSortOrder(order++);
            media.setCreatedAt(now());
            mediaMapper.insert(media);
        }
        // 冻结规则 15.2：绑定业务后立即转 ACTIVE，不能等到提交，否则 24 小时清理会删掉草稿图片
        fileIds.forEach(this::activateFile);
        previous.stream()
                .filter(id -> !fileIds.contains(id))
                .forEach(this::releaseFile);
    }

    private void activateFile(Long fileId) {
        int rows = fileAssetMapper.update(null, new UpdateWrapper<FileAssetEntity>()
                .eq("id", fileId)
                .eq("status", "TEMP")
                .set("status", "ACTIVE")
                .set("updated_at", now()));
        if (rows > 0) {
            return;
        }
        FileAssetEntity current = fileAssetMapper.selectById(fileId);
        if (current == null || !"ACTIVE".equals(current.getStatus())) {
            // 文件已被清理任务认领或删除；此时绑定关系必须回滚，不能留下指向已删对象的照片
            throw BusinessException.validation("图片已失效，请重新上传");
        }
    }

    /** 从草稿移除的图片退回 TEMP，交给临时文件清理任务回收；仍被其它业务引用时不处理。 */
    private void releaseFile(Long fileId) {
        // 使用当前读，不能用进入事务时的旧快照判断其他草稿是否仍在引用。
        if (!mediaMapper.selectList(new LambdaQueryWrapper<RepairRequestMediaEntity>()
                .eq(RepairRequestMediaEntity::getFileId, fileId)
                .last("LIMIT 1 FOR UPDATE")).isEmpty()) {
            return;
        }
        fileAssetMapper.update(null, new UpdateWrapper<FileAssetEntity>()
                .eq("id", fileId)
                .eq("status", "ACTIVE")
                .set("status", "TEMP")
                .set("updated_at", now()));
    }

    private void requireOwnedFiles(Long userId, List<Long> fileIds) {
        validateOwnedFiles(userId, fileIds, lockFiles(fileIds));
    }

    private Map<Long, FileAssetEntity> lockFiles(List<Long> fileIds) {
        Map<Long, FileAssetEntity> locked = new LinkedHashMap<>();
        // 多文件按 ID 排序加锁，避免两个请求以不同顺序锁同一组文件
        for (Long fileId : fileIds.stream().distinct().sorted().toList()) {
            // 与临时文件清理任务使用同一行锁：清理任务的条件 UPDATE 会等待本事务提交，
            // 从而保证"绑定成功"和"对象被删"不会同时发生
            FileAssetEntity file = fileAssetMapper.selectOne(new LambdaQueryWrapper<FileAssetEntity>()
                    .eq(FileAssetEntity::getId, fileId)
                    .last("FOR UPDATE"));
            locked.put(fileId, file);
        }
        return locked;
    }

    private void validateOwnedFiles(Long userId, List<Long> fileIds, Map<Long, FileAssetEntity> locked) {
        for (Long fileId : fileIds) {
            FileAssetEntity file = locked.get(fileId);
            if (file == null || !"CUSTOMER".equals(file.getUploaderType())
                    || !userId.equals(file.getUploaderId())) {
                throw BusinessException.forbidden("文件归属校验失败");
            }
            if (!Set.of("TEMP", "ACTIVE").contains(file.getStatus())) {
                throw BusinessException.validation("文件已失效，请重新上传");
            }
            if (file.getEtag() == null || file.getEtag().isBlank()) {
                throw BusinessException.validation("文件尚未完成上传，请稍后重试");
            }
            if (!Set.of("REPAIR_MEDIA", "NAME_PLATE").contains(file.getPurpose())
                    || file.getMimeType() == null || !file.getMimeType().startsWith("image/")) {
                throw BusinessException.validation("报修仅支持图片文件");
            }
        }
    }

    private void activateFiles(List<Long> fileIds) {
        fileIds.forEach(this::activateFile);
    }

    private List<Long> mediaIds(Long repairRequestId) {
        return mediaMapper.selectList(new LambdaQueryWrapper<RepairRequestMediaEntity>()
                        .eq(RepairRequestMediaEntity::getRepairRequestId, repairRequestId)
                        .orderByAsc(RepairRequestMediaEntity::getSortOrder))
                .stream().map(RepairRequestMediaEntity::getFileId).toList();
    }

    /** CHG-0004：草稿查询按上传顺序返回 5 分钟签名地址，页面恢复时可显示缩略图。 */
    private List<FileAssetVO> mediaFiles(Long repairRequestId) {
        List<RepairRequestMediaEntity> media = mediaMapper.selectList(
                new LambdaQueryWrapper<RepairRequestMediaEntity>()
                        .eq(RepairRequestMediaEntity::getRepairRequestId, repairRequestId)
                        .orderByAsc(RepairRequestMediaEntity::getSortOrder));
        List<FileAssetVO> files = new ArrayList<>();
        for (RepairRequestMediaEntity item : media) {
            FileAssetEntity file = fileAssetMapper.selectById(item.getFileId());
            if (file == null || "DELETED".equals(file.getStatus())) {
                continue;
            }
            String url;
            try {
                url = storageService.generateSignedUrl(file.getStorageKey(), Duration.ofMinutes(5));
            } catch (RuntimeException ex) {
                url = "";
            }
            files.add(new FileAssetVO(file.getId(), url, file.getMimeType(),
                    file.getSizeBytes(), file.getSha256()));
        }
        return files;
    }

    private SafetyEventEntity createSafetyEvent(RepairRequestEntity entity,
                                                List<SafetyRuleEngine.MatchedRule> matched, String text) {
        SafetyEventEntity event = new SafetyEventEntity();
        event.setRepairRequestId(entity.getId());
        event.setRuleCodes(assembler.toJson(matched.stream().map(SafetyRuleEngine.MatchedRule::ruleCode).toList()));
        event.setMatchedRules(assembler.toJson(matched));
        event.setMatchedText(text.length() > 1000 ? text.substring(0, 1000) : text);
        event.setStatus("OPEN");
        event.setCreatedAt(now());
        event.setUpdatedAt(now());
        safetyEventMapper.insert(event);
        return event;
    }

    private WorkOrderEntity createEmergencyWorkOrder(Long userId, RepairRequestEntity entity,
                                                     CustomerAddressEntity address, SafetyEventEntity event) {
        ApplianceBrandEntity brand = entity.getBrandId() == null ? null : brandMapper.selectById(entity.getBrandId());
        ApplianceModelEntity model = entity.getModelId() == null ? null : modelMapper.selectById(entity.getModelId());
        WorkOrderEntity order = new WorkOrderEntity();
        order.setVisitPolicy("CUSTOMER_SLOT");
        order.setWorkOrderNo(nextNo("WO"));
        order.setRepairRequestId(entity.getId());
        order.setCustomerId(userId);
        order.setStatus("WAITING_MANUAL_REVIEW");
        order.setApplianceType(entity.getApplianceType());
        order.setBrandCode(brand == null ? null : brand.getCode());
        order.setModelCode(model == null ? null : model.getModelCode());
        Map<String, Object> device = new LinkedHashMap<>();
        device.put("applianceType", entity.getApplianceType());
        device.put("brandCode", brand == null ? null : brand.getCode());
        device.put("modelCode", model == null ? null : model.getModelCode());
        device.put("modelText", entity.getModelText());
        device.put("serialNumber", entity.getSerialNumber());
        order.setDeviceSnapshot(assembler.toJson(device));
        order.setContactSnapshot(assembler.toJson(contactSnapshot(address)));
        order.setAddressSnapshot(assembler.toJson(addressSnapshot(address)));
        order.setSymptomDescription(entity.getSymptomDescription());
        order.setVersion(0);
        order.setCreatedAt(now());
        order.setUpdatedAt(now());
        workOrderMapper.insert(order);

        WorkOrderStatusLogEntity log = new WorkOrderStatusLogEntity();
        log.setWorkOrderId(order.getId());
        log.setFromStatus(null);
        log.setToStatus("WAITING_MANUAL_REVIEW");
        log.setReason("高风险硬规则命中：" + String.join(",", matchedCodes(event)));
        log.setOperatorType("SYSTEM");
        log.setTraceId(com.fixmate.common.trace.TraceContext.current());
        log.setCreatedAt(now());
        statusLogMapper.insert(log);

        safetyEventMapper.update(null, new UpdateWrapper<SafetyEventEntity>()
                .eq("id", event.getId())
                .set("work_order_id", order.getId())
                .set("updated_at", now()));
        event.setWorkOrderId(order.getId());
        return order;
    }

    @SuppressWarnings("unchecked")
    private List<String> matchedCodes(SafetyEventEntity event) {
        try {
            return objectMapper.readValue(event.getRuleCodes(), List.class);
        } catch (Exception ex) {
            return List.of();
        }
    }

    private String privateContactSnapshot(CustomerAddressEntity address) {
        Map<String,Object> snapshot=new LinkedHashMap<>();
        snapshot.put("contactName",address.getContactName());
        snapshot.put("contactPhone",address.getContactPhone());
        snapshot.put("contactSource","SUBMISSION_SNAPSHOT");
        return assembler.toJson(snapshot);
    }

    private Map<String, Object> contactSnapshot(CustomerAddressEntity address) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("contactName", address.getContactName());
        map.put("contactPhone", maskPhone(address.getContactPhone()));
        return map;
    }

    private Map<String, Object> addressSnapshot(CustomerAddressEntity address) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("province", address.getProvince());
        map.put("city", address.getCity());
        map.put("district", address.getDistrict());
        map.put("detail", address.getDetail());
        return map;
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    private String joinText(RepairRequestEntity entity) {
        List<String> parts = new ArrayList<>();
        if (entity.getSymptomDescription() != null) {
            parts.add(entity.getSymptomDescription());
        }
        if (entity.getErrorCodeText() != null) {
            parts.add(entity.getErrorCodeText());
        }
        if (entity.getModelText() != null) {
            parts.add(entity.getModelText());
        }
        return String.join(" ", parts);
    }

    private SafetyAnswersDTO parseSafety(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, SafetyAnswersDTO.class);
        } catch (Exception ex) {
            return null;
        }
    }

    private RepairRequestVO toVO(RepairRequestEntity entity) {return toVO(entity,true);}
    private RepairRequestVO toVO(RepairRequestEntity entity,boolean includeMedia) {
        ApplianceBrandEntity brand = entity.getBrandId() == null ? null : brandMapper.selectById(entity.getBrandId());
        ApplianceModelEntity model = entity.getModelId() == null ? null : modelMapper.selectById(entity.getModelId());
        return assembler.toVO(entity, brand, model, includeMedia?mediaIds(entity.getId()):List.of(), includeMedia?mediaFiles(entity.getId()):List.of());
    }

    /** 同进程去重并处理时钟回拨；RR与普通/紧急WO共用分配序列，唯一键保留。 */
    static String nextNo(String prefix) {
        return NUMBERS.next(prefix);
    }

    private LocalDateTime now() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }
}
