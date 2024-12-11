package com.tianji.learning.service;

import com.tianji.learning.domain.po.PointsBoard;
import com.baomidou.mybatisplus.extension.service.IService;
import com.tianji.learning.domain.query.PointsBoardQuery;
import com.tianji.learning.domain.vo.PointsBoardVO;

/**
 * <p>
 * 学霸天梯榜 服务类
 * </p>
 *
 * @author ldn
 * @since 2024-12-09
 */
public interface IPointsBoardService extends IService<PointsBoard> {
    /**
     *
     * @param query
     * @return
     */
    PointsBoardVO queryPointsBoard(PointsBoardQuery query);
}
