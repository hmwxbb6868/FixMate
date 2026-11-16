package com.fixmate.service.user.serviceImpl;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fixmate.common.audit.AuditLogService;
import com.fixmate.dto.user.WorkOrderDetailVO;
import com.fixmate.entity.ApplianceBrandEntity;
import com.fixmate.entity.ApplianceModelEntity;
import com.fixmate.entity.RepairRequestEntity;
import com.fixmate.entity.WorkOrderEntity;
import com.fixmate.entity.WorkOrderStatusLogEntity;
import com.fixmate.exception.BusinessException;
import com.fixmate.mapper.ApplianceBrandMapper;
import com.fixmate.mapper.ApplianceModelMapper;
import com.fixmate.mapper.RepairRequestMapper;
import com.fixmate.mapper.WorkOrderMapper;
import com.fixmate.mapper.WorkOrderStatusLogMapper;
import com.fixmate.service.user.UserRepairConvertService;
import com.fixmate.service.user.UserWorkOrderService;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserRepairConvertServiceImpl implements UserRepairConvertService {

    /**
     * 只有真正完成信息采集（TRIAGED）的报修才允许转工单。
     * 阶段 1 页面不开放入口，测试需准备 TRIAGED 受控数据，不能依赖前端隐藏。
     */
    private static final Set<String> CONVERTIBLE = Set.of("TRIAGED");

    private final RepairRequestMapper repairRequestMapper;
    private final WorkOrderMapper workOrderMapper;
    private final WorkOrderStatusLogMapper statusLogMapper;
    private final ApplianceBrandMapper brandMapper;
    private final ApplianceModelMapper modelMapper;
    private final UserWorkOrderService workOrderService;
    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;

    public UserRepairConvertServiceImpl(RepairRequestMapper repairRequestMapper,
                                        WorkOrderMapper workOrderMapper,
                                        WorkOrderStatusLogMapper statusLogMapper,
                                        ApplianceBrandMapper brandMapper,
                                        ApplianceModelMapper modelMapper,
                                        UserWorkOrderService workOrderService,
                                        AuditLogService auditLogService,
                                        ObjectMapper objectMapper) {
        this.repairRequestMapper = repairRequestMapper;
        this.workOrderMapper = workOrderMapper;
        this.statusLogMapper = statusLogMapper;
        this.brandMapper = brandMapper;
        this.modelMapper = modelMapper;
        this.workOrderService = workOrderService;
        this.auditLogService = auditLogService;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public WorkOrderDetailVO convertToWorkOrder(Long userId, Long repairRequestId, Integer expectedVersion) {
        RepairRequestEntity request = repairRequestMapper.selectById(repairRequestId);
        if (request == null) {
            throw BusinessException.notFound("报修不存在");
        }
        if (!request.getUserId().equals(userId)) {
            throw BusinessException.forbidden("无权操作该报修");
        }
        if (!CONVERTIBLE.contains(request.getStatus())) {
            throw BusinessException.stateConflict("当前状态不可转工单");
        }
        if (!expectedVersion.equals(request.getVersion())) {
            throw BusinessException.versionConflict("报修已被更新，请刷新后重试");
        }

        ApplianceBrandEntity brand = request.getBrandId() == null ? null : brandMapper.selectById(request.getBrandId());
        ApplianceModelEntity model = request.getModelId() == null ? null : modelMapper.selectById(request.getModelId());

        WorkOrderEntity order = new WorkOrderEntity();
        order.setVisitPolicy("CUSTOMER_SLOT");
        order.setWorkOrderNo(UserRepairRequestServiceImpl.nextNo("WO"));
        order.setRepairRequestId(request.getId());
        order.setCustomerId(userId);
        order.setStatus("WAITING_ASSIGNMENT");
        order.setApplianceType(request.getApplianceType());
        order.setBrandCode(brand == null ? null : brand.getCode());
        order.setModelCode(model == null ? null : model.getModelCode());
        Map<String, Object> device = new LinkedHashMap<>();
        device.put("applianceType", request.getApplianceType());
        device.put("brandCode", brand == null ? null : brand.getCode());
        device.put("modelCode", model == null ? null : model.getModelCode());
        device.put("modelText", request.getModelText());
        device.put("serialNumber", request.getSerialNumber());
        order.setDeviceSnapshot(toJson(device));
        order.setContactSnapshot(request.getContactSnapshot());
        order.setAddressSnapshot(request.getAddressSnapshot());
        order.setSymptomDescription(request.getSymptomDescription());
        order.setVersion(0);
        order.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));
        order.setUpdatedAt(LocalDateTime.now(ZoneOffset.UTC));
        workOrderMapper.insert(order);

        WorkOrderStatusLogEntity log = new WorkOrderStatusLogEntity();
        log.setWorkOrderId(order.getId());
        log.setToStatus("WAITING_ASSIGNMENT");
        log.setReason("由报修申请 " + request.getRepairNo() + " 创建");
        log.setOperatorType("CUSTOMER");
        log.setOperatorId(userId);
        log.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));
        statusLogMapper.insert(log);

        int rows = repairRequestMapper.update(null, new UpdateWrapper<RepairRequestEntity>()
                .eq("id", request.getId())
                .eq("version", expectedVersion)
                .set("status", "CONVERTED")
                .set("work_order_id", order.getId())
                .set("version", expectedVersion + 1)
                .set("updated_at", LocalDateTime.now(ZoneOffset.UTC)));
        if (rows == 0) {
            throw BusinessException.versionConflict("报修已被更新，请刷新后重试");
        }

        auditLogService.recordCustomer(userId, "REPAIR_CONVERTED", "REPAIR_REQUEST", request.getId(),
                Map.of("status", request.getStatus()), Map.of("status", "CONVERTED", "workOrderId", order.getId()));

        return workOrderService.buildDetail(workOrderMapper.selectById(order.getId()));
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            throw new IllegalStateException("snapshot serialize failed", ex);
        }
    }
}
