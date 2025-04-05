package com.tianji.promotion.task;


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianji.promotion.domain.po.Coupon;
import com.tianji.promotion.enums.CouponStatus;
import com.tianji.promotion.service.ICouponService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;


/**
 * @author 鲁昊天
 * @date 2024/12/30
 */
@Component
@Slf4j
public class CouponIssueChangeHandler {
    @Resource
    private ICouponService couponService;

    @XxlJob("couponIssueJobHandler")
    public void couponIssueJobHandler() throws Exception {
        int shardIndex = XxlJobHelper.getShardIndex();
        log.info("优惠券发放任务执行，shardIndex:{}", shardIndex);
        int shardTotal = XxlJobHelper.getShardTotal();
        log.info("优惠券发放任务执行，shardTotal:{}", shardTotal);
        // 定时结束发放优惠券
        this.finishIssueCoupon(shardTotal, shardIndex);
        // 定时开始发放优惠券
        this.issueCoupon(shardTotal, shardIndex);
    }

    private void finishIssueCoupon(int shardTotal, int shardIndex) {
        //查看所有当前状态时
        int pageNumber = shardIndex + 1;
        while (true) {
            Page<Coupon> page = new Page<>(pageNumber, 2);
            log.info("开始查询第{}页", pageNumber);
            page = couponService.lambdaQuery()
                    .eq(Coupon::getStatus, CouponStatus.ISSUING)
                    .le(Coupon::getIssueEndTime, LocalDateTime.now())
                    .page(page);
            log.info("查询到{}页优惠卷的信息是：{}", pageNumber, page.getRecords().get(0));
            log.info("和：{}", page.getRecords().get(1));
            //如果查询到对应的优惠卷就结束优惠卷并且继续循环
            for (Coupon record : page.getRecords()) {
                couponService.lambdaUpdate()
                        .set(Coupon::getStatus, CouponStatus.FINISHED)
                        .eq(Coupon::getId, record.getId())
                        .update();
                log.info("结束了name:{} id：{}的优惠卷的发放", record.getName(), record.getId());
            }
            pageNumber += shardTotal;
            if (pageNumber > page.getPages()) {
                break;
            }
        }

    }

    private void issueCoupon(int shardTotal, int shardIndex) {

    }
}
