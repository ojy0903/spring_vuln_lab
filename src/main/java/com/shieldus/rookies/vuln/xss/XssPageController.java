package com.shieldus.rookies.vuln.xss;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Profile("local")
@Controller
@RequiredArgsConstructor
public class XssPageController {

    public final XssPostService postService;

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

    @GetMapping("/vuln/xss/posts/{id}") // 상세 (Stored XSS 실행 가능)
    public String detail(@PathVariable long id, Model model) {
        model.addAttribute("post", postService.findById(id));
        return "vuln/xss-post";
    }

    @PostMapping("/vuln/xss/posts") // 글 작성 (Stored XSS 저장 가능)
    public String create(@RequestParam String title, @RequestParam String content) {
        postService.create(title, content);
        return "redirect:/vuln/xss";
    }
}
