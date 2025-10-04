package com.tictac.game.web;

import com.tictac.game.dto.*;
import com.tictac.game.security.JwtUser;
import com.tictac.game.service.GameService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/game")
public class GameController {

    private final GameService service;
    public GameController(GameService service) { this.service = service; }

    @PostMapping("/self/start")
    @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
    public GameStateResponse startSelf(@AuthenticationPrincipal JwtUser user,
                                       @Valid @RequestBody StartSelfRequest req) {
        return service.startSelf(user, req);
    }

    @PostMapping("/vs-system/start")
    @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
    public GameStateResponse startVsSystem(@AuthenticationPrincipal JwtUser user,
                                           @Valid @RequestBody StartVsSystemRequest req) {
        return service.startVsSystem(user, req);
    }

    @PostMapping("/{id}/move")
    public GameStateResponse move(@AuthenticationPrincipal JwtUser user,
                                  @PathVariable("id") long id,
                                  @Valid @RequestBody MoveRequest req) {
        return service.move(user, id, req);
    }

    @GetMapping("/{id}")
    public GameStateResponse get(@AuthenticationPrincipal JwtUser user,
                                 @PathVariable("id") long id) {
        return service.get(id, user);
    }
}
