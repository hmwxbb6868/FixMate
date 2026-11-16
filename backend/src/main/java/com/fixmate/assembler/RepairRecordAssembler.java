package com.fixmate.assembler;

import com.fixmate.dto.user.RepairRequestVO;
import com.fixmate.dto.user.RepairRequestSnapshotVO;
import org.springframework.stereotype.Component;

@Component
public class RepairRecordAssembler {
    public RepairRequestSnapshotVO snapshot(RepairRequestVO r) {
        return new RepairRequestSnapshotVO(r.getId(), r.getRepairNo(), r.getStatus(),
                r.getApplianceType(), r.getBrandId(), r.getBrandCode(), r.getModelId(), r.getModelCode(),
                r.getModelText(), r.getSerialNumber(), r.getSymptomDescription(), r.getErrorCodeText(),
                r.getOccurredAt(), r.getMediaFileIds(), r.getMediaFiles(), r.getSafetyAnswers(),
                r.getAddressId(), r.getCoverageStatus(), r.getWorkOrderId(), r.getVersion(), r.getCreatedAt());
    }
}
