package com.socialmedia.service;

import com.socialmedia.entity.User;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * No-op UserService when database is not configured. Persistence is skipped.
 */
@Service
@ConditionalOnMissingBean(UserService.class)
public class StubUserService extends UserService {

    public StubUserService() {
        super(null);
    }

    @Override
    public User saveOrUpdateUser(User user) {
        return user;
    }

    @Override
    public User saveOrUpdateUserByUsername(User user) {
        return user;
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return Optional.empty();
    }
}
