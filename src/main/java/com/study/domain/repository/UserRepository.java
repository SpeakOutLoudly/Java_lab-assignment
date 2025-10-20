package com.study.domain.repository;

import com.study.domain.model.User;

import java.util.Optional;

public interface UserRepository {
    Optional<User> findById(String id);
    Optional<User> findByName(String name);
    User create(User user);

    boolean changePassword(User  user, String  oldPassword, String newPassword);

}