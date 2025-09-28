package com.tictac.auth.web;

import com.tictac.auth.dto.RegistrationResponse;
import com.tictac.auth.service.RegistrationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = AuthController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                SecurityFilterAutoConfiguration.class
        }
)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerValidationTest {

    @Autowired MockMvc mvc;

    @MockBean RegistrationService registrationService;

    @Test
    void validation_fails_on_missing_fields() throws Exception {
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }


    void validation_passes_on_valid_payload() throws Exception {
        when(registrationService.register(any()))
                .thenReturn(new RegistrationResponse(1L, "mnilekar"));

        String body = "{"
                + "\"firstName\":\"M\",\"lastName\":\"N\",\"birthDate\":\"1993-07-10\",\"nationality\":\"IN\","
                + "\"email\":\"mn@example.com\",\"mobile\":\"9999999999\",\"username\":\"mnilekar\",\"password\":\"P@ssword1\""
                + "}";

        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());
    }
}
