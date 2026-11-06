# Cross-Site Scripting (XSS)

## 0. 개념 정리

**XSS(Cross-Site Scripting)**는 애플리케이션이 사용자 입력값을 **HTML/JS 응답에 이스케이프 없이 그대로 출력**할 때, 공격자가 입력에 스크립트를 끼워 넣어 **피해자 브라우저에서 임의의 JavaScript를 실행**시키는 공격이다.

핵심 원인은 SQLi와 같이 **"코드와 데이터의 경계 붕괴"** 이다. 대상이 DB 쿼리가 아닌 **브라우저가 받는 HTML 문서**라는 점만 다르다. 개발자는 입력값을 단순한 표시용 텍스트로 의도했지만, 브라우저 입장에서는 그게 텍스트인지 `<script>` 태그인지 구분할 방법이 없다. 꺾쇠(`<`) 하나로 뒤 문자열이 마크업으로 해석되면 공격이 성립한다.

주요 유형:
- **Reflected(반사형)**: 페이로드가 저장되지 않고 **요청(URL 파라미터 등)에 담겨 그 응답에 즉시 되돌아온다.** 공격자가 만든 악성 링크를 피해자가 클릭할 때 실행된다.
- **Stored(저장형)**: 페이로드가 **DB에 저장**되어, 그 콘텐츠를 보는 **모든 사용자**에게 반복 실행된다. 영향 범위가 가장 크다.
- **DOM 기반**: 서버가 아니라 **클라이언트 JS**가 `innerHTML` 등으로 입력을 DOM에 넣을 때 발생한다.

영향: 세션 쿠키 탈취, 피해자 권한으로의 요청 위조(계정 탈취·설정 변경), 피싱 화면 삽입, 키로깅 등 — 모두 피해자 브라우저 안에서 실행된다.

---

## 1. 취약 코드 + 취약한 이유

### 대상 기능
게시판의 **① 제목 검색(Reflected)** 과 **② 글 상세 보기(Stored)**. 본문에 `<b>`, `<i>` 같은 HTML 서식을 허용하려는 의도로 출력 시 이스케이프를 끄면서 취약점이 발생한다.

### 취약 코드

`vuln/xss/XssPageController.java` — 입력을 HTML 문자열로 조립해 모델에 담는다.

```java
@GetMapping("/vuln/xss") // 목록 + 검색 (Reflected XSS)
public String list(@RequestParam(defaultValue = "") String keyword, Model model) {
    if (!keyword.isEmpty()) {
        // 취약점: 사용자 입력을 HTML 문자열에 직접 연결 -> 템플릿에서 th:utext 로 출력
        model.addAttribute("message", "'<b>" + keyword + "</b>' 검색 결과");
    }
    model.addAttribute("keyword", keyword);
    model.addAttribute("posts", postService.search(keyword));
    return "vuln/xss";
}

@GetMapping("/vuln/xss/posts/{id}") // 상세 (Stored XSS 실행)
public String detail(@PathVariable long id, Model model) {
    model.addAttribute("post", postService.findById(id));
    return "vuln/xss-post";
}
```

`vuln/xss/XssPostService.java` — 작성 글을 **원문 그대로 저장**한다(정제 없음).

```java
@Transactional
public void create(String title, String content) {
    Post post = Post.builder()
            .title(title)
            .content(content)   // 원문 그대로 저장
            .author(userRepository.getReferenceById(FIXED_AUTHOR_ID))
            .secret(false)
            .build();
    postRepository.save(post);
}
```

템플릿 — 출력 시 **`th:utext`(이스케이프 안 함)** 가 싱크다.

```html
<!-- templates/vuln/xss.html : Reflected 싱크 -->
<p th:if="${message != null}" th:utext="${message}"></p>

<!-- templates/vuln/xss-post.html : Stored 싱크 -->
<div class="content" th:utext="${post.content}"></div>
```

### 왜 취약한가
- Thymeleaf의 `th:text`는 자동으로 HTML 이스케이프하지만, **`th:utext`(unescaped text)는 값을 그대로** 출력한다. 서식 태그(`<b>`)를 살리려던 의도가 `<script>`·이벤트 핸들러까지 통과시킨다.
- Reflected: 컨트롤러가 `"'<b>" + keyword + "</b>'..."` 로 **HTML을 문자열로 조립**해, 마크업과 사용자 데이터가 뒤섞인 채 `th:utext`로 나간다.
- Stored: 입력은 원문 저장 + 출력도 `th:utext` -> 한 번 심으면 **보는 사람마다 영구 실행**된다.

---

## 2. 공격 재현

### (A) Reflected — 악성 링크 클릭 시 실행

요청(`keyword`에 페이로드, 실제로는 URL 인코딩):
```
GET /vuln/xss?keyword=<script>alert(1)</script>
```

완성되는 HTML(message 영역):
```html
<p>'<b><script>alert(1)</script></b>' 검색 결과</p>
```

서버가 보낸 **최초 문서에 `<script>`가 직접 포함**되므로 브라우저가 파싱 중 실행한다 → `alert(1)`. `alert`를 쿠키 접근(`document.cookie`)이나 외부 전송 코드로 바꾸면 **세션 탈취**로 이어진다. 저장이 없으므로 매번 실행시키려면 피해자가 그 URL을 열어야 한다(공격자가 링크를 유도).

### (B) Stored — 글을 보는 모두에게 실행

1) 글 작성:
```
POST /vuln/xss/posts
title=안녕하세요&content=<img src=x onerror=alert(document.cookie)>
```
2) 누구든 상세 페이지 열람:
```
GET /vuln/xss/posts/{id}
```
완성되는 HTML:
```html
<div class="content"><img src=x onerror=alert(document.cookie)></div>
```
이미지 로드 실패(`src=x`) → `onerror` 핸들러 실행. `<script>` 보다 `<img onerror>`가 삽입 맥락에 따라(특히 DOM 기반) `<script>`가 실행되지 않는 경우에도 범용적으로 사용이 가능하다.

### (C) 결과
- (A): 링크를 연 피해자 브라우저에서 스크립트 실행.
- (B): 해당 글을 여는 **모든 사용자**에게 반복 실행.

---

## 3. 원인 분석

1. **출력 인코딩 부재**: `th:utext`로 입력을 그대로 출력 -> HTML 맥락에서 코드/데이터 구분이 불가해진다.
2. **서버의 HTML 문자열 조립**: `"'<b>" + keyword + ...` 처럼 마크업과 데이터를 문자열로 섞어, 데이터가 구조(태그)로 승격된다.
3. **신뢰 경계 위반**: 클라이언트 입력은 전부 불신 대상인데 검증·인코딩 없이 화면에 편입했다.
4. **맥락(context) 인식 부재**: 같은 입력도 HTML 본문/속성/JS/URL 맥락마다 필요한 인코딩이 다른데 이를 구분하지 않았다.
5. **입력 저장 원문화**: Stored의 경우 출력 시점 방어가 유일한 방어선인데 그마저 없다.

---

## 4. 패치 (안전 코드 + 차이점)

핵심은 **"출력 맥락에 맞게 처리"** - 순수 텍스트는 자동 이스케이프, HTML 서식이 꼭 필요하면 허용목록 정제.

### 4-1. Reflected — 마크업은 템플릿에 고정, 값은 `th:text`로 이스케이프

`secure/xss/SecXssPageController.java` (HTML을 조립하지 않고 값만 전달)
```java
@GetMapping("/secure/xss")
public String list(@RequestParam(defaultValue = "") String keyword, Model model) {
    model.addAttribute("keyword", keyword);          // 조립 X, 값만 전달
    model.addAttribute("posts", postService.search(keyword));
    return "secure/xss";
}
```

`templates/secure/xss.html`
```html
<!-- 마크업(<b>)은 템플릿에 고정, 사용자 입력은 th:text로 이스케이프해서 끼워 넣음 -->
<p th:if="${!#strings.isEmpty(keyword)}">'<b th:text="${keyword}"></b>' 검색 결과</p>
```
`<script>`는 `&lt;script&gt;`로 출력되어 **텍스트로만 보이고 실행되지 않는다.**

### 4-2. Stored — 출력 직전 허용목록(Safelist) 기반 정제

`secure/xss/SecXssPostService.java`
```java
public PostResponse findById(Long id) {
    Post post = postRepository.findPublicById(id).orElseThrow();

    // basic(): <b>,<i>,<u>,<p>,<br>,<ul>,<a href> 등 서식 태그만 허용
    // <script>, <img>, on* 이벤트 속성, javascript: 링크는 제거
    // prettyPrint(false): jsoup의 줄바꿈 자동 정리 방지(본문 줄바꿈 보존)
    String safeContent = Jsoup.clean(post.getContent(), "", Safelist.basic(),
            new Document.OutputSettings().prettyPrint(false));

    return new PostResponse(post.getId(), post.getTitle(), safeContent, post.isSecret());
}
```
저장은 원문 그대로 두고 **정제는 출력 시점**에 수행한다. 서식 태그는 살리되 스크립트/핸들러는 제거되므로 `th:utext`를 유지해도 안전하다.

### 4-3. 2차 방어(심층 방어) — CSP

`templates/secure/*.html`
```html
<!-- 인라인 스크립트·on* 핸들러 실행 차단 → 이스케이프가 뚫려도 스크립트가 실행되지 않음 -->
<meta http-equiv="Content-Security-Policy" content="script-src 'self'">
```

### 취약 vs 안전

| 구분 | 취약 (`vuln`) | 안전 (`secure`) |
|---|---|---|
| 검색어 출력 | `th:utext` (날것) | `th:text` (자동 이스케이프) |
| HTML 조립 | 컨트롤러가 문자열 연결 | 마크업은 템플릿 고정, 값만 전달 |
| 본문 저장 | 원문 그대로 | 원문 저장(정제는 출력 시점) |
| 본문 출력 | `th:utext` (정제 없음) | jsoup `Safelist.basic()` 정제 후 출력 |
| `<script>alert(1)</script>` | 실행됨 | `&lt;script&gt;...`로 표시(실행 X) / 정제 시 제거 |
| 2차 방어 | 없음 | CSP `script-src 'self'` |


---

## 5. 대응 가이드 (OWASP / KISA)

1. **출력 인코딩(맥락별)이 기본** — HTML 본문/속성/JS/URL 각 맥락에 맞게 이스케이프. 템플릿 자동 이스케이프(`th:text`)를 신뢰하고 `th:utext`는 지양.
2. **HTML 서식 허용이 필요하면 허용목록 정제** — jsoup `Safelist`, OWASP Java HTML Sanitizer. 블랙리스트/직접 필터링은 우회당한다.
3. **입력 검증은 보조 수단** — 근본 대책은 출력 측 처리. 검증만으로 XSS를 막으려 하지 않는다.
4. **CSP(Content-Security-Policy)** — `script-src`에 `'unsafe-inline'` 금지. 이스케이프가 뚫려도 실행을 차단하는 심층 방어.
5. **세션 쿠키 `HttpOnly`** — XSS로 `document.cookie` 접근 시 세션 탈취 완화.
6. **위험 싱크 회피** — 클라이언트에서 `innerHTML`, `document.write`, `eval` 등에 사용자 입력을 직접 넣지 않는다(DOM 기반 XSS 예방).

참조: OWASP Top 10 A05:2025 – Injection(XSS는 2021부터 Injection 범주에 통합), OWASP XSS Prevention Cheat Sheet, OWASP DOM based XSS Prevention Cheat Sheet, Content Security Policy Cheat Sheet, KISA 『소프트웨어 개발보안 가이드』 크로스사이트 스크립트 항목.