package com.tianji.learning.service;

import com.tianji.learning.domain.po.PointsRecord;
import com.baomidou.mybatisplus.extension.service.IService;
import com.tianji.learning.domain.vo.PointsStatisticsVO;
import com.tianji.learning.mq.message.SignInMessage;

import java.util.List;

/**
 * <p>
 * 学习积分记录，每个月底清零 服务类
 * </p>
 *
 * @author ldn
 * @since 2024-12-09
 */
public interface IPointsRecordService extends IService<PointsRecord> {
    /*
     *添加签到积分
     * */
    void insertSignPoints(SignInMessage message);

    /*
     *查询今日积分情况
     * */
    List<PointsStatisticsVO> queryMyPointsToday();
/*
     *创建积分排行榜表
 */
    void createPointsBoardTableBySeason(String tableName);
}
