package service;

import entity.User;

public interface UserService {

    User createMember(String email, String userName);

    User createAdmin(String email, String userName);

    User findById(String id);

    User findByEmail(String email);

    void updateUserName(String id, String newUserName);

    void withdraw(String id);

    void sleep(String id);

    void block(String id);
}
