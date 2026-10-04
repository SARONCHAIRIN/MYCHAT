package com.rindev.chat.dto.request;

import com.rindev.chat.enums.MemberRole;
import jakarta.validation.constraints.NotNull;

public record UpdateMemberRoleRequest(
        @NotNull(message = "Role is required") MemberRole role) {
}