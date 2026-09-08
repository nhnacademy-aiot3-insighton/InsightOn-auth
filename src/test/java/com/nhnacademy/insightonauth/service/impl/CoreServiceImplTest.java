package com.nhnacademy.insightonauth.service.impl;

import com.nhnacademy.insightonauth.client.CoreClient;
import com.nhnacademy.insightonauth.dto.core.UserGroupResponse;
import com.nhnacademy.insightonauth.exception.external.CoreServiceUnavailableException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CoreServiceImplTest {

    @Mock
    private CoreClient coreClient;

    @InjectMocks
    private CoreServiceImpl coreService;

    @Test
    @DisplayName("그룹 관리자면 true 반환")
    void isGroupManager_true() {
        when(coreClient.isGroupManager(1L)).thenReturn(true);

        assertThat(coreService.isGroupManager(1L)).isTrue();
    }

    @Test
    @DisplayName("그룹 관리자가 아니면 false 반환")
    void isGroupManager_false() {
        when(coreClient.isGroupManager(1L)).thenReturn(false);

        assertThat(coreService.isGroupManager(1L)).isFalse();
    }

    @Test
    @DisplayName("Core 응답이 null이면 예외 발생")
    void isGroupManager_nullResponse_throws() {
        when(coreClient.isGroupManager(1L)).thenReturn(null);

        assertThatThrownBy(() -> coreService.isGroupManager(1L))
                .isInstanceOf(CoreServiceUnavailableException.class);
    }

    @Test
    @DisplayName("getUserGroup은 Core 응답을 그대로 반환")
    void getUserGroup_delegates() {
        UserGroupResponse response = new UserGroupResponse(true, "그룹A");
        when(coreClient.getUserGroup(1L)).thenReturn(response);

        assertThat(coreService.getUserGroup(1L)).isEqualTo(response);
    }
}
