package com.socialmedia.service;

import com.socialmedia.entity.User;
import com.socialmedia.repository.UserRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.util.Optional;

@Service
@ConditionalOnBean(DataSource.class)
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public User saveOrUpdateUser(User user) {
        if (user.getId() == null) {
            return userRepository.save(user);
        }

        Optional<User> existingUserOpt = userRepository.findById(user.getId());
        if (existingUserOpt.isPresent()) {
            User existingUser = existingUserOpt.get();
            user.setFirstSeenAt(existingUser.getFirstSeenAt());
            return userRepository.save(user);
        }
        return userRepository.save(user);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public User saveOrUpdateUserByUsername(User user) {
        Optional<User> existingUserOpt = userRepository.findByUserName(user.getUserName());

        if (existingUserOpt.isPresent()) {
            User existingUser = existingUserOpt.get();
            user.setId(existingUser.getId());
            user.setFirstSeenAt(existingUser.getFirstSeenAt());
            user.setTweets(null);
            return userRepository.save(user);
        }
        return userRepository.save(user);
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUserName(username);
    }
}

