package com.tianji.learning.service.Impl;

import com.tianji.common.utils.UserContext;
import com.tianji.learning.constants.RedisConstants;
import com.tianji.learning.domain.po.PointsBoard;
import com.tianji.learning.domain.query.PointsBoardQuery;
import com.tianji.learning.domain.vo.PointsBoardVO;
import com.tianji.learning.mapper.PointsBoardMapper;
import com.tianji.learning.service.IPointsBoardService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 学霸天梯榜 服务实现类
 * </p>
 *
 * @author ldn
 * @since 2024-12-09
 */
@Service
@RequiredArgsConstructor
public class PointsBoardServiceImpl extends ServiceImpl<PointsBoardMapper, PointsBoard> implements IPointsBoardService {
    private final StringRedisTemplate redisTemplate;

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

        return null;
    }

    private PointsBoard historySeasonMyBoard(Long userId) {
        return null;
    }

    private PointsBoard currentSeasonMyBoard(Long userId) {
        double score = redisTemplate.opsForZSet().score(RedisConstants.POINTS_BOARD_KEY_PREFIX, userId);
        Long rank = redisTemplate.opsForZSet().reverseRank(RedisConstants.POINTS_BOARD_KEY_PREFIX, userId);
if(score == null|| rank == null){
    return PointsBoard.builder()
            .build();
}
        return PointsBoard.builder()
                .points((int) score)
                .id(rank)
                .build();
    }
}
