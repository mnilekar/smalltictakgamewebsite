package com.tictac.game.web;

import com.tictac.game.service.MatchmakingService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Method;
import java.util.Map;

@RestController
@RequestMapping("/api/game/pvp/mm")
public class MatchmakingController {

    private final MatchmakingService mm;
    public MatchmakingController(MatchmakingService mm) { this.mm = mm; }

    @PostMapping("/join")
    public ResponseEntity<?> join(Authentication auth) {
        long uid = userId(auth);
        var r = mm.join(uid);
        if (r.matched) {
            var g = r.game;
            return ResponseEntity.ok(Map.of(
                    "matched", true,
                    "gameId", g.gameId(),
                    "mode", g.mode(),
                    "youAre", g.youAre(),
                    "board", g.board(),
                    "turn", g.turn(),
                    "status", g.status(),
                    "deadlineAt", g.deadlineAt()
            ));
        }
        return ResponseEntity.ok(Map.of(
                "matched", false,
                "ticketId", r.ticketId,
                "waitUntil", r.waitUntil,
                "timeoutSeconds", r.timeoutSeconds
        ));
    }

    @GetMapping("/status/{ticketId}")
    public ResponseEntity<?> status(@PathVariable("ticketId") String ticketId) {
        var opt = mm.status(ticketId);
        if (opt.isEmpty()) return ResponseEntity.ok(Map.of("matched", false, "expired", true));
        var r = opt.get();
        if (r.matched) {
            var g = r.game;
            return ResponseEntity.ok(Map.of(
                    "matched", true,
                    "gameId", g.gameId(),
                    "mode", g.mode(),
                    "youAre", g.youAre(),
                    "board", g.board(),
                    "turn", g.turn(),
                    "status", g.status(),
                    "deadlineAt", g.deadlineAt()
            ));
        }
        if (r.ticketId == null) return ResponseEntity.ok(Map.of("matched", false, "expired", true));
        return ResponseEntity.ok(Map.of(
                "matched", false,
                "ticketId", r.ticketId,
                "waitUntil", r.waitUntil,
                "timeoutSeconds", r.timeoutSeconds
        ));
    }

    @PostMapping("/cancel/{ticketId}")
    public ResponseEntity<?> cancel(@PathVariable("ticketId") String ticketId) {
        mm.cancel(ticketId);
        return ResponseEntity.ok(Map.of("ok", true));
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
