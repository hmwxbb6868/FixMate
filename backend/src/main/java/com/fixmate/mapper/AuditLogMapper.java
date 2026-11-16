package com.fixmate.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fixmate.entity.AuditLogEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AuditLogMapper extends BaseMapper<AuditLogEntity> {
}
