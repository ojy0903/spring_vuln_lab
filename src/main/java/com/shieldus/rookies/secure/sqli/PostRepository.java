package com.shieldus.rookies.secure.sqli;

import com.shieldus.rookies.common.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long> {
    // WHERE is_secret = false AND title LIKE %keyword%  (바인딩 자동)
//    List<Post> findBySecretFalseAndTitleContaining(String keyword);

    @Query("""
    SELECT p
    FROM Post p
    WHERE p.secret = false AND p.title LIKE :keyword
    """)
    List<Post> findPublicPostsByTitle(@Param("keyword") String keyword);
}
