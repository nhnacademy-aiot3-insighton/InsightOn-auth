package service.impl;

import entity.User;
import service.UserService;

public class UserServiceImpl implements UserService {
    @Override
    public User createMember(String email, String userName) {
        return null;
    }

    @Override
    public User createAdmin(String email, String userName) {
        return null;
    }

    @Override
    public User findById(String id) {
        return null;
    }

    @Override
    public User findByEmail(String email) {
        return null;
    }

    @Override
    public void updateUserName(String id, String newUserName) {

    }

    @Override
    public void withdraw(String id) {

    }

    @Override
    public void sleep(String id) {

    }

    @Override
    public void block(String id) {

    }
}
