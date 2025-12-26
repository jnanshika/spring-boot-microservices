package com.pm.auth2service.repository;

import com.pm.auth2service.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User,String> {
    User findByEmail(String email);
}
