package com.fixmate.mapper;

import java.time.LocalDateTime;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface DatabaseClockMapper {
    @Select("SELECT UTC_TIMESTAMP(3)")
    LocalDateTime now();
}
