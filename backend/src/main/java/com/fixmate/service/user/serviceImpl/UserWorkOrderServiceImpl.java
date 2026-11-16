package com.fixmate.service.user.serviceImpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fixmate.assembler.QuoteAssembler;
import com.fixmate.common.api.PageVO;
import com.fixmate.common.audit.AuditLogService;
import com.fixmate.common.enums.WorkOrderStatus;
import com.fixmate.dto.user.AcceptanceVO;
import com.fixmate.dto.user.AppointmentVO;
import com.fixmate.dto.user.FileAssetVO;
import com.fixmate.dto.user.QuoteVO;
import com.fixmate.dto.user.ServiceRecordVO;
import com.fixmate.dto.user.TimelineEventVO;
import com.fixmate.dto.user.WorkOrderAcceptanceDTO;
import com.fixmate.dto.user.WorkOrderDetailVO;
import com.fixmate.dto.user.WorkOrderListItemVO;
import com.fixmate.entity.AppointmentEntity;
import com.fixmate.entity.HumanHandoffRecordEntity;
import com.fixmate.entity.QuoteEntity;
import com.fixmate.entity.RepairRequestEntity;
import com.fixmate.entity.RepairRequestMediaEntity;
import com.fixmate.entity.ServiceRecordEntity;
import com.fixmate.entity.StaffUserEntity;
import com.fixmate.entity.UrgeRecordEntity;
import com.fixmate.entity.WorkOrderEntity;
import com.fixmate.entity.WorkOrderStatusLogEntity;
import com.fixmate.exception.BusinessException;
import com.fixmate.mapper.AppointmentMapper;
import com.fixmate.mapper.QuoteMapper;
import com.fixmate.mapper.RepairRequestMapper;
import com.fixmate.mapper.RepairRequestMediaMapper;
import com.fixmate.mapper.FileAssetMapper;
import com.fixmate.mapper.HumanHandoffRecordMapper;
import com.fixmate.mapper.ServiceRecordMapper;
import com.fixmate.mapper.StaffUserMapper;
import com.fixmate.mapper.UrgeRecordMapper;
import com.fixmate.mapper.WorkOrderMapper;
import com.fixmate.mapper.WorkOrderStatusLogMapper;
import com.fixmate.service.common.WorkOrderStateMachine;
import com.fixmate.service.common.WorkOrderUrges;
import com.fixmate.service.common.CustomerCancellationPolicy;
import com.fixmate.service.common.VisitSchedulingService;
import com.fixmate.service.common.WorkOrderFollowUp;
import com.fixmate.service.user.UserWorkOrderService;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserWorkOrderServiceImpl implements UserWorkOrderService {

    private final WorkOrderMapper workOrderMapper;
    private final AppointmentMapper appointmentMapper;
    private final QuoteMapper quoteMapper;
    private final QuoteAssembler quoteAssembler;
    private final ServiceRecordMapper serviceRecordMapper;
    private final StaffUserMapper staffUserMapper;
    private final WorkOrderStatusLogMapper statusLogMapper;
    private final RepairRequestMapper repairRequestMapper;
    private final RepairRequestMediaMapper repairRequestMediaMapper;
    private final FileAssetMapper fileAssetMapper;
    private final com.fixmate.storage.ObjectStorageService storageService;
    private final WorkOrderStateMachine stateMachine;
    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;
    private final WorkOrderUrges urges;
    private final CustomerCancellationPolicy cancellations;
    private final UrgeRecordMapper urgeRecords;
    private final HumanHandoffRecordMapper handoffRecords;
    private final VisitSchedulingService visits;
    private final WorkOrderFollowUp followUp;
    private final com.fixmate.mapper.DbTimeMapper clock;

    public UserWorkOrderServiceImpl(WorkOrderMapper workOrderMapper,
                                    AppointmentMapper appointmentMapper,
                                    QuoteMapper quoteMapper,
                                    QuoteAssembler quoteAssembler,
                                    ServiceRecordMapper serviceRecordMapper,
                                    StaffUserMapper staffUserMapper,
                                    WorkOrderStatusLogMapper statusLogMapper,
                                    RepairRequestMapper repairRequestMapper,
                                    RepairRequestMediaMapper repairRequestMediaMapper,
                                    FileAssetMapper fileAssetMapper,
                                    com.fixmate.storage.ObjectStorageService storageService,
                                    WorkOrderStateMachine stateMachine,
                                    AuditLogService auditLogService,
                                    ObjectMapper objectMapper,WorkOrderUrges urges,CustomerCancellationPolicy cancellations,
                                    UrgeRecordMapper urgeRecords,HumanHandoffRecordMapper handoffRecords,
                                    VisitSchedulingService visits,WorkOrderFollowUp followUp,com.fixmate.mapper.DbTimeMapper clock) {
        this.workOrderMapper = workOrderMapper;
        this.appointmentMapper = appointmentMapper;
        this.quoteMapper = quoteMapper;
        this.quoteAssembler = quoteAssembler;
        this.serviceRecordMapper = serviceRecordMapper;
        this.staffUserMapper = staffUserMapper;
        this.statusLogMapper = statusLogMapper;
        this.repairRequestMapper = repairRequestMapper;
        this.repairRequestMediaMapper = repairRequestMediaMapper;
        this.fileAssetMapper = fileAssetMapper;
        this.storageService = storageService;
        this.stateMachine = stateMachine;
        this.auditLogService = auditLogService;
        this.objectMapper = objectMapper;
        this.urges = urges;
        this.cancellations=cancellations;
        this.urgeRecords=urgeRecords;
        this.handoffRecords=handoffRecords;
        this.visits=visits;
        this.followUp=followUp;
        this.clock=clock;
    }

    @Override
    public PageVO<WorkOrderListItemVO> list(Long userId, int page, int size, String status) {
        LambdaQueryWrapper<WorkOrderEntity> query = new LambdaQueryWrapper<WorkOrderEntity>()
                .eq(WorkOrderEntity::getCustomerId, userId)
                .orderByDesc(WorkOrderEntity::getCreatedAt);
        if (status != null && !status.isBlank()) {
            query.eq(WorkOrderEntity::getStatus, status);
        }
        Page<WorkOrderEntity> result = workOrderMapper.selectPage(new Page<>(page, size), query);
        List<WorkOrderListItemVO> items = result.getRecords().stream()
                .map(o -> new WorkOrderListItemVO(o.getId(), o.getWorkOrderNo(), o.getStatus(),
                        o.getApplianceType(), o.getBrandCode(), o.getModelCode(),
                        latestAppointmentAt(o.getId()), o.getCreatedAt()))
                .toList();
        return new PageVO<>(items, page, size, result.getTotal());
    }

    @Override
    public WorkOrderDetailVO detail(Long userId, Long workOrderId) {
        WorkOrderEntity order = requireOwned(userId, workOrderId);
        return buildDetail(order,true,true);
    }

    @Override
    @Transactional(isolation=org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public WorkOrderDetailVO accept(Long userId, Long workOrderId, WorkOrderAcceptanceDTO dto) {
        WorkOrderEntity order = workOrderMapper.selectOne(new LambdaQueryWrapper<WorkOrderEntity>().eq(WorkOrderEntity::getId,workOrderId).last("FOR UPDATE"));
        if(order==null)throw BusinessException.notFound("工单不存在");
        if(!userId.equals(order.getCustomerId()))throw BusinessException.forbidden("无权访问该工单");
        if (!WorkOrderStatus.WAITING_ACCEPTANCE.name().equals(order.getStatus())) {
            throw BusinessException.stateConflict("当前状态不可验收");
        }
        stateMachine.transition(workOrderId, WorkOrderStatus.COMPLETED, dto.expectedWorkOrderVersion(),
                "用户验收通过", "CUSTOMER", userId);
        java.time.LocalDateTime acceptedAt = java.time.LocalDateTime.now(java.time.ZoneOffset.UTC);
        workOrderMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<WorkOrderEntity>()
                .eq("id", workOrderId)
                .set("acceptance_note", dto.note())
                .set("accepted_at", acceptedAt));
        auditLogService.recordCustomer(userId, "WORK_ORDER_ACCEPTED", "WORK_ORDER", workOrderId,
                Map.of("status", "WAITING_ACCEPTANCE"),
                Map.of("status", "COMPLETED", "note", String.valueOf(dto.note())));
        return buildDetail(workOrderMapper.selectById(workOrderId),true,true);
    }

    public WorkOrderDetailVO buildDetail(WorkOrderEntity order) {return buildDetail(order,true);}
    public WorkOrderDetailVO buildDetail(WorkOrderEntity order,boolean includeMedia) {
        return buildDetail(order,includeMedia,false);
    }

    private WorkOrderDetailVO buildDetail(WorkOrderEntity order,boolean includeMedia,boolean customerView) {
        AppointmentVO appointment = latestAppointment(order);
        List<QuoteEntity> quotes = quoteMapper.selectList(new LambdaQueryWrapper<QuoteEntity>()
                .eq(QuoteEntity::getWorkOrderId, order.getId())
                .orderByAsc(QuoteEntity::getVersionNo));
        QuoteVO current = null;
        List<QuoteVO> history = new ArrayList<>();
        var now=quotes.stream().anyMatch(q->"SUBMITTED".equals(q.getStatus())&&q.getExpiresAt()!=null)?clock.utcNow():null;
        for (QuoteEntity quote : quotes) {
            if ("DRAFT".equals(quote.getStatus()) || ("WITHDRAWN".equals(quote.getStatus()) && quote.getSubmittedAt() == null)) continue;
            QuoteVO vo = quoteAssembler.toVO(quote);
            boolean expired="SUBMITTED".equals(quote.getStatus())&&quote.getExpiresAt()!=null&&!quote.getExpiresAt().isAfter(now);
            if (expired) vo.setStatus("EXPIRED");
            if (current == null && "SUBMITTED".equals(quote.getStatus()) && !expired) {
                current = vo;
            } else {
                history.add(vo);
            }
        }

        List<ServiceRecordVO> serviceRecords = serviceRecordMapper.selectList(
                        new LambdaQueryWrapper<ServiceRecordEntity>()
                                .eq(ServiceRecordEntity::getWorkOrderId, order.getId())
                                .orderByAsc(ServiceRecordEntity::getSubmittedAt))
                .stream()
                .map(r -> new ServiceRecordVO(r.getId(), r.getSubmittedAt(),
                        technicianName(r.getTechnicianId()), r.getSummary()))
                .toList();
        AcceptanceVO acceptance = null;
        if (WorkOrderStatus.COMPLETED.name().equals(order.getStatus())) {
            acceptance = new AcceptanceVO(true, order.getAcceptanceNote(), order.getAcceptedAt());
        }

        List<TimelineEventVO> timeline = statusLogMapper.selectList(
                        new LambdaQueryWrapper<WorkOrderStatusLogEntity>()
                                .eq(WorkOrderStatusLogEntity::getWorkOrderId, order.getId())
                                .orderByAsc(WorkOrderStatusLogEntity::getCreatedAt))
                .stream()
                // 用户端只给映射后的文案；reason 含规则代码，属于内部备注，不下发
                .map(l -> new TimelineEventVO("WORK_ORDER_STATUS",
                        com.fixmate.service.common.UserTimelineText.eventTitle(l.getFromStatus(), l.getToStatus()),
                        null, l.getCreatedAt()))
                .toList();
        if(customerView) {
            timeline=new ArrayList<>(timeline);
            timeline.addAll(supportUpdates(order));
            timeline.sort(Comparator.comparing(TimelineEventVO::occurredAt));
        }

        Map<String, String> contact = parseMap(order.getContactSnapshot());
        Map<String, String> address = parseMap(order.getAddressSnapshot());
        String addressText = String.join("", nullSafe(address.get("province")), nullSafe(address.get("city")),
                nullSafe(address.get("district")), nullSafe(address.get("detail")));

        // 故障码与发生时间只存在于报修记录，工单不复制这两项，因此按 repairRequestId 回查
        RepairRequestEntity request = order.getRepairRequestId() == null
                ? null : repairRequestMapper.selectById(order.getRepairRequestId());

        List<FileAssetVO> mediaFiles = new ArrayList<>();
        if (includeMedia && order.getRepairRequestId() != null) {
            List<RepairRequestMediaEntity> media = repairRequestMediaMapper.selectList(
                    new LambdaQueryWrapper<RepairRequestMediaEntity>()
                            .eq(RepairRequestMediaEntity::getRepairRequestId, order.getRepairRequestId())
                            .orderByAsc(RepairRequestMediaEntity::getSortOrder));
            for (RepairRequestMediaEntity item : media) {
                com.fixmate.entity.FileAssetEntity file = fileAssetMapper.selectById(item.getFileId());
                if (file == null) {
                    continue;
                }
                String url;
                try {
                    // 私有 Bucket：只下发 5 分钟短时签名地址，不返回对象键或长期凭证
                    url = storageService.generateSignedUrl(file.getStorageKey(), java.time.Duration.ofMinutes(5));
                } catch (RuntimeException ex) {
                    url = "";
                }
                mediaFiles.add(new FileAssetVO(file.getId(), url, file.getMimeType(),
                        file.getSizeBytes(), file.getSha256()));
            }
        }

        var actions=allowedActions(order,current!=null);
        return new WorkOrderDetailVO(order.getId(), order.getWorkOrderNo(), order.getStatus(),
                order.getApplianceType(), order.getBrandCode(), order.getModelCode(),
                order.getSymptomDescription(),
                request == null ? null : request.getErrorCodeText(),
                request == null ? null : request.getOccurredAt(),
                mediaFiles,
                contact.get("contactName"), contact.get("contactPhone"),
                addressText, appointment, technicianName(order.getTechnicianId()), current, history,
                serviceRecords, acceptance, timeline, actions, order.getVersion(),
                cancellations.cancellationAmount(order,actions.contains("CANCEL_WITH_VISIT_FEE")?"CANCEL_WITH_VISIT_FEE":null));
    }

    private List<TimelineEventVO> supportUpdates(WorkOrderEntity order) {
        var updates=new ArrayList<TimelineEventVO>();
        urgeRecords.selectList(new LambdaQueryWrapper<UrgeRecordEntity>()
                .eq(UrgeRecordEntity::getWorkOrderId,order.getId())
                .eq(UrgeRecordEntity::getUserId,order.getCustomerId())
                .orderByDesc(UrgeRecordEntity::getId).last("LIMIT 20"))
                .forEach(row->updates.add(supportUpdate("URGE_UPDATE","催办",row.getStatus(),
                        row.getCreatedAt(),row.getHandledAt(),row.getHandlingConclusion())));
        handoffRecords.selectList(new LambdaQueryWrapper<HumanHandoffRecordEntity>()
                .eq(HumanHandoffRecordEntity::getWorkOrderId,order.getId())
                .eq(HumanHandoffRecordEntity::getUserId,order.getCustomerId())
                .orderByDesc(HumanHandoffRecordEntity::getId).last("LIMIT 20"))
                .forEach(row->updates.add(supportUpdate("HUMAN_HANDOFF_UPDATE","人工请求",row.getStatus(),
                        row.getCreatedAt(),row.getHandledAt(),row.getHandlingConclusion())));
        return updates;
    }

    private TimelineEventVO supportUpdate(String type,String label,String status,java.time.LocalDateTime createdAt,
                                           java.time.LocalDateTime handledAt,String conclusion) {
        String title=switch(status) {
            case "HANDLED" -> label+"已处理";
            case "INVALIDATED" -> label+"已失效";
            default -> label+"待处理";
        };
        return new TimelineEventVO(type,title,handledAt==null?null:conclusion,handledAt==null?createdAt:handledAt);
    }

    private List<String> allowedActions(WorkOrderEntity order, boolean hasCurrentQuote) {
        List<String> actions = new ArrayList<>();
        String status = order.getStatus();
        boolean terminal = WorkOrderStatus.COMPLETED.name().equals(status)
                || WorkOrderStatus.CANCELLED.name().equals(status)
                || WorkOrderStatus.CLOSED_UNRESOLVED.name().equals(status);
        if (terminal) {
            return actions;
        }
        String cancellation=cancellations.action(order);
        if(cancellation!=null)actions.add(cancellation);
        if (!urges.awaitingRedispatchAfterWithdrawal(order)) {
            actions.add("URGE");
        }
        actions.add("HUMAN_HANDOFF");
        boolean rework=followUp.isRework(order);
        if (rework&&visits.canSelect(order)) {
            actions.add("SELECT_PREFERRED_VISIT");
        } else if (WorkOrderStatus.APPOINTED.name().equals(status)
                || WorkOrderStatus.WAITING_APPOINTMENT.name().equals(status)&&!rework) {
            actions.add("RESCHEDULE");
        }
        if (hasCurrentQuote && (WorkOrderStatus.WAITING_QUOTE_CONFIRMATION.name().equals(status)
                || WorkOrderStatus.PAUSED_FOR_REQUOTE.name().equals(status))) {
            actions.add("ACCEPT_QUOTE");
            actions.add("REJECT_QUOTE");
        }
        if (WorkOrderStatus.WAITING_ACCEPTANCE.name().equals(status)) {
            actions.add("ACCEPTANCE");
            actions.add("RAISE_ACCEPTANCE_ISSUE");
        }
        return actions;
    }

    private WorkOrderEntity requireOwned(Long userId, Long workOrderId) {
        WorkOrderEntity order = workOrderMapper.selectById(workOrderId);
        if (order == null) {
            throw BusinessException.notFound("工单不存在");
        }
        if (!order.getCustomerId().equals(userId)) {
            throw BusinessException.forbidden("无权访问该工单");
        }
        return order;
    }

    private java.time.LocalDateTime latestAppointmentAt(Long workOrderId) {
        AppointmentEntity appointment = latestAppointmentEntity(workOrderId);
        return appointment == null ? null : appointment.getScheduledStartAt();
    }

    private AppointmentVO latestAppointment(WorkOrderEntity order) {
        AppointmentEntity appointment = latestAppointmentEntity(order.getId());
        if (appointment == null) {
            return null;
        }
        if (Set.of("WAITING_MANUAL_REVIEW","WAITING_APPOINTMENT").contains(order.getStatus())
                && "COMPLETED".equals(appointment.getStatus()) && followUp.isRework(order) && visits.canSelect(order)) return null;
        AppointmentVO vo = new AppointmentVO();
        vo.setId(appointment.getId());
        vo.setStatus(appointment.getStatus());
        vo.setScheduledStartAt(appointment.getScheduledStartAt());
        vo.setScheduledEndAt(appointment.getScheduledEndAt());
        vo.setNote(appointment.getNote());
        // 预约未单独指派时，展示工单层面的维修人员
        vo.setTechnicianName(technicianName(appointment.getTechnicianId() != null
                ? appointment.getTechnicianId() : order.getTechnicianId()));
        return vo;
    }

    private AppointmentEntity latestAppointmentEntity(Long workOrderId) {
        return appointmentMapper.selectOne(new LambdaQueryWrapper<AppointmentEntity>()
                .eq(AppointmentEntity::getWorkOrderId, workOrderId)
                .orderByDesc(AppointmentEntity::getId)
                .last("limit 1"));
    }

    private String technicianName(Long technicianId) {
        if (technicianId == null) {
            return null;
        }
        StaffUserEntity staff = staffUserMapper.selectById(technicianId);
        return staff == null ? null : staff.getName();
    }

    private Map<String, String> parseMap(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, String>>() {
            });
        } catch (Exception ex) {
            return Map.of();
        }
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }
}
