package com.demoday.ddangddangddang.dto.caseDto.second;

import com.demoday.ddangddangddang.domain.Rebuttal;
import com.demoday.ddangddangddang.domain.enums.DebateSide;
import lombok.Builder;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
@Builder
public class RebuttalResponseDto {
    private Long rebuttalId;
    private Long parentId; // 부모 반론 ID (대댓글용)
    private String authorNickname;
    private String authorRank;
    private DebateSide type;
    private String content;
    private Integer likesCount;
    private Boolean isLikedByMe;
    private List<RebuttalResponseDto> children; // 중첩 대댓글

    // 엔티티 리스트 -> 중첩 DTO 리스트로 변환하는 헬퍼 메서드
    public static List<RebuttalResponseDto> buildTree(List<Rebuttal> rebuttals, Set<Long> userLikedRebuttalIds) {

        // 1. 모든 반론을 DTO 맵으로 변환
        Map<Long, RebuttalResponseDto> dtoMap = rebuttals.stream()
                .map(rebuttal -> RebuttalResponseDto.builder()
                        .rebuttalId(rebuttal.getId())
                        .parentId(rebuttal.getParent() != null ? rebuttal.getParent().getId() : null)
                        .authorNickname(rebuttal.getUser().getNickname())
                        .authorRank(rebuttal.getUser().getRank().getDisplayName())
                        .type(rebuttal.getType())
                        .content(rebuttal.getContent())
                        .likesCount(rebuttal.getLikesCount())
                        .isLikedByMe(userLikedRebuttalIds.contains(rebuttal.getId()))
                        .children(new ArrayList<>()) // 자식 리스트 초기화
                        .build())
                .collect(Collectors.toMap(RebuttalResponseDto::getRebuttalId, dto -> dto));

        // 2. 부모-자식 관계 설정
        dtoMap.values().stream()
                .filter(dto -> dto.getParentId() != null)
                .forEach(dto -> {
                    RebuttalResponseDto parentDto = dtoMap.get(dto.getParentId());
                    if (parentDto != null) {
                        parentDto.getChildren().add(dto);
                    }
                });

        // 3. 최상위 댓글(부모가 없는)만 필터링하여 반환
        return dtoMap.values().stream()
                .filter(dto -> dto.getParentId() == null)
                .collect(Collectors.toList());
    }
}