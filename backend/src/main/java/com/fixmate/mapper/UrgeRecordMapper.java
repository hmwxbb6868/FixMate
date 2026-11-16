package com.fixmate.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fixmate.entity.UrgeRecordEntity;
import java.time.LocalDateTime;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface UrgeRecordMapper extends BaseMapper<UrgeRecordEntity> {
    @Update("""
            UPDATE urge_record
               SET status='INVALIDATED', handled_by=NULL, handled_at=#{at}, handling_conclusion=#{conclusion},
                   version=version+1, updated_at=#{at}
             WHERE work_order_id=#{orderId} AND status='PENDING'
            """)
    int invalidatePending(@Param("orderId") Long orderId,@Param("at") LocalDateTime at,
                          @Param("conclusion") String conclusion);
}
