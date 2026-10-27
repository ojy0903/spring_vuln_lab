package com.shieldus.rookies.vuln.sqli;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Profile("local")
@Service
@RequiredArgsConstructor
public class VulnPostSearchService {

    private final JdbcTemplate jdbcTemplate;

    public List<Map<String, Object>> searchPost(String keyword) {
        //취약점: 사용자 입력을 쿼리 문자열에 직접 연결 -> SQL Injection 공격 가능
        String sql = "SELECT post_id, title, content, is_secret "
                    + "FROM posts "
                    + "WHERE is_secret = false "  // 공개글만 조회 가능하도록 is_secret 고정
                    + "AND title LIKE '%" + keyword + "%'";

        return jdbcTemplate.queryForList(sql);
    }
}
