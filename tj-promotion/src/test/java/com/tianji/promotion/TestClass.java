package com.tianji.promotion;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianji.promotion.domain.po.Coupon;
import com.tianji.promotion.enums.CouponStatus;
import com.tianji.promotion.service.ICouponService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;

/**
 * @author 鲁昊天
 * @date 2024/12/29
 */
@SpringBootTest
@Slf4j
public class TestClass {
    @Autowired
    private ICouponService couponService;

    @Test
    public void test() {
        Page<Coupon> page = new Page<>(4, 1);
        Page<Coupon> coupons2 = couponService.lambdaQuery()
                .eq(Coupon::getStatus, CouponStatus.ISSUING)
                .le(Coupon::getIssueEndTime, LocalDateTime.now())
                .page(page);
        System.out.println(coupons2.getTotal());
        System.out.println(coupons2.getPages());

    }
}