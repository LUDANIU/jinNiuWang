package com.tianji.promotion.mapper;

import com.tianji.promotion.domain.po.Coupon;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Select;

/**
 * <p>
 * 优惠券的规则信息 Mapper 接口
 * </p>
 *
 * @author ldn
 * @since 2024-12-13
 */
public interface CouponMapper extends BaseMapper<Coupon> {
    /**
     * 给优惠卷已领数量加一
     *
     * @param id 优惠卷id
     */
    @Select("update coupon set issue_num = issue_num + 1 where id = #{id}")
    void incrIssueNum(Long id);
}
