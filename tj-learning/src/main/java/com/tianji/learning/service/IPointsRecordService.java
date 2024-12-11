package com.tianji.learning.service;

import com.tianji.learning.domain.po.PointsRecord;
import com.baomidou.mybatisplus.extension.service.IService;
import com.tianji.learning.mq.message.SignInMessage;

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
}
