package com.shieldus.rookies.secure.sqli;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class SecPageSearchController {

    private final SecurePostSearchService postSearchService;

    @GetMapping("/secure/sqli")
    public String page() {
        return "secure/sqli";
    }

    // JDBC 바인딩 검색
    @GetMapping("/secure/sqli/search/jdbc")
    public String searchByJdbc(@RequestParam String keyword, Model model) {
        model.addAttribute("jdbcKeyword", keyword);
        model.addAttribute("jdbcResults", postSearchService.searchByJdbc(keyword)); // List<Map>
        return "secure/sqli";
    }

    // JPA 검색
    @GetMapping("/secure/sqli/search/jpa")
    public String searchByJpa(@RequestParam String keyword, Model model) {
        model.addAttribute("jpaKeyword", keyword);
        model.addAttribute("jpaResults", postSearchService.searchByJpa(keyword)); // List<Post>
        return "secure/sqli";
    }
}
