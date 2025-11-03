package com.demoday.ddangddangddang.service;

import com.demoday.ddangddangddang.domain.ArgumentInitial;
import com.demoday.ddangddangddang.domain.Case;
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

@Service
@Slf4j
@RequiredArgsConstructor
public class ChatGptService2 {

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

            String aiResponseJson = String.valueOf(completion
                    .choices().get(0)
                    .message()
                    .content());   // ← String 기대

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

    private String buildPrompt(Case newCase, List<ArgumentInitial> arguments) {
        ArgumentInitial argA = arguments.stream().filter(a -> a.getType() == DebateSide.A).findFirst().orElseThrow();
        ArgumentInitial argB = arguments.stream().filter(a -> a.getType() == DebateSide.B).findFirst().orElseThrow();

        return String.format(
                "다음은 사용자가 입력한 솔로 모드 밸런스 게임입니다. 현명한 판사가 되어 판결을 내려주세요." +
                        "\n\n[주제]: %s" +
                        "\n\n[A측 입장]: %s" +
                        "\n[A측 근거]: %s" +
                        "\n\n[B측 입장]: %s" +
                        "\n[B측 근거]: %s" +
                        "\n\n---" +
                        "\n판결을 다음 JSON 형식에 맞춰서만 응답해주세요. 다른 설명은 절대 추가하지 마세요:" +
                        "\n{" +
                        "\n  \"verdict\": \"[판결 내용]\", " +
                        "\n  \"conclusion\": \"[최종 결론 한 문장, '...라고 판단된다.'로 끝내기]\", " +
                        "\n  \"ratioA\": [0-100 정수], " +
                        "\n  \"ratioB\": [0-100 정수]" +
                        "\n}" +
                        "\n[주의] ratioA와 ratioB의 합은 반드시 100이어야 합니다.",
                newCase.getTitle(),
                argA.getMainArgument(), argA.getReasoning(),
                argB.getMainArgument(), argB.getReasoning()
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
