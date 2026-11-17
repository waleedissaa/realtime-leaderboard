package com.waleedissaa.leaderboard.ranking;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/leaderboards/{slug}")
public class RankingController {

    private final RankingService service;

    public RankingController(RankingService service) {
        this.service = service;
    }

    @PostMapping("/scores")
    public ResponseEntity<SubmitScoreResponse> submit(@PathVariable String slug,
                                                      @Valid @RequestBody SubmitScoreRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.submit(slug, request));
    }

    @GetMapping("/players/{playerId}")
    public PlayerStanding standing(@PathVariable String slug, @PathVariable long playerId) {
        return service.standing(slug, playerId);
    }

    @GetMapping("/rankings")
    public ResponseEntity<RankingsPage> rankings(
            @PathVariable String slug,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "0") int offset,
            @RequestHeader(value = HttpHeaders.IF_NONE_MATCH, required = false) String ifNoneMatch) {
        String tag = "\"" + service.currentVersion(slug) + "-" + limit + "-" + offset + "\"";
        if (tag.equals(ifNoneMatch)) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED).eTag(tag).build();
        }
        return ResponseEntity.ok().eTag(tag).body(service.rankings(slug, limit, offset));
    }
}
