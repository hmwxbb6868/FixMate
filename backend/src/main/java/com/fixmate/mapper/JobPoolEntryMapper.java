package com.fixmate.mapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fixmate.entity.JobPoolEntryEntity;
import org.apache.ibatis.annotations.Mapper;
@Mapper public interface JobPoolEntryMapper extends BaseMapper<JobPoolEntryEntity> {
    default JobPoolEntryEntity latestForOrder(Long workOrderId) {
        return selectOne(new LambdaQueryWrapper<JobPoolEntryEntity>()
                .eq(JobPoolEntryEntity::getWorkOrderId,workOrderId)
                .orderByDesc(JobPoolEntryEntity::getId).last("LIMIT 1"));
    }
}
