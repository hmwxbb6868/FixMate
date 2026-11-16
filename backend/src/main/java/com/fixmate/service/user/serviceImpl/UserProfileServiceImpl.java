package com.fixmate.service.user.serviceImpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.fixmate.common.audit.AuditLogService;
import com.fixmate.dto.user.CurrentUserVO;
import com.fixmate.dto.user.UpdateCurrentUserDTO;
import com.fixmate.entity.CustomerUserEntity;
import com.fixmate.entity.FileAssetEntity;
import com.fixmate.entity.RepairRequestMediaEntity;
import com.fixmate.exception.BusinessException;
import com.fixmate.mapper.CustomerUserMapper;
import com.fixmate.mapper.FileAssetMapper;
import com.fixmate.mapper.RepairRequestMediaMapper;
import com.fixmate.service.user.UserProfileService;
import com.fixmate.storage.ObjectStorageService;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserProfileServiceImpl implements UserProfileService {
    private final CustomerUserMapper mapper;
    private final FileAssetMapper files;
    private final RepairRequestMediaMapper media;
    private final ObjectStorageService storage;
    private final AuditLogService audit;
    public UserProfileServiceImpl(CustomerUserMapper mapper, FileAssetMapper files,
            RepairRequestMediaMapper media, ObjectStorageService storage, AuditLogService audit) {
        this.mapper=mapper;this.files=files;this.media=media;this.storage=storage;this.audit=audit;
    }
    private void requireActive(CustomerUserEntity user) {
        if(user==null||!"ENABLED".equals(user.getStatus()))throw BusinessException.forbidden("账号不可用");
    }
    @Override public CurrentUserVO current(Long userId) {
        CustomerUserEntity user=mapper.selectById(userId);requireActive(user);return view(user);
    }
    private CurrentUserVO view(CustomerUserEntity user) {
        String url=user.getAvatarUrl();
        if(user.getAvatarFileId()!=null) {
            url=null;
            FileAssetEntity file=files.selectById(user.getAvatarFileId());
            if(file!=null&&user.getId().equals(file.getUploaderId())&&"CUSTOMER".equals(file.getUploaderType())
                    &&"AVATAR".equals(file.getPurpose())&&"ACTIVE".equals(file.getStatus())) {
                try {url=storage.generateSignedUrl(file.getStorageKey(),Duration.ofMinutes(5));}
                catch(RuntimeException ignored) { /* 保留真实关联，读取失败时显示默认头像，可刷新重取。 */ }
            }
        }
        return new CurrentUserVO(user.getId(),user.getNickname(),url,user.getPhone(),Boolean.TRUE.equals(user.getPhoneVerified()));
    }
    @Override @Transactional
    public CurrentUserVO update(Long userId, UpdateCurrentUserDTO dto) {
        String nickname=dto.nickname()==null?"":dto.nickname().strip();
        if(nickname.isEmpty()||nickname.length()>64||nickname.codePoints().anyMatch(Character::isISOControl))
            throw BusinessException.validation("昵称需为1至64个字符，不能包含控制字符");
        CustomerUserEntity user=mapper.selectOne(new LambdaQueryWrapper<CustomerUserEntity>()
                .eq(CustomerUserEntity::getId,userId).last("FOR UPDATE"));requireActive(user);
        Long previous=user.getAvatarFileId(),next=previous;
        if(dto.avatarFileId()!=null) {
            try {next=Long.valueOf(dto.avatarFileId());if(next<=0)throw new NumberFormatException();}
            catch(NumberFormatException ex){throw BusinessException.validation("头像文件编号不合法");}
            SortedSet<Long> ids=new TreeSet<>();if(previous!=null)ids.add(previous);ids.add(next);
            Map<Long,FileAssetEntity> locked=new HashMap<>();
            for(Long id:ids)locked.put(id,files.selectOne(new LambdaQueryWrapper<FileAssetEntity>()
                    .eq(FileAssetEntity::getId,id).last("FOR UPDATE")));
            FileAssetEntity file=locked.get(next);
            if(file==null||!userId.equals(file.getUploaderId())||!"CUSTOMER".equals(file.getUploaderType()))
                throw BusinessException.forbidden("无权使用该头像文件");
            if(!"AVATAR".equals(file.getPurpose())||!Set.of("TEMP","ACTIVE").contains(file.getStatus())
                    ||file.getEtag()==null||file.getEtag().isBlank()||file.getMimeType()==null||!file.getMimeType().startsWith("image/"))
                throw BusinessException.validation("头像文件未上传完成或已失效，请重新选择");
            if("TEMP".equals(file.getStatus())) {
                int changed=files.update(null,new UpdateWrapper<FileAssetEntity>().eq("id",next).eq("status","TEMP")
                    .set("status","ACTIVE").set("updated_at",LocalDateTime.now(ZoneOffset.UTC)));
                if(changed!=1)throw BusinessException.stateConflict("头像已变化，请重新选择");
            }
        }
        var update=new UpdateWrapper<CustomerUserEntity>().eq("id",userId).eq("status","ENABLED")
            .set("nickname",nickname).set("updated_at",LocalDateTime.now(ZoneOffset.UTC));
        if(dto.avatarFileId()!=null)update.set("avatar_file_id",next);
        if(mapper.update(null,update)!=1)throw BusinessException.stateConflict("资料状态已变化，请重新读取");
        user.setNickname(nickname);user.setAvatarFileId(next);
        if(previous!=null&&!previous.equals(next)) {
            long references=mapper.selectCount(new LambdaQueryWrapper<CustomerUserEntity>().eq(CustomerUserEntity::getAvatarFileId,previous));
            long repairRefs=media.selectCount(new LambdaQueryWrapper<RepairRequestMediaEntity>().eq(RepairRequestMediaEntity::getFileId,previous));
            if(references==0&&repairRefs==0)files.update(null,new UpdateWrapper<FileAssetEntity>()
                .eq("id",previous).eq("status","ACTIVE").set("status","TEMP").set("updated_at",LocalDateTime.now(ZoneOffset.UTC)));
        }
        audit.recordCustomer(userId,"CUSTOMER_PROFILE_UPDATED","CUSTOMER_USER",userId,null,
                Map.of("nicknameChanged",true,"avatarChanged",!Objects.equals(previous,next)));
        return view(user);
    }
}
