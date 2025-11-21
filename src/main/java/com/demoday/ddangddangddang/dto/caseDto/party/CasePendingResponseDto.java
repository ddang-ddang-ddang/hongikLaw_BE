package com.demoday.ddangddangddang.dto.caseDto.party;

import com.demoday.ddangddangddang.domain.ArgumentInitial;
import com.demoday.ddangddangddang.domain.Case;
import com.demoday.ddangddangddang.domain.enums.DebateSide;
import lombok.Getter;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

@Getter
public class CasePendingResponseDto {
    private Long caseId;
    private String title;
    private String argumentAMain;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Seoul")
    private LocalDateTime createdAt; // 등록 시간

    public CasePendingResponseDto(Case aCase, ArgumentInitial argumentA) {
        this.caseId = aCase.getId();
        this.title = aCase.getTitle();
        // A측 입장문이 존재하고 타입이 A가 맞는지 확인
        if (argumentA != null && argumentA.getType() == DebateSide.A) {
            this.argumentAMain = argumentA.getMainArgument();
        } else {
            this.argumentAMain = "입장 확인 중..."; // 예외 처리
        }
        this.createdAt = aCase.getCreatedAt();
    }
}