package com.nhnacademy.insightonauth.service.impl;

import com.nhnacademy.insightonauth.entity.Role;
import com.nhnacademy.insightonauth.entity.User;
import com.nhnacademy.insightonauth.entity.UserRole;
import com.nhnacademy.insightonauth.exception.user.UserRoleNotFoundException;
import com.nhnacademy.insightonauth.repository.UserRoleRepository;
import com.nhnacademy.insightonauth.service.UserRoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class UserRoleServiceImpl implements UserRoleService {

    private final UserRoleRepository userRoleRepository;

    @Override
    public void create(User user, Role role) {
        UserRole userRole = new UserRole(user, role);
        userRoleRepository.save(userRole);
    }

    @Override
    public void addRole(User user, Role role) {
        boolean alreadyHas = userRoleRepository.existsByUserAndRole(user, role);
        if (alreadyHas) {
            //이미 존재하면 그냥 넘김
            return;
        }
        UserRole userRole = new UserRole(user, role);
        userRoleRepository.save(userRole);
    }

    @Override
    public void removeRole(User user, Role role) {
        UserRole userRole = userRoleRepository.findByUserAndRole(user, role)
                .orElseThrow(() -> new UserRoleNotFoundException("해당 권한이 존재하지 않습니다."));

        userRoleRepository.delete(userRole);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserRole> findByUser(User user) {
        List<UserRole> userRoleList = userRoleRepository.findByUser(user);

        return userRoleList;
    }

    @Override
    public void deleteUserRole(User user) {
        List<UserRole> userRoleList = userRoleRepository.findByUser(user);
        userRoleRepository.deleteAll(userRoleList);
    }
}
