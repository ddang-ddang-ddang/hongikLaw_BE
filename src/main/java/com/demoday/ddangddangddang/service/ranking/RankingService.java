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
    public List<String> getHotCaseIds(int topN) {
        // ZREVRANGE 명령어: 점수가 높은 순(역순)으로 0위부터 (topN-1)위까지 조회
        Set<String> hotCasesSet = redisTemplate.opsForZSet().reverseRange(HOT_CASES_KEY, 0, topN - 1);

        if (hotCasesSet == null) {
            return List.of(); // 비어있는 리스트 반환
        }

        // Set을 List로 변환하여 순서 보장 (Sorted Set이므로 이미 순서대로 반환됨)
        return hotCasesSet.stream().collect(Collectors.toList());
    }

    public ApiResponse<List<CaseSimpleDto>> getHotCases(int topN) {

        // 1. Redis에서 랭킹 순으로 ID 목록 조회
        Set<String> hotCaseIdsSet = redisTemplate.opsForZSet().reverseRange(HOT_CASES_KEY, 0, topN - 1);

        if (hotCaseIdsSet == null || hotCaseIdsSet.isEmpty()) {
            // [수정] 비어있는 리스트도 함께 반환
            return ApiResponse.onSuccess("아직 핫한 사건이 없습니다.", Collections.emptyList());
        }

        // 2. ID 목록을 List<Long>으로 변환 (Redis 순서 유지)
        List<Long> hotCaseIds = hotCaseIdsSet.stream()
                .map(Long::parseLong)
                .collect(Collectors.toList());

        Collection<CaseStatus> statuses = new HashSet<>();
        statuses.add(CaseStatus.THIRD);
        statuses.add(CaseStatus.SECOND);

        // 3. DB에서 ID 목록에 해당하는 Case 정보 조회 (1번 쿼리)
        List<Case> hotCasesFromDb = caseRepository.findAllByIdInAndStatusIn(hotCaseIds,statuses);

        // 4. Map으로 변환 (Key: caseId, Value: Case 객체)
        Map<Long, Case> caseMap = hotCasesFromDb.stream()
                .collect(Collectors.toMap(Case::getId, Function.identity()));

        // 5. Redis 랭킹 순서(hotCaseIds)대로 Case 리스트 정렬
        List<Case> orderedCases = hotCaseIds.stream()
                .map(caseMap::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        // 6. [N+1 방지] 정렬된 Case 리스트로 모든 Arguments를 *한 번에* 조회 (2번 쿼리)
        List<ArgumentInitial> allArguments = argumentInitialRepository.findByaCaseInOrderByTypeAsc(orderedCases);

        // 7. Case ID별로 Argument 문자열 리스트를 그룹핑 (Map<CaseId, List<ArgumentString>>)
        Map<Long, List<String>> argumentsMap = allArguments.stream()
                .collect(Collectors.groupingBy(
                        argument -> argument.getACase().getId(), // Key: Case ID
                        Collectors.mapping(ArgumentInitial::getMainArgument, Collectors.toList()) // Value: List<String>
                ));

        // 8. 랭킹 순서(orderedCases)대로 DTO 빌드
        List<CaseSimpleDto> orderedHotCases = orderedCases.stream()
                .limit(10)
                .map(aCase -> {
                    // Map에서 해당 Case의 arguments 리스트를 찾음 (없으면 빈 리스트)
                    List<String> mainArguments = argumentsMap.getOrDefault(aCase.getId(), Collections.emptyList());

                    int distinctCount = caseRepository.countDistinctParticipants(aCase.getId());

                    return CaseSimpleDto.builder()
                            .caseId(aCase.getId())
                            .title(aCase.getTitle())
                            .mainArguments(mainArguments)
                            .participateCnt(distinctCount)
                            .build();
                })
                .collect(Collectors.toList());

        // 6. 공통 응답 형식으로 래핑하여 반환
        return ApiResponse.onSuccess("현재 핫한 사건 리스트 조회 성공",orderedHotCases);
    }

    // (참고) 랭킹과 점수를 함께 가져오려면?
    public Set<ZSetOperations.TypedTuple<String>> getHotCasesWithScores(int topN) {
        return redisTemplate.opsForZSet().reverseRangeWithScores(HOT_CASES_KEY, 0, topN - 1);
    }
}
