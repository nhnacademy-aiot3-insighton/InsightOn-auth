package com.nhnacademy.insightonauth.dto.admin;

import com.nhnacademy.insightonauth.entity.Role;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * 회원 권한 전체 교체 요청. 목록에 담긴 권한이 최종 상태가 된다
 * (기존에 있고 목록에도 있으면 유지, 목록에 없으면 삭제, 목록에만 있으면 추가).
 */
public record RolesUpdateRequest(
        @NotEmpty(message = "권한을 1개 이상 지정해주세요.")
        List<Role> roles
) {
}
