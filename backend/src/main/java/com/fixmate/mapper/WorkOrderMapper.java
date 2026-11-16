package com.fixmate.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fixmate.entity.WorkOrderEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface WorkOrderMapper extends BaseMapper<WorkOrderEntity> {
}
