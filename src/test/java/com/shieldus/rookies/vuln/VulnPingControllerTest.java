package com.shieldus.rookies.vuln;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

@SpringBootTest
@AutoConfigureMockMvc
class VulnPingControllerTest {

    @Nested
    class 기본_프로필 {

        @Autowired
        MockMvc mockMvc;

        @Test
        void vuln_엔드포인트_등록_안됨() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.get("/vuln/ping"))
                    .andExpect(MockMvcResultMatchers.status().isNotFound());
        }
    }

    @Nested
    @ActiveProfiles("local")
    class local_프로필 {

        @Autowired
        MockMvc mockMvc;

        @Test
        void ping_OK_응답() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.get("/vuln/ping"))
                    .andExpect(MockMvcResultMatchers.status().isOk())
                    .andExpect(MockMvcResultMatchers.content().string("OK"));
        }
    }

}