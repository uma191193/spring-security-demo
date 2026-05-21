package org.example.service;

import org.example.UserEntity;
import org.springframework.stereotype.Service;

@Service
public interface UserService {
    UserEntity saveUser(UserEntity userEntity);
}
