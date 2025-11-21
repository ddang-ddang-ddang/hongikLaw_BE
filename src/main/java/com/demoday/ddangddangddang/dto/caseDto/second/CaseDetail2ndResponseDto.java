package com.demoday.ddangddangddang.dto.caseDto.second;

import com.demoday.ddangddangddang.domain.ArgumentInitial;
import com.demoday.ddangddangddang.domain.Case;
import com.demoday.ddangddangddang.domain.Defense;
import com.demoday.ddangddangddang.domain.Judgment;
import com.demoday.ddangddangddang.domain.Rebuttal;
import com.demoday.ddangddangddang.domain.Vote;
import com.demoday.ddangddangddang.domain.enums.DebateSide;
import com.demoday.ddangddangddang.dto.caseDto.JudgmentResponseDto;
import lombok.Builder;
import lombok.Getter;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
@Builder
public class CaseDetail2ndResponseDto {
    private Long caseId;
    private String caseTitle;
    private LocalDateTime deadline;
    private List<DefenseDto> defenses;
    private VoteDto userVote; // 내가 투표한 정보 (투표 안했으면 null)
    private JudgmentResponseDto currentJudgment; // [수정] 실시간 AI 판결 결과

    private ArgumentDetailDto argumentA;
    private ArgumentDetailDto argumentB;

    @Getter
    @Builder
    public static class DefenseDto {
        private Long defenseId;
        private String authorNickname;
        private DebateSide side;
        private String content;
        private Integer likesCount;
        private Boolean isLikedByMe;
        private List<RebuttalDto> rebuttals; // 이 변론에 달린 반론(대댓글) 목록
    }

    @Getter
    @Builder
    public static class RebuttalDto {
        private Long rebuttalId;
        private Long parentId; // 부모 반론 ID (대댓글용)
        private String authorNickname;
        private DebateSide type;
        private String content;
        private Integer likesCount;
        private Boolean isLikedByMe;
        private List<RebuttalDto> children; // 중첩 대댓글
    }

    @Getter
    @Builder
    public static class VoteDto {
        private DebateSide choice;
    }

    // 입장문 DTO
    @Getter
    @Builder
    public static class ArgumentDetailDto {
        private String mainArgument;
        private String reasoning;
        private Long authorId;

        public static ArgumentDetailDto fromEntity(ArgumentInitial arg) {
            return ArgumentDetailDto.builder()
                    .mainArgument(arg.getMainArgument())
                    .reasoning(arg.getReasoning())
                    .authorId(arg.getUser().getId())
                    .build();
        }
    }

    // 엔티티 리스트를 DTO 리스트로 변환하는 정적 팩토리 메서드
    public static CaseDetail2ndResponseDto fromEntities(
            Case aCase,
            List<Defense> defenseList,
            List<Rebuttal> rebuttalList,
            Vote userVote,
            Judgment finalJudgment, // [추가] AI 판결
            Set<Long> userLikedDefenseIds,
            Set<Long> userLikedRebuttalIds,
            ArgumentInitial argA,
            ArgumentInitial argB
            )
    {
        // 1. 모든 반론 DTO 생성
        Map<Long, RebuttalDto> rebuttalDtoMap = rebuttalList.stream()
                .map(rebuttal -> RebuttalDto.builder()
                        .rebuttalId(rebuttal.getId())
                        .parentId(rebuttal.getParent() != null ? rebuttal.getParent().getId() : null)
                        .authorNickname(rebuttal.getUser().getNickname())
                        .type(rebuttal.getType())
                        .content(rebuttal.getContent())
                        .likesCount(rebuttal.getLikesCount())
                        .isLikedByMe(userLikedRebuttalIds.contains(rebuttal.getId()))
                        .children(new java.util.ArrayList<>()) // 자식 리스트 초기화
                        .build())
                .collect(Collectors.toMap(RebuttalDto::getRebuttalId, dto -> dto));

        // 2. 반론 DTO 간의 부모-자식 관계 설정 (중첩 댓글)
        rebuttalDtoMap.values().stream()
                .filter(dto -> dto.getParentId() != null)
                .forEach(dto -> {
                    RebuttalDto parentDto = rebuttalDtoMap.get(dto.getParentId());
                    if (parentDto != null) {
                        parentDto.getChildren().add(dto);
                    }
                });

        // 3. 변론 ID별로 최상위 반론(부모가 없는)들만 그룹화
        Map<Long, List<RebuttalDto>> topLevelRebuttalsByDefenseId = rebuttalDtoMap.values().stream()
                .filter(dto -> dto.getParentId() == null) // 최상위 반론만 필터링
                .collect(Collectors.groupingBy(dto -> {
                    Rebuttal entity = rebuttalList.stream().filter(r -> r.getId().equals(dto.getRebuttalId())).findFirst().get();
                    return entity.getDefense().getId();
                }));

        // 4. 변론 DTO 생성
        List<DefenseDto> defenseDtos = defenseList.stream()
                .map(defense -> DefenseDto.builder()
                        .defenseId(defense.getId())
                        .authorNickname(defense.getUser().getNickname())
                        .side(defense.getType())
                        .content(defense.getContent())
                        .likesCount(defense.getLikesCount())
                        .isLikedByMe(userLikedDefenseIds.contains(defense.getId()))
                        .rebuttals(topLevelRebuttalsByDefenseId.getOrDefault(defense.getId(), List.of()))
                        .build())
                .collect(Collectors.toList());

        // 5. 최종 DTO 빌드
        return CaseDetail2ndResponseDto.builder()
                .caseId(aCase.getId())
                .caseTitle(aCase.getTitle())
                .deadline(aCase.getAppealDeadline())
                .defenses(defenseDtos)
                .userVote(userVote != null ? VoteDto.builder().choice(userVote.getType()).build() : null)
                .currentJudgment(finalJudgment != null ? new JudgmentResponseDto(finalJudgment) : null)
                .argumentA(argA != null ? ArgumentDetailDto.fromEntity(argA) : null)
                .argumentB(argB != null ? ArgumentDetailDto.fromEntity(argB) : null)
                .build();
    }
}