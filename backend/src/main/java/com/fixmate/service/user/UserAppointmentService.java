package com.fixmate.service.user;

import com.fixmate.dto.user.AppointmentRescheduleDTO;
import com.fixmate.dto.user.AppointmentVO;

public interface UserAppointmentService {

    AppointmentVO reschedule(Long userId, Long workOrderId, AppointmentRescheduleDTO dto);
}
