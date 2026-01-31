package com.tictac.game.web;

import com.tictac.game.dto.GlobalRankingEntry;
import com.tictac.game.dto.HistoryEntry;
import com.tictac.game.dto.MyStatsResponse;
import com.tictac.game.repo.StatsRepository;
import com.tictac.game.security.JwtUser;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/game")
public class StatsController {

    private final StatsRepository statsRepository;

    public StatsController(StatsRepository statsRepository) {
        this.statsRepository = statsRepository;
    }

    @GetMapping("/history/me")
    public List<HistoryEntry> myHistory(@AuthenticationPrincipal JwtUser user,
                                        @RequestParam(name = "limit", defaultValue = "20") int limit,
                                        @RequestParam(name = "offset", defaultValue = "0") int offset,
                                        @RequestParam(name = "from", required = false)
                                        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                        @RequestParam(name = "to", required = false)
                                        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                        @RequestParam(name = "mode", required = false) String mode) {
        List<String> modes = parseModes(mode);
        return statsRepository.findHistory(user.userId(), modes, from, to, limit, offset);
    }

    @GetMapping("/stats/me")
    public MyStatsResponse myStats(@AuthenticationPrincipal JwtUser user) {
        return statsRepository.findMyStats(user.userId());
    }

    @GetMapping("/stats/global")
    public List<GlobalRankingEntry> globalStats(@RequestParam(name = "minMatches", defaultValue = "5") int minMatches,
                                                @RequestParam(name = "limit", defaultValue = "50") int limit,
                                                @RequestParam(name = "offset", defaultValue = "0") int offset) {
        return statsRepository.findGlobalRanking(minMatches, limit, offset);
    }

    private static List<String> parseModes(String mode) {
        if (mode == null || mode.isBlank()) {
            return List.of();
        }
        return Arrays.stream(mode.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(String::toUpperCase)
                .collect(Collectors.toList());
    }
}
