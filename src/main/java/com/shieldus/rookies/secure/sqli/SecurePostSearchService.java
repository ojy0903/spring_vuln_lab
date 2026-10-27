package com.shieldus.rookies.secure.sqli;

import com.shieldus.rookies.common.entity.Post;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SecurePostSearchService {

    private final PostRepository postRepository;
    private final JdbcTemplate jdbcTemplate;

    // 1 JDBC: ? 플레이스홀더 바인딩
    public List<Map<String, Object>> searchByJdbc(String keyword) {
        String sql = "SELECT post_id, title, content, is_secret "
                + "FROM posts "
                + "WHERE is_secret = false "  // 공개글만 조회 가능하도록 is_secret 고정
                + "AND title LIKE ?";  // 조회 키워드 자리를 ? 로 비워서 SQL 문 준비

        // 와일드카드(%)는 SQL문이 아니라 '바인딩될 값'에 붙인다
        return jdbcTemplate.queryForList(sql, "%" + keyword + "%");
    }

    // 2. JPA: 파생 쿼리 (파라미터 바인딩 자동)
    public List<PostResponse> searchByJpa(String keyword) {
//        return postRepository.findBySecretFalseAndTitleContaining(keyword).stream()
//                .map(PostResponse::from)
//                .toList();

        List<Post> posts = postRepository.findBySecretFalseAndTitleContaining(keyword);
        List<PostResponse> responses = new ArrayList<>();
        for (Post post : posts) {
            PostResponse response = PostResponse.from(post);
            responses.add(response);
        }

        return responses;
    }
}
