package com.tianji.promotion.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.PageDto;
import com.tianji.promotion.domain.dto.CouponFormDTO;
import com.tianji.promotion.domain.dto.CouponIssueFormDTO;
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

    /*
     *分页查询优惠卷
     * */
    PageDto<CouponPageVO> queryCouponByPage(CouponQuery query);

    /*
     *发放优惠卷
     * */
    void issueCoupon(Long id, CouponIssueFormDTO dto);

    /*
     *修改优惠卷
     * */
    void updateCoupon(CouponFormDTO dto, Long id);
}
