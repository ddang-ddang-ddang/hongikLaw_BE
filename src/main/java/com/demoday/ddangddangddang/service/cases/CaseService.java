package com.demoday.ddangddangddang.service.cases;

// ... (기존 imports) ...
import com.demoday.ddangddangddang.domain.*;
import com.demoday.ddangddangddang.domain.enums.*;
import com.demoday.ddangddangddang.domain.event.CaseCreatedEvent;
import com.demoday.ddangddangddang.domain.event.CaseParticipationEvent;
import com.demoday.ddangddangddang.dto.ai.AiJudgmentDto;
import com.demoday.ddangddangddang.dto.caseDto.*;
import com.demoday.ddangddangddang.dto.caseDto.party.CasePendingResponseDto;
import com.demoday.ddangddangddang.dto.home.CaseOnResponseDto;
import com.demoday.ddangddangddang.dto.notice.NotificationResponseDto;
import com.demoday.ddangddangddang.global.code.GeneralErrorCode;
import com.demoday.ddangddangddang.global.exception.GeneralException;
import com.demoday.ddangddangddang.global.sse.SseEmitters;
import com.demoday.ddangddangddang.repository.ArgumentInitialRepository;
import com.demoday.ddangddangddang.repository.CaseRepository;
import com.demoday.ddangddangddang.repository.JudgmentRepository;
import com.demoday.ddangddangddang.repository.CaseParticipationRepository; // [추가]
import com.demoday.ddangddangddang.service.ChatGptService;
import com.demoday.ddangddangddang.service.ExpService;
import com.demoday.ddangddangddang.service.ranking.RankingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CaseService {

    private final CaseRepository caseRepository;
    private final ArgumentInitialRepository argumentInitialRepository;
    private final JudgmentRepository judgmentRepository;
    private final ChatGptService chatGptService;
    private final CaseParticipationRepository caseParticipationRepository;
    private final RankingService rankingService;
    private final SseEmitters sseEmitters;
    private final ApplicationEventPublisher eventPublisher;
    private final ExpService expService;

    @Transactional
    public CaseResponseDto createCase(CaseRequestDto requestDto, User user) {
        if (requestDto.getMode().equals(CaseMode.SOLO)) {
            // 솔로 모드: 기존 로직 수행 (즉시 1차 판결)
            return createSoloCase(requestDto, user);
        } else {
            // VS(파티) 모드: 방 생성 (상대방 대기)
            return createPartyCase(requestDto, user);
        }
    }

    private CaseResponseDto createSoloCase(CaseRequestDto requestDto, User user) {
        if (requestDto.getArgumentAMain() == null || requestDto.getArgumentBMain() == null) {
            throw new GeneralException(GeneralErrorCode.INVALID_PARAMETER, "솔로 모드는 A/B 입장문이 모두 필요합니다.");
        }

        Case newCase = Case.builder()
                .mode(CaseMode.SOLO)
                .title(requestDto.getTitle())
                .status(CaseStatus.FIRST)
                .build();

        // [추가] 광고 정보가 요청에 포함되어 있다면 설정
        if (requestDto.getAdLink() != null && requestDto.getAdImageUrl() != null) {
            newCase.markAsAd(requestDto.getAdLink(), requestDto.getAdImageUrl());
        }

        caseRepository.save(newCase);

        //A입장문 저장
        ArgumentInitial argumentA = ArgumentInitial.builder()
                .aCase(newCase)
                .user(user)
                .type(DebateSide.A)
                .mainArgument(requestDto.getArgumentAMain())
                .reasoning(requestDto.getArgumentAReasoning()).build();
        argumentInitialRepository.save(argumentA);

        //B 입장문 저장
        ArgumentInitial argumentB = ArgumentInitial.builder()
                .aCase(newCase)
                .user(user)
                .type(DebateSide.B)
                .mainArgument(requestDto.getArgumentBMain())
                .reasoning(requestDto.getArgumentBReasoning())
                .build();
        argumentInitialRepository.save(argumentB);

        caseParticipationRepository.save(CaseParticipation.builder()
                .aCase(newCase)
                .user(user)
                .result(CaseResult.ONGOING)
                .build()); // [수정] Result 추가

        List<ArgumentInitial> arguments = List.of(argumentA, argumentB);
        AiJudgmentDto aiResult = chatGptService.getAiJudgment(newCase, arguments);

        Judgment judgment = Judgment.builder()
                .aCase(newCase)
                .stage(JudgmentStage.INITIAL)
                .content(aiResult.getVerdict())
                .basedOn(aiResult.getConclusion())
                .ratioA(aiResult.getRatioA())
                .ratioB(aiResult.getRatioB()).build();
        judgmentRepository.save(judgment);

        //이벤트 발생
        eventPublisher.publishEvent(new CaseCreatedEvent(user));

        return new CaseResponseDto(newCase.getId());
    }

    private CaseResponseDto createPartyCase(CaseRequestDto requestDto, User user) {
        // VS 모드 생성 시, A측 입장문도 함께 생성합니다.
        if (requestDto.getArgumentAMain() == null) {
            throw new GeneralException(GeneralErrorCode.INVALID_PARAMETER, "VS 모드는 A측 입장문이 필요합니다.");
        }

        Case newCase = Case.builder()
                .mode(CaseMode.PARTY)
                .title(requestDto.getTitle())
                .status(CaseStatus.PENDING)
                .build();
        caseRepository.save(newCase);

        // A측 입장문 저장
        ArgumentInitial argumentA = ArgumentInitial.builder()
                .aCase(newCase)
                .user(user)
                .type(DebateSide.A)
                .mainArgument(requestDto.getArgumentAMain())
                .reasoning(requestDto.getArgumentAReasoning())
                .build();
        argumentInitialRepository.save(argumentA);

        // A측 참여 기록 저장
        caseParticipationRepository.save(CaseParticipation.builder()
                .aCase(newCase)
                .user(user)
                .result(CaseResult.ONGOING).build());

        // VS 모드 사건 생성 시 +100 exp
        expService.addExp(user, 100L, "VS 모드 사건 생성");

        //이벤트 리스너 호출(사건 생성)
        eventPublisher.publishEvent(new CaseCreatedEvent(user));

        return new CaseResponseDto(newCase.getId());
    }


    // VS모드 1차 입장문 제출 API 로직 (자동 할당)
    @Transactional
    public void createInitialArgument(Long caseId, ArgumentInitialRequestDto requestDto, User user) {
        Case aCase = findCaseById(caseId);

        // 상태 및 모드 검증
        if (aCase.getMode() != CaseMode.PARTY) {
            throw new GeneralException(GeneralErrorCode.INVALID_PARAMETER, "VS 모드 사건이 아닙니다.");
        }
        if (aCase.getStatus() != CaseStatus.PENDING) {
            throw new GeneralException(GeneralErrorCode.FORBIDDEN, "이미 재판이 시작된 사건입니다.");
        }

        // 이미 참여했는지 (입장문 냈는지) 확인
        List<ArgumentInitial> arguments = argumentInitialRepository.findByaCaseOrderByTypeAsc(aCase);
        boolean alreadySubmitted = arguments.stream().anyMatch(arg -> arg.getUser().getId().equals(user.getId()));
        if (alreadySubmitted) {
            throw new GeneralException(GeneralErrorCode.INVALID_PARAMETER, "이미 이 사건에 입장문을 제출했습니다.");
        }

        DebateSide assignedSide;
        if (arguments.isEmpty()) {
            // 내가 첫 번째 제출자 -> A측 할당
            // createPartyCase에서 A측이 이미 생성되므로 이 분기는 사실상 실행X
            assignedSide = DebateSide.A;
        } else if (arguments.size() == 1) {
            // 내가 두 번째 제출자 -> B측 할당
            assignedSide = DebateSide.B;
        } else {
            // 이미 2명이 꽉 참 (로직상 PENDING 상태이므로 발생하기 어려움)
            throw new GeneralException(GeneralErrorCode.FORBIDDEN, "사건이 이미 꽉 찼습니다.");
        }

        // CaseParticipation 생성
        caseParticipationRepository.save(CaseParticipation.builder()
                .aCase(aCase)
                .user(user)
                .result(CaseResult.ONGOING)
                .build());

        // 입장문 생성
        ArgumentInitial argument = ArgumentInitial.builder()
                .aCase(aCase)
                .user(user)
                .type(assignedSide) // <-- 자동으로 할당된 B
                .mainArgument(requestDto.getMainArgument())
                .reasoning(requestDto.getReasoning())
                .build();
        argumentInitialRepository.save(argument);

        // 판결 트리거 (내가 B측(두 번째)일 경우)
        if (assignedSide == DebateSide.B) {
            ArgumentInitial firstArgument = arguments.get(0); // 기존 A측 입장문
            List<ArgumentInitial> allArguments = List.of(firstArgument, argument);
            Long hostUserId = firstArgument.getUser().getId(); // 방장(A) ID

            NotificationResponseDto dto = NotificationResponseDto.builder()
                    .message("상대방이 입장하여 1차 재판이 시작됩니다!")
                    .caseId(aCase.getId())
                    .iconUrl("https://ddangddangddang-demoday.s3.ap-northeast-2.amazonaws.com/noti/person.png")
                    .build();

            // A에게 알림 전송
            sseEmitters.sendNotification(hostUserId, "notification", dto);

            // AI 1차 판결 요청
            AiJudgmentDto aiResult = chatGptService.getAiJudgment(aCase, allArguments);

            Judgment judgment = Judgment.builder()
                    .aCase(aCase)
                    .stage(JudgmentStage.INITIAL)
                    .content(aiResult.getVerdict())
                    .basedOn(aiResult.getConclusion())
                    .ratioA(aiResult.getRatioA())
                    .ratioB(aiResult.getRatioB())
                    .build();
            judgmentRepository.save(judgment);

            // 사건 상태를 PENDING -> FIRST로 변경
            //파티모드의 경우 바로 second로 가도록 설정
            aCase.updateStatus(CaseStatus.FIRST);
            if(aCase.getMode() == CaseMode.PARTY) {
                aCase.updateStatus(CaseStatus.SECOND);
            }
        }

        eventPublisher.publishEvent(new CaseParticipationEvent(user));
    }


    @Transactional(readOnly = true)
    public JudgmentResponseDto getJudgment(Long caseId, User user) {
        Case foundCase = findCaseById(caseId);
        // TODO: 본인 확인 로직

        Judgment judgment = judgmentRepository.findByaCase_IdAndStage(caseId, JudgmentStage.INITIAL)
                .orElseThrow(() -> new GeneralException(GeneralErrorCode.INVALID_PARAMETER, "아직 판결이 완료되지 않았습니다."));

        return new JudgmentResponseDto(judgment);
    }

    @Transactional(readOnly = true)
    public CaseDetailResponseDto getCaseDetail(Long caseId, User user) {
        Case aCase = findCaseById(caseId);

        List<ArgumentInitial> arguments = argumentInitialRepository.findByaCaseOrderByTypeAsc(aCase);
        if (arguments.isEmpty()) {
            throw new GeneralException(GeneralErrorCode.INTERNAL_SERVER_ERROR, "데이터 오류: 입장문을 찾을 수 없습니다.");
        }

        ArgumentInitial argA = arguments.get(0);
        // B측 입장문이 없으면 null로 처리 (PENDING 상태 대응)
        ArgumentInitial argB = (arguments.size() > 1) ? arguments.get(1) : null;

        if (aCase.getMode() == CaseMode.SOLO && !argA.getUser().getId().equals(user.getId())) {
            throw new GeneralException(GeneralErrorCode.FORBIDDEN, "해당 사건에 대한 조회 권한이 없습니다.");
        }

        rankingService.addCaseScore(caseId, 1.0);

        return CaseDetailResponseDto.builder()
                .aCase(aCase)
                .argA(argA)
                .argB(argB) // null이 들어갈 수 있음
                .build();
    }

    @Transactional
    public void updateCaseStatus(Long caseId, CaseStatusRequestDto requestDto, User user) {
        Case foundCase = findCaseById(caseId);
        // TODO: 본인 확인 로직

        if (requestDto.getStatus() == CaseStatus.SECOND) {
            throw new GeneralException(GeneralErrorCode.INVALID_PARAMETER, "2차 재판 시작은 /appeal API를 이용해주세요.");
        }

        if (requestDto.getStatus() == CaseStatus.DONE) {
            foundCase.updateStatus(CaseStatus.DONE);
        } else {
            throw new GeneralException(GeneralErrorCode.INVALID_PARAMETER, "잘못된 상태 값입니다. DONE만 가능합니다.");
        }
    }

    @Transactional
    public List<CasePendingResponseDto> getPendingCases() {
        // 1. PENDING 상태인 사건 목록을 최신순으로 조회
        List<Case> pendingCases = caseRepository.findAllByStatusOrderByCreatedAtDesc(CaseStatus.PENDING);

        // 2. N+1 문제 방지를 위해 모든 PENDING 사건의 1차 입장문을 한 번에 조회
        List<ArgumentInitial> arguments = argumentInitialRepository.findByaCaseInOrderByTypeAsc(pendingCases);

        // 3. Case ID 기준으로 A측 입장문(첫 번째)을 매핑
        Map<Long, ArgumentInitial> argumentAMap = arguments.stream()
                .filter(arg -> arg.getType() == DebateSide.A) // A측 입장문만 필터링
                .collect(Collectors.toMap(
                        arg -> arg.getACase().getId(), // Key: Case ID
                        arg -> arg,                    // Value: ArgumentInitial 객체
                        (existing, replacement) -> existing // 중복 시 기존 값 유지
                ));

        // 4. DTO로 변환하여 반환
        return pendingCases.stream()
                .map(aCase -> {
                    ArgumentInitial argumentA = argumentAMap.get(aCase.getId());
                    // A측 입장문이 없는 PENDING case는 논리적으로 없어야 하지만, 방어 코드
                    if (argumentA == null) {
                        return null;
                    }
                    return new CasePendingResponseDto(aCase, argumentA);
                })
                .filter(dto -> dto != null) // 혹시 모를 null 제거
                .collect(Collectors.toList());
    }

    private Case findCaseById(Long caseId) {
        return caseRepository.findById(caseId)
                .orElseThrow(() -> new GeneralException(GeneralErrorCode.INVALID_PARAMETER, "사건을 찾을 수 없습니다."));
    }

    // [추가] 사건 검색 메서드
    @Transactional(readOnly = true)
    public List<CaseOnResponseDto> searchCases(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            throw new GeneralException(GeneralErrorCode.INVALID_PARAMETER, "검색어를 입력해주세요.");
        }

        // Repository에서 키워드로 검색
        List<Case> foundCases = caseRepository.findByTitleContainingOrderByCreatedAtDesc(keyword);

        // 엔티티 리스트를 DTO 리스트로 변환
        return convertToDto(foundCases);
    }

    // 엔티티 리스트를 DTO 리스트로 변환하는 헬퍼 메서드 (private)
    private List<CaseOnResponseDto> convertToDto(List<Case> cases) {
        if (cases.isEmpty()) {
            return Collections.emptyList();
        }

        // N+1 문제 방지를 위해 검색된 모든 Case의 입장문을 한 번에 조회
        List<ArgumentInitial> arguments = argumentInitialRepository.findByaCaseInOrderByTypeAsc(cases);

        // Case ID별로 입장문(MainArgument) 리스트를 그룹핑
        Map<Long, List<String>> argumentsMap = arguments.stream()
                .collect(Collectors.groupingBy(
                        arg -> arg.getACase().getId(),
                        Collectors.mapping(ArgumentInitial::getMainArgument, Collectors.toList())
                ));

        // Case 리스트를 DTO로 변환
        return cases.stream()
                .map(aCase -> CaseOnResponseDto.builder()
                        .caseId(aCase.getId())
                        .title(aCase.getTitle())
                        .status(aCase.getStatus())
                        .mainArguments(argumentsMap.getOrDefault(aCase.getId(), Collections.emptyList()))
                        .build())
                .collect(Collectors.toList());
    }
}