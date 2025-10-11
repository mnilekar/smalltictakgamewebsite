package com.tictac.game.web;

import com.tictac.game.service.PvpService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Method;
import java.util.Map;

@RestController
@RequestMapping("/api/game/pvp")
public class PvpController {

    private final PvpService service;

    public PvpController(PvpService service) {
        this.service = service;
    }

    @PostMapping("/start")
    public ResponseEntity<?> start(Authentication auth) {
        long uid = userId(auth);
        var dto = service.start(uid);
        return ResponseEntity.ok(Map.of(
                "gameId", dto.gameId(),
                "mode", dto.mode(),
                "youAre", dto.youAre(),
                "board", dto.board(),
                "turn", dto.turn(),
                "status", dto.status(),
                "deadlineAt", dto.deadlineAt()
        ));
    }

    @PostMapping("/{id}/join")
    public ResponseEntity<?> join(@PathVariable("id") long id, Authentication auth) {
        long uid = userId(auth);
        var dto = service.join(id, uid);
        return ResponseEntity.ok(Map.of(
                "gameId", dto.gameId(),
                "mode", dto.mode(),
                "youAre", dto.youAre(),
                "board", dto.board(),
                "turn", dto.turn(),
                "status", dto.status(),
                "deadlineAt", dto.deadlineAt()
        ));
    }

    public record MoveRequest(int row, int col) {}

    @PostMapping("/{id}/move")
    public ResponseEntity<?> move(@PathVariable("id") long id,
                                  @RequestBody MoveRequest req,
                                  Authentication auth) {
        long uid = userId(auth);
        var dto = service.move(id, uid, req.row(), req.col());
        return ResponseEntity.ok(Map.of(
                "gameId", dto.gameId(),
                "mode", dto.mode(),
                "board", dto.board(),
                "turn", dto.turn(),
                "status", dto.status(),
                "deadlineAt", dto.deadlineAt()
        ));
    }

    private long userId(Authentication auth) {
        if (auth == null || auth.getPrincipal() == null) throw new RuntimeException("No principal");
        Object p = auth.getPrincipal();
        try {
            Method m = p.getClass().getMethod("userId");
            Object v = m.invoke(p);
            if (v instanceof Number n) return n.longValue();
            return Long.parseLong(String.valueOf(v));
        } catch (Exception ignore) {
            String name = auth.getName();
            try { return Long.parseLong(name); } catch (Exception e) {
                throw new RuntimeException("Unable to resolve user id from principal");
            }
        }
    }
}
