package com.nhnacademy.insightonauth.service;

import com.nhnacademy.insightonauth.entity.Role;
import com.nhnacademy.insightonauth.entity.User;
import com.nhnacademy.insightonauth.entity.UserRole;

import java.util.List;

public interface UserRoleService {

    void create(User user, Role role);

    List<UserRole> findByUser(User user);
}
