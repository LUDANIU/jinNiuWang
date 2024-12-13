package com.tianji.learning.handler;

import com.tianji.common.utils.DateUtils;
import com.tianji.learning.constants.MysqlConstants;
import com.tianji.learning.constants.RedisConstants;
import com.tianji.learning.domain.po.PointsBoard;
import com.tianji.learning.service.IPointsBoardService;
import com.tianji.learning.service.IPointsRecordService;
import com.tianji.learning.utils.TableInfoContext;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author 鲁昊天
 * @date 2024/12/12
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class PointsBoardPersistentHandler {
    private final IPointsRecordService pointsRecordService;
    private final StringRedisTemplate redisTemplate;
    private final IPointsBoardService pointsBoardervice;

    @XxlJob("createTableJob")
    void createPointsBoardTableOfLastSeason() {
        log.info("创建上赛季积分榜表到数据库");
        LocalDate now = LocalDate.now().minusMonths(1);
        String lastMonth = now.format(DateUtils.POINTS_BOARD_SUFFIX_FORMATTER);
        String tableName = MysqlConstants.POINTS_BOARD_TABLE_PREFIX + lastMonth;
        pointsRecordService.createPointsBoardTableBySeason(tableName);
    }

    @XxlJob("savePointsBoard2DB")
    void savePointsBoard2DB() {
        //把赛季名称存入上下文
        LocalDate now = LocalDate.now().minusMonths(1);
        String lastMonth = now.format(DateUtils.POINTS_BOARD_SUFFIX_FORMATTER);
        TableInfoContext.setTableName(MysqlConstants.POINTS_BOARD_TABLE_PREFIX + lastMonth);
        //获取redis中的排行榜
        int start = 0;
        while (true) {
            Set<ZSetOperations.TypedTuple<String>> typedTuples = redisTemplate.opsForZSet().reverseRangeWithScores(
                    RedisConstants.POINTS_BOARD_KEY_PREFIX + lastMonth, start, start + 100);
            if (typedTuples == null || typedTuples.isEmpty()) {
                break;
            }
            Set<PointsBoard> set = typedTuples.stream()
                    .map(tuple -> PointsBoard.builder()
                            .userId(Long.valueOf(tuple.getValue()))
                            .points(tuple.getScore().intValue())
                            .build()
                    )
                    .collect(Collectors.toSet());
            log.info("准备保存{}个排行榜数据到数据库", set.size());
            log.info("保存的数据是:{}", set.toString());
            //分批保存排行榜
            pointsBoardervice.saveBatch(set);
            start += 101;
        }
        //删除redis中的排行榜
        redisTemplate.unlink(RedisConstants.POINTS_BOARD_KEY_PREFIX + lastMonth);
        //释放上下文
        TableInfoContext.remove();
    }
}
