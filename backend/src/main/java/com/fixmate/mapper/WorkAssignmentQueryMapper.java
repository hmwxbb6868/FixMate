package com.fixmate.mapper;

import org.apache.ibatis.annotations.*;

@Mapper
public interface WorkAssignmentQueryMapper {
    @Select("""
        SELECT COUNT(DISTINCT w.id) FROM work_order w
        LEFT JOIN work_order_assignment a ON a.id=w.current_assignment_id AND a.work_order_id=w.id
        WHERE w.status NOT IN ('COMPLETED','CANCELLED','CLOSED_UNRESOLVED')
          AND (w.technician_id=#{staffId} OR (a.technician_id=#{staffId} AND a.status IN ('PENDING','ACTIVE','RETURN_REQUESTED')))
        """)
    long activeTaskCount(@Param("staffId") Long staffId);
}
