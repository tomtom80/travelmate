package de.evia.travelmate.iam.adapters.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PrometheusEndpointTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void prometheusEndpointReturnsJvmMetrics() throws Exception {
        mockMvc.perform(get("/actuator/prometheus"))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("jvm_memory")));
    }

    @Test
    void prometheusEndpointReturnsHikariMetrics() throws Exception {
        mockMvc.perform(get("/actuator/prometheus"))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("hikaricp")));
    }

    @Test
    void prometheusEndpointReturnsHttpMetrics() throws Exception {
        mockMvc.perform(get("/actuator/info"));

        mockMvc.perform(get("/actuator/prometheus"))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("http_server_requests")));
    }
}
