package com.demoday.ddangddangddang.dto.ai;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter // ObjectMapper가 JSON을 파싱할 때 필요
@NoArgsConstructor
public class AiJudgmentDto {
    private String verdict; // 판결 내용
    private String conclusion; // 결론
    private Integer ratioA;
    private Integer ratioB;

    // AI 호출 실패 시 사용할 비상용 생성자
    public AiJudgmentDto(String verdict, String conclusion, Integer ratioA, Integer ratioB) {
        this.verdict = verdict;
        this.conclusion = conclusion;
        this.ratioA = ratioA;
        this.ratioB = ratioB;
    }
}