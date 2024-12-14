package com.tianji.promotion.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.PageDto;
import com.tianji.promotion.domain.dto.CouponFormDTO;
import com.tianji.promotion.domain.po.Coupon;
import com.baomidou.mybatisplus.extension.service.IService;
import com.tianji.promotion.domain.query.CouponQuery;
import com.tianji.promotion.domain.vo.CouponPageVO;

/**
 * <p>
 * 优惠券的规则信息 服务类
 * </p>
 *
 * @author ldn
 * @since 2024-12-13
 */
public interface ICouponService extends IService<Coupon> {
    /*
     *新增优惠卷
     * */
    void saveCoupon(CouponFormDTO dto);

    PageDto<CouponPageVO> queryCouponByPage(CouponQuery query);
}
