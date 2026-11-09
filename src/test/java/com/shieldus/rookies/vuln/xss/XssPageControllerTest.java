package com.shieldus.rookies.vuln.xss;

import com.shieldus.rookies.common.dtos.PostResponse;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.BDDMockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.util.List;

@WebMvcTest(XssPageController.class)
@ActiveProfiles("local")
class XssPageControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean XssPostService postService;

    @Nested
    class Reflected_XSS { // GET /vuln/xss?keyword=...

        @Test
        void keyword가_이스케이프_없이_그대로_반영된다() throws Exception {
            BDDMockito.given(postService.search(ArgumentMatchers.any())).willReturn(List.of());
            String payload = "<script>alert(1)</script>";

            mockMvc.perform(MockMvcRequestBuilders.get("/vuln/xss").param("keyword", payload))
                    .andExpect(MockMvcResultMatchers.status().isOk())
                    // th:utext → 페이로드가 <b>래퍼까지 원문 그대로 응답에 박힘 (= 싱크 취약)
                    .andExpect(MockMvcResultMatchers.content()
                            .string(Matchers.containsString("<b>" + payload + "</b>")))
                    // message가 이스케이프됐다면 <b>도 &lt;b&gt;로 나왔을 것.
                    .andExpect(MockMvcResultMatchers.content()
                            .string(Matchers.not(Matchers.containsString("&lt;b&gt;"))));
        }
    }

    @Nested
    class Stored_XSS { // GET /vuln/xss/posts/{id}

        @Test
        void 저장된_본문이_이스케이프_없이_출력된다() throws Exception {
            String payload = "<img src=x onerror=alert(1)>";
            BDDMockito.given(postService.findById(1L))
                    .willReturn(new PostResponse(1L, "제목", payload, false));

            mockMvc.perform(MockMvcRequestBuilders.get("/vuln/xss/posts/{id}", 1))
                    .andExpect(MockMvcResultMatchers.status().isOk())
                    .andExpect(MockMvcResultMatchers.content().string(Matchers.containsString(payload)));
        }
    }
}