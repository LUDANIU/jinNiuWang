package com.tianji.promotion.service.impl;

import com.tianji.promotion.constants.PromotionConstants;
import com.tianji.promotion.domain.po.Coupon;
import com.tianji.promotion.domain.po.ExchangeCode;
import com.tianji.promotion.mapper.ExchangeCodeMapper;
import com.tianji.promotion.service.IExchangeCodeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.promotion.utils.CodeUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.BoundValueOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * 兑换码 服务实现类
 * </p>
 *
 * @author ldn
 * @since 2024-12-13
 */
@Service
@Slf4j
public class ExchangeCodeServiceImpl extends ServiceImpl<ExchangeCodeMapper, ExchangeCode> implements IExchangeCodeService {
    private final StringRedisTemplate redisTemplate;
    private final BoundValueOperations<String, String> serialOps;

    public ExchangeCodeServiceImpl(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.serialOps =
                redisTemplate.boundValueOps(PromotionConstants.COUPON_CODE_SERIAL_KEY);
    }

    @Override
    @Async("generateExchangeCodeExecutor")
    public void asyncGenerateCode(Coupon coupon) {
        //发放优惠卷的数量
        int num = coupon.getTotalNum();
        if (num <= 0) {
            throw new RuntimeException("发放优惠卷的数量不能小于0");
        }
        Long result = serialOps.increment(num);
        if (result == null) {
            throw new RuntimeException("发放失败");
        }
        int maxCouponNum = result.intValue();
        List<ExchangeCode> exchangeCodes = new ArrayList<>();
        for (int i = maxCouponNum; i >maxCouponNum- num; i--) {
            //给每一张优惠卷生成兑换码
            // 2.生成兑换码
            String code = CodeUtil.generateCode(i, coupon.getId());
            ExchangeCode e = new ExchangeCode();
            e.setCode(code);
            e.setId(i);
            e.setExchangeTargetId(coupon.getId());
            e.setExpiredTime(coupon.getIssueEndTime());
            exchangeCodes.add(e);
          System.out.println("当前执行的线程的名称："+Thread.currentThread().getName());
        }
        this.saveBatch(exchangeCodes);
    }
    /**
     * 修改优惠卷对应位图
     * @param serialNum 对应的位图位置
     * @param b 修改的状态
     * @return 修改前的状态
     */
    @Override
    public boolean updateExchangeMark(Long serialNum, boolean b) {
        Boolean boo = this.redisTemplate.opsForValue()
                .setBit(PromotionConstants.COUPON_CODE_MAP_KEY, serialNum, b);
        return  boo != null && boo;
    }
}
