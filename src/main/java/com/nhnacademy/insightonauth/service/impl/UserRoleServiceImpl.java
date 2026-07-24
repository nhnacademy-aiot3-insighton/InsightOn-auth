package com.nhnacademy.insightonauth.service.impl;

import com.nhnacademy.insightonauth.entity.Role;
import com.nhnacademy.insightonauth.entity.User;
import com.nhnacademy.insightonauth.entity.UserRole;
import com.nhnacademy.insightonauth.repository.UserRepository;
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
    public List<UserRole> findByUser(User user) {
        List<UserRole> userRoleList = userRoleRepository.findByUser(user);

        return userRoleList;
    }
}
