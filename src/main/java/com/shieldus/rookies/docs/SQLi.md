# SQL Injection (SQLi)

## 0. 개념 정리

**SQL Injection**은 애플리케이션이 사용자 입력값을 SQL 쿼리의 일부로 **그대로 이어 붙여(문자열 연결)** 실행할 때, 공격자가 입력값에 SQL 구문을 끼워 넣어 **쿼리의 의미 자체를 바꾸는** 공격이다.

핵심 원인은 **"코드(SQL 구문)"와 "데이터(사용자 입력)"의 경계가 무너지는 것.**
개발자는 입력값을 단순한 "검색어 데이터"로 의도했지만, DB 입장에서는 그게 데이터인지 구문인지 구분할 방법이 없다. 따옴표 하나(')로 데이터 영역을 빠져나와 뒤에 오는 문자열이 SQL 구문으로 해석되면 공격이 성립한다.

주요 유형:
- **In-band (UNION 기반)**: `UNION SELECT`로 다른 테이블/컬럼 데이터를 결과에 합쳐서 직접 빼낸다.
- **Boolean/Error 기반 Blind**: 결과가 직접 안 보일 때 참/거짓 응답 차이나 에러 메시지로 한 글자씩 추론한다.
- **Time 기반 Blind**: `SLEEP()` 등 지연 함수의 응답 시간 차이로 추론한다.

영향: 인증 우회, 비공개 데이터 유출(이 프로젝트에선 `is_secret=true` 비밀글), 데이터 변조·삭제, 경우에 따라 DB 서버 장악

OWASP Top 10에서는 **A05:2025 – Injection** 카테고리에 해당한다.

---

## 1. 취약 코드 + 취약한 이유

### 대상 기능
게시글 **제목 검색** 기능. 공개글(`is_secret = false`)만 검색되도록 의도했다.

### 취약 코드
`vuln/sqli/VulnPostSearchService.java`

```java
public List<Map<String, Object>> searchPost(String keyword) {
    //취약점: 사용자 입력을 쿼리 문자열에 직접 연결 -> SQL Injection 공격 가능
    String sql = "SELECT post_id, title, content, is_secret "
                + "FROM posts "
                + "WHERE is_secret = false "   // 공개글만 조회 가능하도록 is_secret 고정
                + "AND title LIKE '%" + keyword + "%'";

    return jdbcTemplate.queryForList(sql);
}
```

`vuln/sqli/SqliPageController.java` — 입력값을 아무 검증 없이 서비스로 전달한다.

```java
@GetMapping("/vuln/sqli/search")
public String search(@RequestParam String keyword, Model model) {
    model.addAttribute("keyword", keyword);
    model.addAttribute("results", postSearchService.searchPost(keyword));
    return "vuln/sqli";
}
```

### 왜 취약한가
- 사용자가 보낸 `keyword`가 쿼리 문자열에 직접 연결(`+ keyword +`)된다. 작은따옴표를 포함한 입력이 들어오면 문자열 리터럴을 탈출해 뒤 문자열이 **SQL 구문으로 해석**된다.
- `WHERE is_secret = false` 조건으로 공개글만 노출하려 했지만, 이 조건 역시 **문자열의 일부일 뿐**이라 공격자가 `OR`/`UNION`으로 손쉽게 무력화할 수 있다.
- JPA/JPQL은 기본적으로 파라미터 바인딩이라 안전하지만, 여기서는 **`JdbcTemplate` + 문자열 연결**을 일부러 사용해 취약점을 만들었다.

---

## 2. 공격 재현

검색 폼은 `GET /vuln/sqli/search?keyword=...` 로 동작한다. `keyword` 파라미터에 페이로드를 넣는다.

### (A) 조건 우회 — 전체 글 노출 (비밀글 포함)

입력:
```
' OR '1'='1
```

완성되는 쿼리:
```sql
SELECT post_id, title, content, is_secret
FROM posts
WHERE is_secret = false AND title LIKE '%' OR '1'='1%'
```

`AND`보다 `OR`가 넓게 걸리면서 `'1'='1'`이 항상 참 → `is_secret = false` 제약이 무력화되고 **비밀글(`is_secret=true`)까지 전부 조회**된다.

### (B) UNION 기반 — 다른 테이블 데이터 유출

`users` 테이블의 계정/비밀번호를 게시글 검색 결과에 섞어서 빼낸다. 원 쿼리가 4개 컬럼(`post_id, title, content, is_secret`)을 반환하므로 컬럼 수를 맞춘다.

입력:
```
' UNION SELECT user_id, username, password, role FROM users -- 
```

완성되는 쿼리:
```sql
SELECT post_id, title, content, is_secret
FROM posts
WHERE is_secret = false AND title LIKE '%'
UNION SELECT user_id, username, password, role FROM users -- %'
```

`-- `(공백 포함) 뒤는 주석 처리되어 꼬리의 `%'`가 무시된다. 결과 테이블의 title/content 칸에 **사용자명과 비밀번호가 그대로 노출**된다.

### (C) 결과
- (A): 의도한 공개글 외에 비밀글 전부가 화면에 뜬다.
- (B): 게시글과 무관한 `users` 테이블 데이터가 검색 결과로 출력된다.


---

## 3. 원인 분석

1. **구문과 데이터의 미분리**: 입력값이 SQL 쿼리 "문자열"에 직접 들어가 DB가 구문/데이터를 구분하지 못한다.
2. **신뢰 경계 위반**: 클라이언트가 보낸 값은 전부 불신 대상인데, 검증·바인딩 없이 쿼리에 편입했다.
3. **방어가 문자열 조건에만 의존**: `is_secret = false` 같은 보안 조건을 문자열 연결로 표현해, 입력 조작으로 깨진다.
4. **최소 권한 부재 가능성**: 애플리케이션 DB 계정이 `users` 등 불필요한 테이블까지 읽을 수 있으면 UNION 공격 피해가 커진다.

---

## 4. 패치 (안전 코드 + 차이점)

핵심은 **입력값을 쿼리 구문이 아니라 "바인딩되는 값"으로 넘기는 것** — 파라미터화 쿼리(Prepared Statement) / ORM 바인딩.

### 4-1. JDBC — `?` 플레이스홀더 바인딩
`secure/sqli/SecurePostSearchService.java`

```java
public List<Map<String, Object>> searchByJdbc(String keyword) {
    String sql = "SELECT post_id, title, content, is_secret "
            + "FROM posts "
            + "WHERE is_secret = false "
            + "AND title LIKE ?";   // 키워드 자리를 ? 로 비워 둠

    // 와일드카드(%)는 SQL문이 아니라 '바인딩될 값'에 붙인다
    return jdbcTemplate.queryForList(sql, "%" + keyword + "%");
}
```

### 4-2. JPA — `@Query` 명시 + `:param` 바인딩
`secure/sqli/PostRepository.java`

```java
@Query("""
    SELECT p
    FROM Post p
    WHERE p.secret = false AND p.title LIKE :keyword
    """)
List<Post> findPublicPostsByTitle(@Param("keyword") String keyword);
```

`secure/sqli/SecurePostSearchService.java`

```java
public List<PostResponse> searchByJpa(String keyword) {
    List<Post> posts = postRepository.findPublicPostsByTitle(keyword);
    List<PostResponse> responses = new ArrayList<>();
    for (Post post : posts) {
        responses.add(PostResponse.from(post));
    }
    return responses;
}
```

> 엔티티를 그대로 뷰에 노출하지 않고 `PostResponse` DTO(record)로 변환해 필요한 필드만 내보낸다.

### 취약 vs 안전

| 구분 | 취약 (`vuln`) | 안전 (`secure`) |
|---|---|---|
| 입력 처리 | 쿼리 문자열에 직접 연결 (`+ keyword +`) | `?` / `:keyword` 바인딩 |
| 구문·데이터 경계 | 구분 X (입력이 구문이 됨) | 분리됨 (입력은 항상 값) |
| 와일드카드 `%` | SQL 문자열에 포함 | 바인딩 값 쪽에 부착 |
| `is_secret` 조건 | 입력 조작으로 우회 가능 | 항상 유지됨 |
| `' OR '1'='1` | 조건 우회 성공 | 단순 검색어로 취급(결과 0건) |
| `UNION SELECT ...` | 타 테이블 유출 | 전체가 하나의 리터럴 값으로 매칭 |

바인딩을 쓰면 `' OR '1'='1`을 넣어도 "제목에 `' OR '1'='1` 문자열이 포함된 글"을 찾는 평범한 검색이 되어, 구문으로 해석되지 않는다.

---

## 5. 대응 가이드 (OWASP / KISA)

1. **파라미터화 쿼리(Prepared Statement) 사용** — 문자열 연결로 쿼리를 만들지 않는다.
2. **ORM / 바인딩 API 활용** — JPA/JPQL, `@Query` + `@Param`은 기본이 바인딩. 
3. **입력 검증(화이트리스트)** — 정렬 컬럼명·방향처럼 바인딩이 불가능한 **식별자 자리**는 허용 목록으로 검증.
4. **최소 권한 원칙** — 애플리케이션 DB 계정에 꼭 필요한 테이블/권한만 부여해 UNION 등 피해 범위를 줄인다.
5. **에러 메시지 최소화** — DB 에러 원문을 사용자에게 노출하지 않아 Error 기반 SQLi 단서를 차단.
6. **WAF 등 심층 방어** — 보조 수단일 뿐, 근본 대책은 파라미터화임을 전제로 사용.

참조: OWASP Top 10 A05:2025 – Injection(구 A03:2021), OWASP SQL Injection Prevention Cheat Sheet, KISA 『소프트웨어 개발보안 가이드』 SQL 삽입 항목.