package com.tictac.auth.service;

import com.tictac.auth.dto.RegistrationRequest;
import com.tictac.auth.dto.RegistrationResponse;
import com.tictac.auth.repo.UserRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RegistrationServiceTest {

    private final UserRepository repo = mock(UserRepository.class);
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);
    private final RegistrationService service = new RegistrationService(repo, encoder);

    private RegistrationRequest valid() {
        RegistrationRequest r = new RegistrationRequest();
        r.setFirstName("Aishu");
        r.setLastName("Sharma");
        r.setBirthDate(LocalDate.of(1995, 5, 10));
        r.setNationality("IN");
        r.setEmail("aishu@example.com");
        r.setMobile("9999999999");
        r.setUsername("aishu95");
        r.setPassword("P@ssword1"); // meets policy
        return r;
    }

    @Test
    void register_success() {
        RegistrationRequest r = valid();

        when(repo.existsByUsername("aishu95")).thenReturn(false);
        when(repo.existsByEmail("aishu@example.com")).thenReturn(false);
        when(encoder.encode("P@ssword1")).thenReturn("$2a$10$abcdef...");
        when(repo.insertUser(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(42L);

        RegistrationResponse res = service.register(r);

        assertEquals(42L, res.getUserId());
        assertEquals("aishu95", res.getUsername());
        verify(repo, times(1)).insertCredentials(eq(42L), anyString(), anyString());
    }

    @Test
    void register_conflict_username() {
        RegistrationRequest r = valid();
        when(repo.existsByUsername("aishu95")).thenReturn(true);
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.register(r));
        assertEquals(409, ex.getStatusCode().value());
    }

    @Test
    void register_conflict_email() {
        RegistrationRequest r = valid();
        when(repo.existsByUsername("aishu95")).thenReturn(false);
        when(repo.existsByEmail("aishu@example.com")).thenReturn(true);
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.register(r));
        assertEquals(409, ex.getStatusCode().value());
    }

    @Test
    void register_bad_password_policy() {
        RegistrationRequest r = valid();
        r.setPassword("weakpass"); // no upper & special
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.register(r));
        assertEquals(400, ex.getStatusCode().value());
    }
}