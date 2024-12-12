package com.tianji.learning.handler;

import com.tianji.common.utils.DateUtils;
import com.tianji.learning.constants.MysqlConstants;
import com.tianji.learning.service.IPointsRecordService;
import com.tianji.learning.service.Impl.PointsBoardServiceImpl;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * @author 鲁昊天
 * @date 2024/12/12
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class PointsBoardPersistentHandler {
    private final IPointsRecordService pointsRecordService;

    @XxlJob("createTableJob")
    void createPointsBoardTableOfLastSeason() {
        log.info("创建上赛季积分榜表到数据库");
        LocalDate now = LocalDate.now().minusMonths(1);
        String lastMonth = now.format(DateUtils.POINTS_BOARD_SUFFIX_FORMATTER);
        String tableName = MysqlConstants.POINTS_BOARD_TABLE_PREFIX + lastMonth;
        pointsRecordService.createPointsBoardTableBySeason(tableName);
    }
}
