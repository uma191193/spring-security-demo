package org.example.service;

import org.example.UserEntity;
import org.example.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    public UserRepository userRepository;

    @Autowired
    public PasswordEncoder encoder;

    @Override
    public UserEntity saveUser(UserEntity userEntity) {
        String encodedPassword = encoder.encode(userEntity.getPassword());
        userEntity.setPassword(encodedPassword);
        return userRepository.save(userEntity);
    }
}
