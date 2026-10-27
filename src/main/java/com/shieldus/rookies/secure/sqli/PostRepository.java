package com.shieldus.rookies.secure.sqli;

import com.shieldus.rookies.common.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long> {
    // WHERE is_secret = false AND title LIKE %keyword%  (바인딩 자동)
    List<Post> findBySecretFalseAndTitleContaining(String keyword);
}
