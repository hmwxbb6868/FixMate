package com.fixmate.mapper;

import java.time.LocalDateTime;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/** 定时任务与过期判定必须使用数据库时间，避免多实例 JVM 时钟漂移。 */
@Mapper
public interface DbTimeMapper {

    @Select("SELECT UTC_TIMESTAMP(3)")
    LocalDateTime utcNow();
}
