package com.tianji.learning.service.Impl;

import com.tianji.api.client.user.UserClient;
import com.tianji.common.utils.DateUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.constants.RedisConstants;
import com.tianji.learning.domain.po.PointsBoard;
import com.tianji.learning.domain.query.PointsBoardQuery;
import com.tianji.learning.domain.vo.PointsBoardItemVO;
import com.tianji.learning.domain.vo.PointsBoardVO;
import com.tianji.learning.mapper.PointsBoardMapper;
import com.tianji.learning.service.IPointsBoardService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * <p>
 * 学霸天梯榜 服务实现类
 * </p>
 *
 * @author ldn
 * @since 2024-12-09
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PointsBoardServiceImpl extends ServiceImpl<PointsBoardMapper, PointsBoard> implements IPointsBoardService {
    private final StringRedisTemplate redisTemplate;
    private final UserClient userClient;

    @Override
    public PointsBoardVO queryPointsBoard(PointsBoardQuery query) {
        //获取用户id
        Long userId = UserContext.getUser();
        //判断是否是当前赛季
        boolean isCurrentSeason = query.getSeason() == null || query.getSeason() == 0;
        //查询我的榜单排名和我的分数
        PointsBoard myBoard = isCurrentSeason ?
                this.currentSeasonMyBoard(userId) : this.historySeasonMyBoard(userId);
        //查询前100名的信息
        List<PointsBoardItemVO> boardList = isCurrentSeason ?
                this.currentSeasonBoardList() : this.historySeasonBoardList();
        //封装返回值
        return PointsBoardVO.builder()
                .rank(myBoard.getId().intValue())
                .points(myBoard.getPoints())
                .boardList(boardList)
                .build();
    }

    private List<PointsBoardItemVO> historySeasonBoardList() {
        return null;
    }

    private List<PointsBoardItemVO> currentSeasonBoardList() {
        LocalDate now = LocalDate.now();
        String key = RedisConstants.POINTS_BOARD_KEY_PREFIX + now.format(DateUtils.POINTS_BOARD_SUFFIX_FORMATTER);
        Set<ZSetOperations.TypedTuple<String>> typedTuples = redisTemplate.opsForZSet().reverseRangeWithScores(key, 0, 100);
        List<PointsBoardItemVO> list = new ArrayList<>();
        if (typedTuples == null) {
            return list;
        }
        Integer rank = 1;
        for (ZSetOperations.TypedTuple<String> tuple : typedTuples) {
            if (tuple == null || tuple.getValue() == null || tuple.getScore() == null || tuple.getValue().isEmpty()) {
                continue;
            }
            log.info("tupleId:{}", tuple.getValue());
            list.add(PointsBoardItemVO.builder()
                    .points(tuple.getScore().intValue())
                    .rank(rank++)
                    .name(userClient.queryUserById(Long.parseLong(tuple.getValue())).getName())
                    .build());
        }
        return list;
    }

    private PointsBoard historySeasonMyBoard(Long userId) {
        return null;
    }

    private PointsBoard currentSeasonMyBoard(Long userId) {
        LocalDate now = LocalDate.now();
        String key = RedisConstants.POINTS_BOARD_KEY_PREFIX + now.format(DateUtils.POINTS_BOARD_SUFFIX_FORMATTER);
        Double score = redisTemplate.opsForZSet().score(key, userId.toString());
        Long rank = redisTemplate.opsForZSet().reverseRank(key, userId.toString());
        if (score == null || rank == null) {
            return PointsBoard.builder()
                    .points(0)
                    .rank(0)
                    .build();
        }
        return PointsBoard.builder()
                .points(score.intValue())
                .id(rank + 1)
                .build();
    }
}
