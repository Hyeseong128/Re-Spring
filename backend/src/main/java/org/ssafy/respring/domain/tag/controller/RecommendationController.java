package org.ssafy.respring.domain.tag.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.ssafy.respring.domain.challenge.dto.response.ChallengeListResponseDto;
import org.ssafy.respring.domain.tag.service.TagRecommendationService;
import org.ssafy.respring.domain.user.service.UserService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/recommend")
@RequiredArgsConstructor
@Tag(name = "Recommendation API", description = "태그 유사도 기반 추천 알고리즘 관련 API")
public class RecommendationController {

    private final TagRecommendationService tagRecommendationService;
    private final UserService userService;

    @GetMapping("/challenges/{nickname}")
    @Operation(summary = "추천 챌린지 목록 조회", description = "태그 유사도 기반으로 챌린지를 추천합니다.")
    public ResponseEntity<List<ChallengeListResponseDto>> recommendChallenges(@PathVariable String nickname) {
        UUID userId = userService.findByNickname(nickname).getId();
        return ResponseEntity.ok(tagRecommendationService.recommendChallenges(userId));
    }

//    @GetMapping("/books/{userId}")
//    public ResponseEntity<List<Book>> recommendBooks(@PathVariable UUID userId) {
//        return ResponseEntity.ok(tagRecommendationService.recommendBooks(userId));
//    }
}
