package com.tictac.auth.service;


import com.tictac.auth.dto.LoginRequest;
import com.tictac.auth.dto.LoginResponse;
import com.tictac.auth.repo.UserRepository;
import com.tictac.auth.security.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;


@Service
public class LoginService {
    private final UserRepository repo;
    private final PasswordEncoder encoder;
    private final JwtUtil jwt;


    public LoginService(UserRepository repo, PasswordEncoder encoder, JwtUtil jwt) {
        this.repo = repo; this.encoder = encoder; this.jwt = jwt;
    }


    public LoginResponse login(LoginRequest req) {
        var opt = repo.findByUsername(req.getUsername());
        if (opt.isEmpty()) throw new ResponseStatusException(UNAUTHORIZED, "Invalid credentials");
        var u = opt.get();
        if (!encoder.matches(req.getPassword(), u.passwordHash()))
            throw new ResponseStatusException(UNAUTHORIZED, "Invalid credentials");
        // ...
        String token = jwt.generate(u.userId(), u.username());
        // ...
        return new LoginResponse(token, u.userId(), u.username());
    }
}