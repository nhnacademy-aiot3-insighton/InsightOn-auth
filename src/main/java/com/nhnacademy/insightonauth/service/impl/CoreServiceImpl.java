package com.nhnacademy.insightonauth.service.impl;

import com.nhnacademy.insightonauth.client.CoreClient;
import com.nhnacademy.insightonauth.dto.core.UserGroupResponse;
import com.nhnacademy.insightonauth.exception.CoreServiceUnavailableException;
import com.nhnacademy.insightonauth.service.CoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * core 서버(CoreClient) 호출 전용 서비스.
 * 모든 메서드에 NOT_SUPPORTED를 걸어, 호출하는 쪽의 DB 트랜잭션을
 * 잠시 중단시킨 채로 Feign 호출을 실행한다.
 * core가 느려지거나 장애가 나도 DB 커넥션을 붙잡지 않도록 하기 위함.
 */
@Service
@RequiredArgsConstructor
public class CoreServiceImpl implements CoreService {

    private final CoreClient coreClient;

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public boolean isGroupManager(Long userId) {
        Boolean result = coreClient.isGroupManager(userId);
        if (result == null) {
            throw new CoreServiceUnavailableException(
                    "Core 서비스로부터 그룹 관리자 여부를 확인할 수 없습니다. userId=" + userId);
        }
        return result;
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public UserGroupResponse getUserGroup(Long userId) {
        return coreClient.getUserGroup(userId);
    }
}
