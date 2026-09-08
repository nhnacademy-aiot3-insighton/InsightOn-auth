package com.nhnacademy.insightonauth.service;

import com.nhnacademy.insightonauth.dto.core.UserGroupResponse;

public interface CoreService {

    boolean isGroupManager(Long userId);

    UserGroupResponse getUserGroup(Long userId);
}
