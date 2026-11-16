package com.fixmate.service.user.serviceImpl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fixmate.assembler.RepairRecordAssembler;
import com.fixmate.common.api.PageVO;
import com.fixmate.common.enums.RepairRecordGroup;
import com.fixmate.dto.user.RepairRecordDetailVO;
import com.fixmate.dto.user.RepairRecordListItemVO;
import com.fixmate.dto.user.RepairRequestVO;
import com.fixmate.entity.WorkOrderEntity;
import com.fixmate.exception.BusinessException;
import com.fixmate.mapper.RepairRecordQueryMapper;
import com.fixmate.mapper.WorkOrderMapper;
import com.fixmate.service.user.UserProfileService;
import com.fixmate.service.user.UserRepairRecordsService;
import com.fixmate.service.user.UserRepairRequestService;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class UserRepairRecordsServiceImpl implements UserRepairRecordsService {
    private final RepairRecordQueryMapper query;
    private final WorkOrderMapper orders;
    private final UserRepairRequestService requests;
    private final UserProfileService profiles;
    private final RepairRecordAssembler assembler;
    private final com.fixmate.service.common.VisitSchedulingService visits;
    private final com.fixmate.service.common.CustomerCancellationPolicy cancellations;

    public UserRepairRecordsServiceImpl(RepairRecordQueryMapper query, WorkOrderMapper orders,
            UserRepairRequestService requests, UserProfileService profiles, RepairRecordAssembler assembler,com.fixmate.service.common.VisitSchedulingService visits,
            com.fixmate.service.common.CustomerCancellationPolicy cancellations) {
        this.query=query; this.orders=orders; this.requests=requests; this.profiles=profiles; this.assembler=assembler;this.visits=visits;this.cancellations=cancellations;
    }

    @Override
    public PageVO<RepairRecordListItemVO> list(Long userId, int page, int size, RepairRecordGroup group) {
        profiles.current(userId);
        if (page < 1 || size < 1 || size > 100) throw BusinessException.validation("分页参数不合法");
        IPage<RepairRecordListItemVO> result = query.list(new Page<>(page,size), userId, group.name());
        return new PageVO<>(result.getRecords(),page,size,result.getTotal());
    }

    @Override
    public RepairRecordDetailVO detail(Long userId, Long id) {
        profiles.current(userId);
        RepairRequestVO request = requests.get(userId,id);
        WorkOrderEntity order = request.getWorkOrderId()==null ? null : orders.selectById(request.getWorkOrderId());
        if (request.getWorkOrderId()!=null && (order==null || !userId.equals(order.getCustomerId())
                || !id.equals(order.getRepairRequestId()))) {
            throw BusinessException.forbidden("关联工单不可访问");
        }
        List<String> actions = new ArrayList<>();
        List<String> original=request.getAllowedActions()==null ? List.of() : request.getAllowedActions();
        if (original.contains("EDIT_DRAFT")) actions.add("CONTINUE_DRAFT");
        if (original.contains("CANCEL")) actions.add("CANCEL");
        // 阶段2人工核对后，由新记录详情承接本人确认；原RepairRequestVO及原三页不变。
        if (order==null && "TRIAGED".equals(request.getStatus())) actions.add("CONVERT_TO_WORK_ORDER");
        if (order!=null) actions.add("VIEW_WORK_ORDER");
        String cancellation=cancellations.action(order);
        if(cancellation!=null)actions.add(cancellation);
        if (order!=null&&visits.canSelect(order)) actions.add("SELECT_PREFERRED_VISIT");
        if(order!=null&&"WAITING_ACCEPTANCE".equals(order.getStatus()))actions.add("RAISE_ACCEPTANCE_ISSUE");
        return new RepairRecordDetailVO(com.fixmate.service.common.VisitSchedulingService.preferred(order),com.fixmate.service.common.VisitSchedulingService.policy(order),assembler.snapshot(request),request.getWorkOrderId(),
                order==null ? null : order.getStatus(),order==null ? null : order.getVersion(),actions,cancellations.cancellationAmount(order,cancellation));
    }
}
