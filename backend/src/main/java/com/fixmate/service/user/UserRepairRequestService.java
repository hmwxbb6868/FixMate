package com.fixmate.service.user;

import com.fixmate.dto.user.CancelRequestDTO;
import com.fixmate.dto.user.RepairDraftUpsertDTO;
import com.fixmate.dto.user.RepairRequestVO;
import com.fixmate.dto.user.RepairSubmitDTO;
import com.fixmate.dto.user.RepairSubmitResult;

public interface UserRepairRequestService {

    RepairRequestVO createDraft(Long userId, RepairDraftUpsertDTO dto);

    RepairRequestVO updateDraft(Long userId, Long repairRequestId, RepairDraftUpsertDTO dto);

    RepairRequestVO get(Long userId, Long repairRequestId);

    RepairRequestVO getWithoutMedia(Long userId, Long repairRequestId);

    RepairSubmitResult submit(Long userId, Long repairRequestId, RepairSubmitDTO dto);

    RepairRequestVO cancel(Long userId, Long repairRequestId, CancelRequestDTO dto);
}
