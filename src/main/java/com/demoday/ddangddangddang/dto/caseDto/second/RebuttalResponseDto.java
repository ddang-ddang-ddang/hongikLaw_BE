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
    private String authorProfileUrl;
    private String authorRank;
    private DebateSide type;
    private String content;
    private Integer likesCount;
    private Boolean isLikedByMe;
    private Boolean isBlind;
    private List<RebuttalResponseDto> children; // 중첩 대댓글

    // 엔티티 리스트 -> 중첩 DTO 리스트로 변환하는 헬퍼 메서드
    public static List<RebuttalResponseDto> buildTree(List<Rebuttal> rebuttals, Set<Long> userLikedRebuttalIds) {

        Map<Long, RebuttalResponseDto> dtoMap = rebuttals.stream()
                .map(rebuttal -> {
                    // 블라인드 마스킹 로직
                    String displayContent = rebuttal.getIsBlind() ? "블라인드 처리된 내용입니다." : rebuttal.getContent();

                    return RebuttalResponseDto.builder()
                            .rebuttalId(rebuttal.getId())
                            .parentId(rebuttal.getParent() != null ? rebuttal.getParent().getId() : null)
                            .authorNickname(rebuttal.getUser().getNickname())
                            .authorProfileUrl(rebuttal.getUser().getProfileImageUrl())
                            .authorRank(rebuttal.getUser().getRank().getDisplayName())
                            .type(rebuttal.getType())
                            .content(displayContent) // 마스킹된 내용 적용
                            .likesCount(rebuttal.getLikesCount())
                            .isLikedByMe(userLikedRebuttalIds.contains(rebuttal.getId()))
                            .isBlind(rebuttal.getIsBlind())
                            .children(new ArrayList<>())
                            .build();
                })
                .collect(Collectors.toMap(RebuttalResponseDto::getRebuttalId, dto -> dto));

        dtoMap.values().stream()
                .filter(dto -> dto.getParentId() != null)
                .forEach(dto -> {
                    RebuttalResponseDto parentDto = dtoMap.get(dto.getParentId());
                    if (parentDto != null) {
                        parentDto.getChildren().add(dto);
                    }
                });

        return dtoMap.values().stream()
                .filter(dto -> dto.getParentId() == null)
                .collect(Collectors.toList());
    }
}