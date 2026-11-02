package com.shieldus.rookies.common.repository;

import com.shieldus.rookies.common.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}
