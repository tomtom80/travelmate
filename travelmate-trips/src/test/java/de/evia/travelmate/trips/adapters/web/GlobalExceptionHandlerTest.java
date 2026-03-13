package de.evia.travelmate.trips.adapters.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import de.evia.travelmate.common.domain.BusinessRuleViolationException;
import de.evia.travelmate.common.domain.DuplicateEntityException;
import de.evia.travelmate.common.domain.EntityNotFoundException;

class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
            .standaloneSetup(new TestErrorController())
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
    }

    @Test
    void entityNotFoundExceptionReturns404() throws Exception {
        mockMvc.perform(get("/test-errors/not-found"))
            .andExpect(status().isNotFound())
            .andExpect(view().name("error/404"))
            .andExpect(model().attributeExists("message"));
    }

    @Test
    void duplicateEntityExceptionReturns409() throws Exception {
        mockMvc.perform(get("/test-errors/duplicate"))
            .andExpect(status().isConflict())
            .andExpect(view().name("error/error"))
            .andExpect(model().attributeExists("message"));
    }

    @Test
    void businessRuleViolationExceptionReturns422() throws Exception {
        mockMvc.perform(get("/test-errors/business-rule"))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(view().name("error/error"))
            .andExpect(model().attributeExists("message"));
    }

    @Test
    void runtimeExceptionReturns500WithGenericMessage() throws Exception {
        mockMvc.perform(get("/test-errors/runtime"))
            .andExpect(status().isInternalServerError())
            .andExpect(view().name("error/error"))
            .andExpect(model().attribute("message", "Ein unerwarteter Fehler ist aufgetreten."));
    }

    @Test
    void entityNotFoundWithHtmxTriggersToastAndPreservesView() throws Exception {
        mockMvc.perform(get("/test-errors/not-found")
                .header("HX-Request", "true"))
            .andExpect(status().isNotFound())
            .andExpect(view().name("fragments/empty :: empty"))
            .andExpect(header().string("HX-Reswap", "none"))
            .andExpect(header().exists("HX-Trigger"));
    }

    @Test
    void duplicateEntityWithHtmxTriggersToastAndPreservesView() throws Exception {
        mockMvc.perform(get("/test-errors/duplicate")
                .header("HX-Request", "true"))
            .andExpect(status().isConflict())
            .andExpect(view().name("fragments/empty :: empty"))
            .andExpect(header().string("HX-Reswap", "none"))
            .andExpect(header().exists("HX-Trigger"));
    }

    @Test
    void businessRuleViolationWithHtmxTriggersToastAndPreservesView() throws Exception {
        mockMvc.perform(get("/test-errors/business-rule")
                .header("HX-Request", "true"))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(view().name("fragments/empty :: empty"))
            .andExpect(header().string("HX-Reswap", "none"))
            .andExpect(header().exists("HX-Trigger"));
    }

    @Test
    void runtimeExceptionWithHtmxTriggersGenericToast() throws Exception {
        mockMvc.perform(get("/test-errors/runtime")
                .header("HX-Request", "true"))
            .andExpect(status().isInternalServerError())
            .andExpect(view().name("fragments/empty :: empty"))
            .andExpect(header().string("HX-Reswap", "none"))
            .andExpect(header().exists("HX-Trigger"));
    }

    @Controller
    @RequestMapping("/test-errors")
    static class TestErrorController {

        @GetMapping("/not-found")
        public String notFound() {
            throw new EntityNotFoundException("Trip", "abc-123");
        }

        @GetMapping("/duplicate")
        public String duplicate() {
            throw new DuplicateEntityException("invitation.error.alreadyExists");
        }

        @GetMapping("/business-rule")
        public String businessRule() {
            throw new BusinessRuleViolationException("trip.error.invalidTransition");
        }

        @GetMapping("/runtime")
        public String runtime() {
            throw new RuntimeException("NullPointerException at SomeService.java:42");
        }
    }
}
