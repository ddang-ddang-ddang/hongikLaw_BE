package com.demoday.ddangddangddang.dto.caseDto;

import com.demoday.ddangddangddang.domain.Judgment;
import lombok.Getter;

@Getter
public class JudgmentResponseDto {
    private final String judgeIllustrationUrl; // 판사 일러스트
    private final String verdict; // 판결 내용
    private final String conclusion; // 결론 땅땅땅
    private final Integer ratioA; // A측 비율
    private final Integer ratioB; // B측 비율

    // [수정] 생성자가 Judgment 엔티티를 받도록 변경
    public JudgmentResponseDto(Judgment judgment) {
        this.judgeIllustrationUrl = "https://example.com/images/judge_neutral.png"; // (임시 URL)
        this.verdict = judgment.getContent();
        this.conclusion = judgment.getBasedOn(); // '결론'을 basedOn 필드에 저장했음
        this.ratioA = judgment.getRatioA(); // DB에서 가져옴
        this.ratioB = judgment.getRatioB(); // DB에서 가져옴
    }
}