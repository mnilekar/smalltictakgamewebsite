package com.tictac.auth.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tictac.auth.dto.RegistrationRequest;
import com.tictac.auth.service.RegistrationService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
class AuthControllerValidationTest {

    @Autowired MockMvc mvc;
    @MockBean RegistrationService service;
    @Autowired ObjectMapper om;

    @Test
    void validation_fails_on_missing_fields() throws Exception {
        RegistrationRequest r = new RegistrationRequest(); // empty -> invalid
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(r)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void validation_passes_on_valid_payload() throws Exception {
        RegistrationRequest r = new RegistrationRequest();
        r.setFirstName("Aishu");
        r.setLastName("Sharma");
        r.setBirthDate(LocalDate.of(1995,5,10));
        r.setNationality("IN");
        r.setEmail("aishu@example.com");
        r.setMobile("9999999999");
        r.setUsername("aishu95");
        r.setPassword("P@ssword1");

        Mockito.when(service.register(Mockito.any())).thenReturn(null); // we only test validation layer

        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(r)))
                .andExpect(status().is2xxSuccessful());
    }
}