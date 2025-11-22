package com.demoday.ddangddangddang.service.third;

import com.demoday.ddangddangddang.domain.*;
import com.demoday.ddangddangddang.domain.enums.ContentType;
import com.demoday.ddangddangddang.domain.enums.DebateSide;
import com.demoday.ddangddangddang.domain.event.AdoptedEvent;
import com.demoday.ddangddangddang.dto.home.UserDefenseRebuttalResponseDto;
import com.demoday.ddangddangddang.dto.third.AdoptRequestDto;
import com.demoday.ddangddangddang.dto.third.AdoptResponseDto;
import com.demoday.ddangddangddang.dto.third.AdoptableItemDto;
import com.demoday.ddangddangddang.global.apiresponse.ApiResponse;
import com.demoday.ddangddangddang.global.code.GeneralErrorCode;
import com.demoday.ddangddangddang.global.exception.GeneralException;
import com.demoday.ddangddangddang.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Transactional
public class AdoptService {
    private final CaseRepository caseRepository;
    private final RebuttalRepository rebuttalRepository;
    private final DefenseRepository defenseRepository;
    private final UserRepository userRepository;
    private final ArgumentInitialRepository argumentInitialRepository;
    private final ApplicationEventPublisher eventPublisher;

    //좋아요 많은 순으로 노출 (유저가 채택화면에서 보게 될 선택지)
    public ApiResponse<AdoptResponseDto> getOpinionBest(Long userId, Long caseId) {

        Case aCase = caseRepository.findById(caseId)
                .orElseThrow(()->new GeneralException(GeneralErrorCode.CASE_NOT_FOUND));

        //유저가 initial한 사건인지 확인
        List<ArgumentInitial> allInitialArguments = argumentInitialRepository.findByaCaseOrderByTypeAsc(aCase);

        //하나라도 유저가 참여한 항목 반환
        ArgumentInitial userInitialArgument = allInitialArguments.stream()
                .filter(arg -> arg.getUser().getId().equals(userId))
                .findFirst()
                .orElseThrow(() -> new GeneralException(GeneralErrorCode.FORBIDDEN_USER_NOT_PART_OF_DEBATE));

        //유저 진영 확인
        DebateSide type = userInitialArgument.getType();

        // [FIX for error 50] BLIND 미포함 메서드 사용
        List<Defense> allDefenses = defenseRepository.findAllByaCase_IdAndTypeAndIsBlindFalse(caseId, type);

        // [RebuttalRepository의 변경도 반영 필요] BLIND 미포함 메서드 사용
        List<Rebuttal> allRebuttals = rebuttalRepository.findAllByDefense_aCase_IdAndTypeAndIsBlindFalse(caseId, type);

        Stream<AdoptableItemDto> defenseStream = allDefenses.stream()
                .map(d -> AdoptableItemDto.builder()
                        .itemType(ContentType.DEFENSE)
                        .id(d.getId())
                        .caseId(d.getACase().getId())
                        .userId(d.getUser().getId())
                        .debateSide(d.getType())
                        .content(d.getContent())
                        .likeCount(d.getLikesCount())
                        // defenseId, parentId, parentContent는 null (자동)
                        .build()
                );

        Stream<AdoptableItemDto> rebuttalStream = allRebuttals.stream()
                .map(r -> {
                    Rebuttal parent = r.getParent();
                    return AdoptableItemDto.builder()
                            .itemType(ContentType.REBUTTAL)
                            .id(r.getId())
                            .caseId(r.getDefense().getACase().getId())
                            .userId(r.getUser().getId())
                            .debateSide(r.getType())
                            .content(r.getContent())
                            .likeCount(r.getLikesCount())
                            // --- Rebuttal 전용 필드 ---
                            .defenseId(r.getDefense().getId())
                            .parentId((parent != null) ? parent.getId() : null)
                            .parentContent((parent != null) ? parent.getContent() : null)
                            .build();
                });

        // 3. [변경] 두 스트림을 합치고(concat), 정렬(sorted)하고, 5개만(limit) 선택
        List<AdoptableItemDto> top5Items = Stream.concat(defenseStream, rebuttalStream)
                .sorted(Comparator.comparing(AdoptableItemDto::getLikeCount).reversed()) // 좋아요 순 정렬
                .limit(5)
                .toList();

        // 4. [변경] 새로운 DTO 형식으로 응답 생성
        AdoptResponseDto responseDto = AdoptResponseDto.builder()
                .items(top5Items) // 'items' 필드에 통합 리스트를 담아 전달
                .build();

        return ApiResponse.onSuccess("좋아요 많은 반론 및 변론 조회 완료",responseDto);
    }

    //사건 진행 third로 바꿈
    public ApiResponse<Void> changeThird(Long userId, Long caseId) {
        User user = userRepository.findById(userId)
                .orElseThrow(()-> new GeneralException(GeneralErrorCode.USER_NOT_FOUND));

        Case aCase = caseRepository.findById(caseId)
                .orElseThrow(()->new GeneralException(GeneralErrorCode.CASE_NOT_FOUND));

        //유저가 initial한 사건인지 확인
        List<ArgumentInitial> allInitialArguments = argumentInitialRepository.findByaCaseOrderByTypeAsc(aCase);

        //하나라도 유저가 참여한 항목 반환
        ArgumentInitial userInitialArgument = allInitialArguments.stream()
                .filter(arg -> arg.getUser().getId().equals(userId))
                .findFirst()
                .orElseThrow(() -> new GeneralException(GeneralErrorCode.FORBIDDEN_USER_NOT_PART_OF_DEBATE));

        aCase.setThird();

        return ApiResponse.onSuccess("사건 상태 THIRD로 변경 완료",null);
    }

    //채택(선택함)
    public ApiResponse<String> createAdopt(Long userId, Long caseId, AdoptRequestDto adoptRequestDto) {
        User user = userRepository.findById(userId)
                .orElseThrow(()-> new GeneralException(GeneralErrorCode.USER_NOT_FOUND));

        Case aCase = caseRepository.findById(caseId)
                .orElseThrow(()->new GeneralException(GeneralErrorCode.CASE_NOT_FOUND));

        // 유저 진영 파악
        List<ArgumentInitial> allInitialArguments = argumentInitialRepository.findByaCaseOrderByTypeAsc(aCase);
        ArgumentInitial userInitialArgument = allInitialArguments.stream()
                .filter(arg -> arg.getUser().getId().equals(userId))
                .findFirst()
                .orElseThrow(() -> new GeneralException(GeneralErrorCode.FORBIDDEN_USER_NOT_PART_OF_DEBATE));

        DebateSide mySide = userInitialArgument.getType();

        // 1. 기존 채택 내역 조회 (내 진영)
        List<Defense> alreadyAdoptedDefenses = defenseRepository.findAllByaCase_IdAndTypeAndIsAdoptedTrue(caseId, mySide);
        List<Rebuttal> alreadyAdoptedRebuttals = rebuttalRepository.findAllByDefense_aCase_IdAndTypeAndIsAdoptedTrue(caseId, mySide);

        // ★ 핵심 로직: 기존에 하나라도 채택된 게 있었다면, 이번 요청은 '수정'이다.
        boolean isEditMode = (!alreadyAdoptedDefenses.isEmpty() || !alreadyAdoptedRebuttals.isEmpty());

        // 2. 기존 채택 해제 (경험치 회수 로직 삭제됨)
        for (Defense oldDefense : alreadyAdoptedDefenses) {
            oldDefense.markAsAdoptedFalse(); // isAdopted = false 만 수행
        }
        for (Rebuttal oldRebuttal : alreadyAdoptedRebuttals) {
            oldRebuttal.markAsAdoptedFalse(); // isAdopted = false 만 수행
        }

        // 3. 새로운 Defense 채택 적용
        List<Long> defenseIds = adoptRequestDto.getDefenseId();
        if (defenseIds != null && !defenseIds.isEmpty()) {
            List<Defense> defensesToAdopt = defenseRepository.findAllById(defenseIds);
            for (Defense defense : defensesToAdopt) {
                if (defense.getType() == mySide) {
                    defense.markAsAdopted(); // 채택 마크

                    // ★ 수정 모드가 아닐 때(최초 채택일 때)만 경험치 지급
                    if (!isEditMode) {
                        defense.getUser().updateExp(100L);
                        eventPublisher.publishEvent(new AdoptedEvent(defense.getUser(), ContentType.DEFENSE));
                    }
                }
            }
        }

        // 4. 새로운 Rebuttal 채택 적용
        List<Long> rebuttalIds = adoptRequestDto.getRebuttalId();
        if (rebuttalIds != null && !rebuttalIds.isEmpty()) {
            List<Rebuttal> rebuttalsToAdopt = rebuttalRepository.findAllById(rebuttalIds);
            for (Rebuttal rebuttal : rebuttalsToAdopt) {
                if (rebuttal.getType() == mySide) {
                    rebuttal.markAsAdopted(); // 채택 마크

                    // ★ 수정 모드가 아닐 때(최초 채택일 때)만 경험치 지급
                    if (!isEditMode) {
                        rebuttal.getUser().updateExp(100L);
                        eventPublisher.publishEvent(new AdoptedEvent(rebuttal.getUser(), ContentType.REBUTTAL));
                    }
                }
            }
        }

        String message = isEditMode ? "채택 목록이 수정되었습니다. (경험치 추가 지급 없음)" : "의견 채택 및 경험치 지급 완료";
        return ApiResponse.onSuccess("success", message);
    }

    public ApiResponse<String> adoptAuto(Long userId, Long caseId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(GeneralErrorCode.USER_NOT_FOUND));

        Case aCase = caseRepository.findById(caseId)
                .orElseThrow(() -> new GeneralException(GeneralErrorCode.CASE_NOT_FOUND));

        // 유저의 진영 확인
        List<ArgumentInitial> allInitialArguments = argumentInitialRepository.findByaCaseOrderByTypeAsc(aCase);
        ArgumentInitial userInitialArgument = allInitialArguments.stream()
                .filter(arg -> arg.getUser().getId().equals(userId))
                .findFirst()
                .orElseThrow(() -> new GeneralException(GeneralErrorCode.FORBIDDEN_USER_NOT_PART_OF_DEBATE));

        // 공통 로직 호출 (해당 진영에 대해 자동 채택 수행)
        performAutoAdoptForSide(aCase, userInitialArgument.getType());

        return ApiResponse.onSuccess("success", "상위 5개 항목이 자동으로 채택되었습니다.");
    }

    /**
     * [추가된 메서드] 스케줄러를 위한 시스템 자동 채택
     * userId 없이 Case 객체만으로 해당 사건의 양쪽 진영 모두에 대해 자동 채택을 수행합니다.
     */
    public void executeSystemAutoAdopt(Case aCase) {
        // 1. 해당 사건의 초기 참여자 정보(진영 정보)를 모두 가져옴
        List<ArgumentInitial> participants = argumentInitialRepository.findByaCaseOrderByTypeAsc(aCase);

        // 2. 각 참여자(진영) 별로 자동 채택 로직 수행
        for (ArgumentInitial participant : participants) {
            performAutoAdoptForSide(aCase, participant.getType());
        }

        // 3. 사건 상태 변경
        aCase.setThird();
    }

    /**
     * [내부 공통 메서드] 특정 사건, 특정 진영에 대한 상위 5개 자동 채택 로직
     * userId 검증 없이 순수하게 DB 조회 및 업데이트만 수행
     */
    private void performAutoAdoptForSide(Case aCase, DebateSide type) {
        Long caseId = aCase.getId();

        // 1. 해당 진영의 Defense 조회 (BLIND 제외)
        List<Defense> allDefenses = defenseRepository.findAllByaCase_IdAndTypeAndIsBlindFalse(caseId, type);

        // 2. 해당 진영의 Rebuttal 조회 (BLIND 제외)
        List<Rebuttal> allRebuttals = rebuttalRepository.findAllByDefense_aCase_IdAndTypeAndIsBlindFalse(caseId, type);

        // 3. Stream 변환 및 통합 (getOpinionBest 로직 재사용)
        Stream<AdoptableItemDto> defenseStream = allDefenses.stream()
                .map(d -> AdoptableItemDto.builder()
                        .itemType(ContentType.DEFENSE)
                        .id(d.getId())
                        .likeCount(d.getLikesCount())
                        .build());

        Stream<AdoptableItemDto> rebuttalStream = allRebuttals.stream()
                .map(r -> AdoptableItemDto.builder()
                        .itemType(ContentType.REBUTTAL)
                        .id(r.getId())
                        .likeCount(r.getLikesCount())
                        .build());

        // 4. 좋아요 순 정렬 후 상위 5개 추출
        List<AdoptableItemDto> top5Items = Stream.concat(defenseStream, rebuttalStream)
                .sorted(Comparator.comparing(AdoptableItemDto::getLikeCount).reversed())
                .limit(5)
                .toList();

        if (top5Items.isEmpty()) return;

        // 5. 채택 상태 변경 및 경험치 지급
        for (AdoptableItemDto item : top5Items) {
            if (item.getItemType() == ContentType.DEFENSE) {
                defenseRepository.findById(item.getId()).ifPresent(defense -> {
                    // 이미 채택된 경우 중복 처리 방지 (옵션)
                    if (!Boolean.TRUE.equals(defense.getIsAdopted())) {
                        defense.markAsAdopted();
                        defense.getUser().updateExp(100L);
                        eventPublisher.publishEvent(new AdoptedEvent(defense.getUser(),ContentType.DEFENSE));
                    }
                });
            } else if (item.getItemType() == ContentType.REBUTTAL) {
                rebuttalRepository.findById(item.getId()).ifPresent(rebuttal -> {
                    if (!Boolean.TRUE.equals(rebuttal.getIsAdopted())) {
                        rebuttal.markAsAdopted();
                        rebuttal.getUser().updateExp(100L);
                        eventPublisher.publishEvent(new AdoptedEvent(rebuttal.getUser(),ContentType.REBUTTAL));
                    }
                });
            }
        }
    }

    //채택된 반론&변론 조회
    public ApiResponse<AdoptResponseDto> getAdopt(Long caseId){
        Case acase = caseRepository.findById(caseId)
                .orElseThrow(()-> new GeneralException(GeneralErrorCode.CASE_NOT_FOUND));

        // [FIX for error 214] BLIND 미포함 메서드 사용
        List<Defense> adoptedDefenses = defenseRepository.findByaCase_IdAndIsAdoptedAndIsBlindFalse(caseId,Boolean.TRUE);
        List<Rebuttal> adoptedRebuttals = rebuttalRepository.findAdoptedRebuttalsByCaseId(caseId); // 이 메서드는 Repository에서 쿼리 수정됨

        Stream<AdoptableItemDto> defenseStream = adoptedDefenses.stream()
                .map(d -> AdoptableItemDto.builder()
                        .itemType(ContentType.DEFENSE)
                        .id(d.getId())
                        .caseId(d.getACase().getId())
                        .userId(d.getUser().getId())
                        .debateSide(d.getType())
                        .content(d.getContent())
                        .likeCount(d.getLikesCount())
                        .build()
                );

        Stream<AdoptableItemDto> rebuttalStream = adoptedRebuttals.stream()
                .map(r -> {
                    Rebuttal parent = r.getParent();
                    return AdoptableItemDto.builder()
                            .itemType(ContentType.REBUTTAL)
                            .id(r.getId())
                            .caseId(r.getDefense().getACase().getId())
                            .userId(r.getUser().getId())
                            .debateSide(r.getType())
                            .content(r.getContent())
                            .likeCount(r.getLikesCount())
                            .defenseId(r.getDefense().getId())
                            .parentId((parent != null) ? parent.getId() : null)
                            .parentContent((parent != null) ? parent.getContent() : null)
                            .build();
                });

        // [수정] 정렬 로직 추가
        List<AdoptableItemDto> adoptedItems = Stream.concat(defenseStream, rebuttalStream)
                .sorted(Comparator.comparing(AdoptableItemDto::getLikeCount).reversed()) // 좋아요 순 정렬
                .toList();

        AdoptResponseDto responseDto = AdoptResponseDto.builder()
                .items(adoptedItems) // 'items' 필드에 통합 리스트를 담아 전달
                .build();

        return ApiResponse.onSuccess("채택된 반론 및 변론 조회 완료",responseDto);
    }
}