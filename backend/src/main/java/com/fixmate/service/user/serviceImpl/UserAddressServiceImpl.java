package com.fixmate.service.user.serviceImpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.fixmate.common.audit.AuditLogService;
import com.fixmate.dto.user.AddressCreateDTO;
import com.fixmate.dto.user.AddressUpdateDTO;
import com.fixmate.dto.user.AddressVO;
import com.fixmate.dto.user.DisableRequestDTO;
import com.fixmate.entity.CustomerAddressEntity;
import com.fixmate.exception.BusinessException;
import com.fixmate.mapper.CustomerAddressMapper;
import com.fixmate.service.user.UserAddressService;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserAddressServiceImpl implements UserAddressService {

    private final CustomerAddressMapper addressMapper;
    private final AuditLogService auditLogService;

    public UserAddressServiceImpl(CustomerAddressMapper addressMapper, AuditLogService auditLogService) {
        this.addressMapper = addressMapper;
        this.auditLogService = auditLogService;
    }

    @Override
    public List<AddressVO> list(Long userId) {
        return addressMapper.selectList(new LambdaQueryWrapper<CustomerAddressEntity>()
                        .eq(CustomerAddressEntity::getUserId, userId)
                        .eq(CustomerAddressEntity::getStatus, "ENABLED")
                        .orderByDesc(CustomerAddressEntity::getIsDefault)
                        .orderByDesc(CustomerAddressEntity::getId))
                .stream().map(this::toVO).toList();
    }

    @Override
    @Transactional
    public AddressVO create(Long userId, AddressCreateDTO dto) {
        boolean isDefault = Boolean.TRUE.equals(dto.isDefault());
        if (isDefault) {
            clearDefault(userId);
        }
        CustomerAddressEntity entity = new CustomerAddressEntity();
        entity.setUserId(userId);
        entity.setContactName(dto.contactName());
        entity.setContactPhone(dto.contactPhone());
        entity.setContactPhoneVerified(false);
        entity.setProvince(dto.province());
        entity.setCity(dto.city());
        entity.setDistrict(dto.district());
        entity.setDetail(dto.detail());
        entity.setIsDefault(isDefault);
        entity.setStatus("ENABLED");
        entity.setVersion(0);
        entity.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));
        entity.setUpdatedAt(LocalDateTime.now(ZoneOffset.UTC));
        addressMapper.insert(entity);
        auditLogService.recordCustomer(userId, "ADDRESS_CREATED", "CUSTOMER_ADDRESS", entity.getId(),
                null, Map.of("city", dto.city(), "district", dto.district()));
        return toVO(entity);
    }

    @Override
    @Transactional
    public AddressVO update(Long userId, Long addressId, AddressUpdateDTO dto) {
        CustomerAddressEntity entity = requireOwned(userId, addressId);
        boolean isDefault = Boolean.TRUE.equals(dto.isDefault());
        if (isDefault) {
            clearDefault(userId);
        }
        int rows = addressMapper.update(null, new UpdateWrapper<CustomerAddressEntity>()
                .eq("id", addressId)
                .eq("version", dto.version())
                .set("contact_name", dto.contactName())
                .set("contact_phone", dto.contactPhone())
                .set("province", dto.province())
                .set("city", dto.city())
                .set("district", dto.district())
                .set("detail", dto.detail())
                .set("is_default", isDefault)
                .set("version", dto.version() + 1)
                .set("updated_at", LocalDateTime.now(ZoneOffset.UTC)));
        if (rows == 0) {
            throw BusinessException.versionConflict("地址已被修改，请刷新后重试");
        }
        auditLogService.recordCustomer(userId, "ADDRESS_UPDATED", "CUSTOMER_ADDRESS", addressId,
                Map.of("version", dto.version()), Map.of("version", dto.version() + 1));
        return toVO(addressMapper.selectById(entity.getId()));
    }

    @Override
    @Transactional
    public void disable(Long userId, Long addressId, DisableRequestDTO dto) {
        requireOwned(userId, addressId);
        int rows = addressMapper.update(null, new UpdateWrapper<CustomerAddressEntity>()
                .eq("id", addressId)
                .eq("version", dto.expectedVersion())
                .set("status", "DISABLED")
                .set("version", dto.expectedVersion() + 1)
                .set("updated_at", LocalDateTime.now(ZoneOffset.UTC)));
        if (rows == 0) {
            throw BusinessException.versionConflict("地址已被修改，请刷新后重试");
        }
        auditLogService.recordCustomer(userId, "ADDRESS_DISABLED", "CUSTOMER_ADDRESS", addressId,
                Map.of("status", "ENABLED"), Map.of("status", "DISABLED"));
    }

    private CustomerAddressEntity requireOwned(Long userId, Long addressId) {
        CustomerAddressEntity entity = addressMapper.selectById(addressId);
        if (entity == null) {
            throw BusinessException.notFound("地址不存在");
        }
        if (!entity.getUserId().equals(userId)) {
            throw BusinessException.forbidden("无权操作该地址");
        }
        return entity;
    }

    private void clearDefault(Long userId) {
        addressMapper.update(null, new UpdateWrapper<CustomerAddressEntity>()
                .eq("user_id", userId)
                .eq("is_default", true)
                .set("is_default", false)
                .set("updated_at", LocalDateTime.now(ZoneOffset.UTC)));
    }

    private AddressVO toVO(CustomerAddressEntity entity) {
        return new AddressVO(entity.getId(), entity.getContactName(), entity.getContactPhone(),
                Boolean.TRUE.equals(entity.getContactPhoneVerified()), entity.getProvince(), entity.getCity(),
                entity.getDistrict(), entity.getDetail(), Boolean.TRUE.equals(entity.getIsDefault()),
                entity.getVersion());
    }
}
