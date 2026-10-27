package com.shieldus.rookies.vuln.sqli;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Profile("local")
@Controller
@RequiredArgsConstructor
public class SqliPageController {

    private final VulnPostSearchService postSearchService;

    @GetMapping("/vuln/sqli")
    public String page() {
        return "vuln/sqli";
    }

    @GetMapping("/vuln/sqli/search") // 취약 검색 실행 + 결과
    public String search(@RequestParam String keyword, Model model) {
        model.addAttribute("keyword", keyword);
        model.addAttribute("results", postSearchService.searchPost(keyword)); // 앞서 설계한 취약 서비스
        return "vuln/sqli";
    }
}
