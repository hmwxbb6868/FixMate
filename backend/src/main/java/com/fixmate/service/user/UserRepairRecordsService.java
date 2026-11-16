package com.fixmate.service.user;

import com.fixmate.common.api.PageVO;
import com.fixmate.common.enums.RepairRecordGroup;
import com.fixmate.dto.user.RepairRecordDetailVO;
import com.fixmate.dto.user.RepairRecordListItemVO;

public interface UserRepairRecordsService {
    PageVO<RepairRecordListItemVO> list(Long userId, int page, int size, RepairRecordGroup group);
    RepairRecordDetailVO detail(Long userId, Long id);
}
