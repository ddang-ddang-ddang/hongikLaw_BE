package com.demoday.ddangddangddang.service;

import com.demoday.ddangddangddang.domain.ArgumentInitial;
import com.demoday.ddangddangddang.domain.Case;
import com.demoday.ddangddangddang.domain.enums.DebateSide;
import com.demoday.ddangddangddang.dto.ai.AiJudgmentDto;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.ChatMessageRole;
import com.theokanning.openai.completion.chat.ChatCompletionChoice;
import com.theokanning.openai.service.OpenAiService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ChatGptService {

    private final OpenAiService openAiService;
    private final ObjectMapper objectMapper; // OpenAiConfig에서 주입

    /**
     * AI에게 판결 요청
     */
    public AiJudgmentDto getAiJudgment(Case newCase, List<ArgumentInitial> arguments) {

        // 1. 프롬프트 생성
        String prompt = buildPrompt(newCase, arguments);

        // 2. ChatGPT 요청 형식 생성
        ChatCompletionRequest request = ChatCompletionRequest.builder()
                .model("gpt-4o") // (최신 모델 사용)
                .messages(List.of(
                        new ChatMessage(ChatMessageRole.SYSTEM.value(), "당신은 논리적이고 공정한 판사입니다. '땅땅땅' 서비스의 1차 판결을 담당합니다."),
                        new ChatMessage(ChatMessageRole.USER.value(), prompt)
                ))
                //.responseFormat(new ChatCompletionRequest.ResponseFormat("json_object")) 0.18.2 버전에서 지원하지 않는 기능 나중에 버전 올려봅세
                .temperature(0.7)
                .build();

        // 3. API 호출 및 응답 파싱
        try {
            // API 호출
            ChatCompletionChoice choice = openAiService.createChatCompletion(request).getChoices().get(0);
            String aiResponseJson = choice.getMessage().getContent();

            log.info("AI 응답 (JSON): {}", aiResponseJson);

            // JSON을 DTO로 파싱
            AiJudgmentDto judgmentDto = objectMapper.readValue(aiResponseJson, AiJudgmentDto.class);

            // (간단한 비율 검증)
            if (judgmentDto.getRatioA() + judgmentDto.getRatioB() != 100) {
                log.warn("AI가 반환한 비율의 합이 100이 아닙니다. 정규화 필요.");
                // TODO: 정규화 로직 (예: 60, 30 -> 67, 33)
            }
            return judgmentDto;

        } catch (Exception e) {
            log.error("OpenAI API 호출 중 오류 발생: {}", e.getMessage());
            // 오류 발생 시 비상용 판결문 반환
            return createFallbackJudgment(e.getMessage());
        }
    }

    /**
     * AI에게 전달할 프롬프트를 생성합니다.
     */
    private String buildPrompt(Case newCase, List<ArgumentInitial> arguments) {
        ArgumentInitial argA = arguments.stream()
                .filter(a -> a.getType() == DebateSide.A).findFirst().orElseThrow();
        ArgumentInitial argB = arguments.stream()
                .filter(a -> a.getType() == DebateSide.B).findFirst().orElseThrow();

        return String.format(
                // [수정] JSON 모드를 못 쓰므로, 프롬프트에서 JSON만 반환하도록 더 강력하게 지시
                "다음은 사용자가 입력한 솔로 모드 밸런스 게임입니다. 현명한 판사가 되어 판결을 내려주세요." +
                        "\n\n[주제]: %s" +
                        "\n\n[A측 입장]: %s" +
                        "\n[A측 근거]: %s" +
                        "\n\n[B측 입장]: %s" +
                        "\n[B측 근거]: %s" +
                        "\n\n---" +
                        "\n[지시사항] 판결을 아래 JSON 형식에 맞춰서 *오직 JSON 코드만* 응답해야 합니다. " +
                        "JSON 앞뒤로 ```json 이나 다른 설명, 줄바꿈을 절대 추가하지 마세요. " +
                        "반드시 JSON 객체({ ... })로만 시작하고 끝나야 합니다." +
                        "\n{" +
                        "\n  \"verdict\": \"[판결 내용: '판사일러' 부분에 들어갈 내용. 양측의 주장을 요약하고 객관적으로 분석하는 글]\", " +
                        "\n  \"conclusion\": \"[최종 결론: '결론 땅땅땅' 부분에 들어갈 내용. '...라고 판단된다.'로 끝나는 한 문장 결론]\", " +
                        "\n  \"ratioA\": [A측 지지 비율(정수, 0-100)], " +
                        "\n  \"ratioB\": [B측 지지 비율(정수, 0-100)]" +
                        "\n}" +
                        "\n[주의] ratioA와 ratioB의 합은 반드시 100이 되어야 합니다.",
                newCase.getTitle(),
                argA.getMainArgument(), argA.getReasoning(),
                argB.getMainArgument(), argB.getReasoning()
        );
    }

    /**
     * AI 호출 실패 시 사용할 비상용(Fallback) 판결문
     */
    private AiJudgmentDto createFallbackJudgment(String errorMessage) {
        return new AiJudgmentDto(
                "AI 판결 중 오류가 발생했습니다: " + errorMessage,
                "오류로 인해 판결을 내릴 수 없습니다.",
                50,
                50
        );
    }
}