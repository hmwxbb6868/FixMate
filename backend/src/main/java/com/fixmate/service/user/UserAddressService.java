package com.fixmate.service.user;

import com.fixmate.dto.user.AddressCreateDTO;
import com.fixmate.dto.user.AddressUpdateDTO;
import com.fixmate.dto.user.AddressVO;
import com.fixmate.dto.user.DisableRequestDTO;
import java.util.List;

public interface UserAddressService {

    List<AddressVO> list(Long userId);

    AddressVO create(Long userId, AddressCreateDTO dto);

    AddressVO update(Long userId, Long addressId, AddressUpdateDTO dto);

    void disable(Long userId, Long addressId, DisableRequestDTO dto);
}
