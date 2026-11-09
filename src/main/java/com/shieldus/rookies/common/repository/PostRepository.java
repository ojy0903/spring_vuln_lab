package com.shieldus.rookies.common.repository;

import com.shieldus.rookies.common.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, Long> {
    // WHERE is_secret = false AND title LIKE %keyword%  (바인딩 자동)
//    List<Post> findBySecretFalseAndTitleContaining(String keyword);

    @Query("""
    SELECT p
    FROM Post p
    WHERE p.secret = false AND p.title LIKE CONCAT('%', :keyword, '%')
    """)
    List<Post> findPublicPostsByTitle(@Param("keyword") String keyword);

    @Query("""
    SELECT p
    FROM Post p
    WHERE p.id = :id AND p.secret = false
    """)
    Optional<Post> findPublicById(@Param("id") Long id);
}
