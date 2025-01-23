package com.tianji.promotion.service;

import com.tianji.promotion.domain.po.UserCoupon;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 用户领取优惠券的记录，是真正使用的优惠券信息 服务类
 * </p>
 *
 * @author 小牛
 * @since 2025-01-18
 */
public interface IUserCouponService extends IService<UserCoupon> {
    /**
     * 用户领取优惠卷
     * @param couponId 优惠卷id
     */
    void receiveCoupon(Long couponId);

    void exchangeCoupon(String code);
}
