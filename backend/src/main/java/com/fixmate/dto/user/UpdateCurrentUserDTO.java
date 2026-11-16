package com.fixmate.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;

public record UpdateCurrentUserDTO(@NotBlank @Size(max=64) String nickname,
        @com.fasterxml.jackson.annotation.JsonSetter(nulls=com.fasterxml.jackson.annotation.Nulls.FAIL) @Pattern(regexp="^[1-9][0-9]{0,18}$") String avatarFileId) {}
