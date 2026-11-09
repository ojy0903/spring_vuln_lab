package com.shieldus.rookies.secure.xss;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class SecXssPageController {

    private final SecXssPostService postService;

    @GetMapping("/secure/xss") // 목록 + 검색
    public String list(@RequestParam(defaultValue = "") String keyword, Model model) {
        // 패치: HTML을 문자열로 조립하지 않고 값만 전달 -> 마크업은 템플릿, 이스케이프는 th:text가 담당
        model.addAttribute("keyword", keyword);
        model.addAttribute("posts", postService.search(keyword));
        return "secure/xss";
    }

    @GetMapping("/secure/xss/posts/{id}") // 상세
    public String detail(@PathVariable long id, Model model) {
        model.addAttribute("post", postService.findById(id));
        return "secure/xss-post";
    }

    @PostMapping("/secure/xss/posts") // 글 작성
    public String create(@RequestParam String title, @RequestParam String content) {
        postService.create(title, content);
        return "redirect:/secure/xss";
    }
}
