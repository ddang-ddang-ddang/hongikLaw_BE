package com.demoday.ddangddangddang.dto.caseDto;

import com.demoday.ddangddangddang.domain.ArgumentInitial;
import com.demoday.ddangddangddang.domain.Case;
import com.demoday.ddangddangddang.domain.enums.CaseMode;
import com.demoday.ddangddangddang.domain.enums.CaseStatus;
import lombok.Builder;
import lombok.Getter;

@Getter
public class CaseDetailResponseDto {
    private Long caseId;
    private String title;
    private CaseStatus status;
    private CaseMode mode;
    private ArgumentDetailDto argumentA;
    private ArgumentDetailDto argumentB;

    @Getter
    private static class ArgumentDetailDto {
        private String mainArgument;
        private String reasoning;
        private Long authorId; // 1차 입장문 작성자 ID

        public ArgumentDetailDto(ArgumentInitial argument) {
            this.mainArgument = argument.getMainArgument();
            this.reasoning = argument.getReasoning();
            this.authorId = argument.getUser().getId();
        }
    }

    @Builder
    public CaseDetailResponseDto(Case aCase, ArgumentInitial argA, ArgumentInitial argB) {
        this.caseId = aCase.getId();
        this.title = aCase.getTitle();
        this.status = aCase.getStatus();
        this.mode = aCase.getMode();
        this.argumentA = new ArgumentDetailDto(argA);
        // [수정] argB가 null이면 field도 null로 설정 (VS 모드 대기 상태 대응)
        this.argumentB = (argB != null) ? new ArgumentDetailDto(argB) : null;
    }
}