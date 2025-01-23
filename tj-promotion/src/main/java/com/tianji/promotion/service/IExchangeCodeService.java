package com.tianji.promotion.service;

import com.tianji.promotion.domain.po.Coupon;
import com.tianji.promotion.domain.po.ExchangeCode;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 兑换码 服务类
 * </p>
 *
 * @author ldn
 * @since 2024-12-13
 */
public interface IExchangeCodeService extends IService<ExchangeCode> {
    void asyncGenerateCode(Coupon coupon);

    /**
     * 修改优惠卷对应位图
     * @param serialNum 对应的位图位置
     * @param b 修改的状态
     * @return 修改前的状态
     */
    boolean updateExchangeMark(Long serialNum, boolean b);
}
