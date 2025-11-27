package com.demoday.ddangddangddang.service.ranking;

import com.demoday.ddangddangddang.domain.ArgumentInitial;
import com.demoday.ddangddangddang.domain.Case;
import com.demoday.ddangddangddang.domain.enums.CaseStatus;
import com.demoday.ddangddangddang.dto.home.CaseSimpleDto;
import com.demoday.ddangddangddang.global.apiresponse.ApiResponse;
import com.demoday.ddangddangddang.repository.ArgumentInitialRepository;
import com.demoday.ddangddangddang.repository.CaseRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
// ... (imports)

@Service
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
@Transactional
public class RankingService {
    @Autowired
    private StringRedisTemplate redisTemplate;

    private final CaseRepository caseRepository;
    private final ArgumentInitialRepository argumentInitialRepository;

    private static final String HOT_CASES_KEY = "hot_cases";

    /**
     * 특정 재판(Case)에 핫한 점수를 추가합니다.
     * @param caseId 재판 ID
     * @param scoreToAdd 더할 점수 (예: 조회수 +1.0, 좋아요 +3.0)
     */
    public void addCaseScore(Long caseId, double scoreToAdd) {
        String caseIdStr = String.valueOf(caseId);

        // ZINCRBY 명령어: hot_cases 키에 caseIdStr 멤버의 점수를 scoreToAdd 만큼 증가시킴
        redisTemplate.opsForZSet().incrementScore(HOT_CASES_KEY, caseIdStr, scoreToAdd);
    }

    /**
     * 현재 핫한 재판 ID 목록을 상위 N개까지 반환합니다.
     * @param topN 가져올 랭킹 수 (예: 10)
     * @return 핫한 재판 ID 목록 (String)
     */
    public ApiResponse<List<CaseSimpleDto>> getHotCases(int topN) {
        List<CaseSimpleDto> resultList = new ArrayList<>();
        Long adCaseId = null;

        // 1. [광고 재판 조회] 가장 최신 광고 재판 1개를 조회하여 최상단에 추가
        Optional<Case> adCaseOpt = caseRepository.findTopByIsAdTrueOrderByCreatedAtDesc();

        if (adCaseOpt.isPresent()) {
            Case adCase = adCaseOpt.get();
            adCaseId = adCase.getId(); // 중복 방지를 위해 ID 저장

            // 광고 재판의 Argument(입장문) 조회
            List<ArgumentInitial> adArguments = argumentInitialRepository.findByaCaseOrderByTypeAsc(adCase);
            List<String> adMainArguments = adArguments.stream()
                    .map(ArgumentInitial::getMainArgument)
                    .collect(Collectors.toList());

            // 광고 재판 참여자 수 조회
            int adParticipateCnt = caseRepository.countDistinctParticipants(adCase.getId());

            // 결과 리스트에 광고 재판 추가 (isAd = true)
            resultList.add(CaseSimpleDto.builder()
                    .caseId(adCase.getId())
                    .title(adCase.getTitle())
                    .mainArguments(adMainArguments)
                    .participateCnt(adParticipateCnt)
                    .isAd(true)
                    .build());
        }

        // 2. [Redis 랭킹 조회]
        // 광고 재판이 랭킹에 포함되어 있을 경우를 대비해 (topN + 1)개 조회
        Set<String> hotCaseIdsSet = redisTemplate.opsForZSet().reverseRange(HOT_CASES_KEY, 0, topN);

        if (hotCaseIdsSet != null && !hotCaseIdsSet.isEmpty()) {
            List<Long> hotCaseIds = new ArrayList<>();

            // Redis에서 가져온 ID들을 순회하며 광고 재판 ID와 중복되는지 확인
            for (String idStr : hotCaseIdsSet) {
                Long id = Long.parseLong(idStr);

                // 이미 추가된 광고 재판이면 리스트에 넣지 않고 건너뜀 (중복 제거 전략)
                if (adCaseId != null && adCaseId.equals(id)) {
                    continue;
                }

                hotCaseIds.add(id);
                // 요청받은 topN 개수가 채워지면 중단
                if (hotCaseIds.size() >= topN) break;
            }

            if (!hotCaseIds.isEmpty()) {
                // 일반 재판은 2차(SECOND) 또는 3차(THIRD) 상태인 것만 조회
                Collection<CaseStatus> statuses = Set.of(CaseStatus.THIRD, CaseStatus.SECOND);

                // DB 조회 (IN 쿼리 사용)
                List<Case> hotCasesFromDb = caseRepository.findAllByIdInAndStatusIn(hotCaseIds, statuses);

                // 조회를 위해 Map으로 변환
                Map<Long, Case> caseMap = hotCasesFromDb.stream()
                        .collect(Collectors.toMap(Case::getId, Function.identity()));

                // Redis 랭킹 순서대로 Case 객체 정렬 (DB 조회 결과는 순서 보장 X)
                List<Case> orderedCases = hotCaseIds.stream()
                        .map(caseMap::get)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());

                // [N+1 방지] 모든 Case의 Argument를 한 번에 조회
                List<ArgumentInitial> allArguments = argumentInitialRepository.findByaCaseInOrderByTypeAsc(orderedCases);
                Map<Long, List<String>> argumentsMap = allArguments.stream()
                        .collect(Collectors.groupingBy(
                                arg -> arg.getACase().getId(),
                                Collectors.mapping(ArgumentInitial::getMainArgument, Collectors.toList())
                        ));

                // 일반 랭킹 재판들을 DTO로 변환하여 결과 리스트에 추가
                for (Case aCase : orderedCases) {
                    List<String> mainArguments = argumentsMap.getOrDefault(aCase.getId(), Collections.emptyList());
                    int distinctCount = caseRepository.countDistinctParticipants(aCase.getId());

                    resultList.add(CaseSimpleDto.builder()
                            .caseId(aCase.getId())
                            .title(aCase.getTitle())
                            .mainArguments(mainArguments)
                            .participateCnt(distinctCount)
                            .isAd(false) // 일반 재판 표시
                            .build());
                }
            }
        }

        return ApiResponse.onSuccess("현재 핫한 사건 리스트 조회 성공", resultList);
    }

    // (참고) 랭킹과 점수를 함께 가져오려면?
    public Set<ZSetOperations.TypedTuple<String>> getHotCasesWithScores(int topN) {
        return redisTemplate.opsForZSet().reverseRangeWithScores(HOT_CASES_KEY, 0, topN - 1);
    }
}
