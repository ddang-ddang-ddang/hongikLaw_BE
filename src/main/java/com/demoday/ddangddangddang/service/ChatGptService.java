package com.demoday.ddangddangddang.service;

import com.demoday.ddangddangddang.domain.ArgumentInitial;
import com.demoday.ddangddangddang.domain.Case;
import com.demoday.ddangddangddang.domain.Defense;
import com.demoday.ddangddangddang.domain.Rebuttal;
import com.demoday.ddangddangddang.domain.enums.DebateSide;
import com.demoday.ddangddangddang.dto.ai.AiJudgmentDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openai.client.OpenAIClient;

import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import com.openai.models.chat.completions.ChatCompletionMessageParam;
import com.openai.models.chat.completions.ChatCompletionSystemMessageParam;
import com.openai.models.chat.completions.ChatCompletionUserMessageParam;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ChatGptService {

    private final OpenAIClient openAIClient;
    private final ObjectMapper objectMapper;

    public AiJudgmentDto getAiJudgment(Case newCase, List<ArgumentInitial> arguments) {
        String prompt = buildPrompt(newCase, arguments);

        // system/user 메시지를 파라미터 객체로 생성 (문자열 직접 전달 X인 버전용)
        ChatCompletionMessageParam systemMsg = ChatCompletionMessageParam.ofSystem(
                ChatCompletionSystemMessageParam.builder()
                        .content("당신은 논리적이고 공정한 판사입니다. 출력은 반드시 '단 하나의 JSON 객체'만 포함해야 합니다. " +
                                "코드블록, 주석, 여분 텍스트 금지. 오직 유효한 JSON만.")
                        .build()
        );
        ChatCompletionMessageParam userMsg = ChatCompletionMessageParam.ofUser(
                ChatCompletionUserMessageParam.builder()
                        .content(prompt)
                        .build()
        );

        ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
                .model("gpt-4o")
                .messages(List.of(systemMsg, userMsg))
                // .responseFormat(...)   // 네 버전에 없음 → 제거
                .temperature(0.7)
                .build();

        try {
            ChatCompletion completion = openAIClient.chat().completions().create(params);

            String aiResponseJson = completion
                    .choices().get(0)
                    .message()
                    .content()  // ← String 기대
                    .orElse("{}");

            aiResponseJson = sanitizeJson(aiResponseJson);

            log.info("AI 응답(JSON): {}", aiResponseJson);

            AiJudgmentDto dto = objectMapper.readValue(aiResponseJson, AiJudgmentDto.class);
            normalizeRatios(dto);
            return dto;

        } catch (Exception e) {
            log.error("OpenAI API 호출 오류: {}", e.getMessage(), e);
            return createFallbackJudgment(e.getMessage());
        }
    }

    /**
     * 2차(FINAL) 판결 요청 (새 메서드)
     */
    public AiJudgmentDto requestFinalJudgment(Case aCase, List<Defense> adoptedDefenses, List<Rebuttal> adoptedRebuttals, long votesA, long votesB) {

        // ⭐️ 프롬프트 빌더 호출 방식 변경
        String prompt = buildFinalPrompt(aCase, adoptedDefenses, adoptedRebuttals, votesA, votesB);

        ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
                .model("gpt-4o")
                .messages(List.of(
                        ChatCompletionMessageParam.ofSystem(
                                ChatCompletionSystemMessageParam.builder()
                                        .content("당신은 논리적이고 공정한 최종 판결 판사입니다. 사용자가 전달한 [채택된 의견]과 [배심원 투표 결과]를 종합하여 최종 판결을 내려야 합니다. " +
                                                "출력은 반드시 '단 하나의 JSON 객체'만 포함해야 합니다. 코드블록, 주석, 여분 텍스트 금지. 오직 유효한 JSON만.")
                                        .build()
                        ),
                        ChatCompletionMessageParam.ofUser(
                                ChatCompletionUserMessageParam.builder()
                                        .content(prompt)
                                        .build()
                        )
                ))
                .temperature(0.7)
                .build();

        try {
            ChatCompletion completion = openAIClient.chat().completions().create(params);

            String aiResponseJson = completion
                    .choices().get(0)
                    .message()
                    .content()
                    .orElse("{}"); // ⭐️ .content()가 Optional<String>을 반환할 경우 orElse 추가 (버전따라 다름)
            // 만약 .content()가 String을 반환하면 .orElse()는 제거

            aiResponseJson = sanitizeJson(aiResponseJson);
            log.info("AI 최종 판결 응답 (JSON): {}", aiResponseJson);

            AiJudgmentDto dto = objectMapper.readValue(aiResponseJson, AiJudgmentDto.class);
            normalizeRatios(dto);
            return dto;

        } catch (Exception e) {
            log.error("OpenAI API 최종 판결 호출 중 오류 발생: {}", e.getMessage(), e);
            return createFallbackJudgment(e.getMessage());
        }
    }

    // 1차 프롬포트
    private String buildPrompt(Case newCase, List<ArgumentInitial> arguments) {
        ArgumentInitial argA = arguments.stream().filter(a -> a.getType() == DebateSide.A).findFirst().orElseThrow();
        ArgumentInitial argB = arguments.stream().filter(a -> a.getType() == DebateSide.B).findFirst().orElseThrow();

        return String.format(
                "다음은 사용자가 입력한 밸런스 게임입니다. 현명한 판사가 되어 판결을 내려주세요. 50:50 판결은 최대한 지양해주세요" +
                        "\n\n[주제]: %s" +
                        "\n\n[A측 입장]: %s" +
                        "\n[A측 근거]: %s" +
                        "\n\n[B측 입장]: %s" +
                        "\n[B측 근거]: %s" +
                        "\n\n---" +
                        "\n판결을 다음 JSON 형식에 맞춰서만 응답해주세요. 다른 설명은 절대 추가하지 마세요:" +
                        "\n{" +
                        "\n  \"verdict\": \"[판결 내용: '판사일러' 부분에 들어갈 내용. 양측의 주장을 50자 이상으로 요약하고, 각 주장의 논리적인 강점과 약점을 상세히 분석하는 글. 최소 100자 이상 작성. 이 문단의 마지막은 반드시 '이에 따라 본 재판부는 다음과 같이 판단합니다.'로 끝나야 함]\", " +
                        "\n  \"conclusion\": \"[최종 결론: '결론 땅땅땅' 부분에 들어갈 내용. '본 재판부는 A or B 측의 논거가 A or B 측의 논거에 비해 약 n%% 정도 더 타당하고 설득력이 높다고 판단합니다. A or B 입장은 불복할 시 재심을 청구할 수 있습니다.' 형식으로 작성]\", " +
                        "\n  \"ratioA\": [0-100 정수], " +
                        "\n  \"ratioB\": [0-100 정수]" +
                        "\n}" +
                        "\n[주의] ratioA와 ratioB의 합은 반드시 100이어야 합니다.",
                newCase.getTitle(),
                argA.getMainArgument(), argA.getReasoning(),
                argB.getMainArgument(), argB.getReasoning()
        );
    }

    /**
     * 2차(FINAL) 프롬프트 생성 (새 메서드)
     */
    private String buildFinalPrompt(Case aCase, List<Defense> adoptedDefenses, List<Rebuttal> adoptedRebuttals, long votesA, long votesB) {

        // ⭐️ '채택된 변론' 목록 문자열 생성
        String adoptedDefenseString = adoptedDefenses.stream()
                .map(d -> String.format(
                        "- [%s측 채택 변론]: %s",
                        d.getType().name(),
                        d.getContent()
                ))
                .collect(Collectors.joining("\n"));

        // ⭐️ '채택된 반론' 목록 문자열 생성
        String adoptedRebuttalString = adoptedRebuttals.stream()
                .map(r -> String.format(
                        "- [%s측 채택 반론]: %s",
                        r.getType().name(),
                        r.getContent()
                ))
                .collect(Collectors.joining("\n"));

        String finalArguments = adoptedDefenseString + "\n" + adoptedRebuttalString;
        if (adoptedDefenses.isEmpty() && adoptedRebuttals.isEmpty()) {
            finalArguments = "채택된 의견이 없습니다.";
        }

        return String.format(
                "다음은 2차 재판(최종심) 판결을 위한 정보입니다. 주제, [채택된 최종 의견], [배심원 투표 결과]를 종합하여 최종 판결을 내려주세요." +
                        "\n\n[주제]: %s" +
                        "\n\n[배심원 투표 결과]: A측 (%d표) vs B측 (%d표)" +
                        "\n\n[채택된 최종 의견 목록]:\n%s" + // ⭐️ '추천수 상위' -> '채택된 최종'
                        "\n\n---" +
                        "\n[지시사항] 위 정보를 바탕으로 최종 판결을 내리고, 반드시 JSON 객체({ ... })로만 응답해주세요. 50:50 판결은 최대한 지양해주세요" +
                        "\n{" +
                        "\n  \"verdict\": \"[최종 판결 내용: 배심원 투표 결과와 사용자가 채택한 의견들을 모두 고려한 150자 이상의 상세하고 종합적인 분석 글. 이 문단의 마지막은 반드시 '이에 따라 본 재판부는 다음과 같이 판단합니다.'로 끝나야 함]\", " +
                        "\n  \"conclusion\": \"[최종 결론: '...라고 최종 판결한다.'로 끝나는 한 문장 결론]\", " +
                        "\n  \"ratioA\": [최종 A측 지지 비율(정수, 0-100)], " +
                        "\n  \"ratioB\": [최종 B측 지지 비율(정수, 0-100)]" +
                        "\n}" +
                        "\n[주의] ratioA와 ratioB의 합은 반드시 100이 되어야 합니다.",
                aCase.getTitle(),
                votesA, votesB,
                finalArguments // ⭐️ 수정된 변수 사용
        );
    }

    private void normalizeRatios(AiJudgmentDto dto) {
        int a = dto.getRatioA(), b = dto.getRatioB(), sum = a + b;
        if (sum == 100) return;
        if (sum <= 0) { dto.setRatioA(50); dto.setRatioB(50); return; }
        int newA = Math.round(a * 100f / sum);
        int newB = 100 - newA;
        dto.setRatioA(newA);
        dto.setRatioB(newB);
    }

    private AiJudgmentDto createFallbackJudgment(String errorMessage) {
        return new AiJudgmentDto(
                "AI 판결 중 오류: " + errorMessage,
                "오류로 인해 판결을 내릴 수 없습니다.",
                50, 50
        );
    }

    private String sanitizeJson(String content) {
        if (content == null) return "{}";
        String s = content.trim();
        if (s.startsWith("```")) {
            s = s.replaceAll("(?s)^```json\\s*|^```\\s*|\\s*```$", "");
        }
        return s.trim();
    }
}
