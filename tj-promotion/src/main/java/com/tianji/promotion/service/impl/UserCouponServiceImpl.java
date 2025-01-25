package com.tianji.promotion.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.common.exceptions.BadRequestException;
import com.tianji.common.exceptions.BizIllegalException;
import com.tianji.common.utils.UserContext;
import com.tianji.promotion.domain.po.Coupon;
import com.tianji.promotion.domain.po.ExchangeCode;
import com.tianji.promotion.domain.po.UserCoupon;
import com.tianji.promotion.enums.ExchangeCodeStatus;
import com.tianji.promotion.mapper.CouponMapper;
import com.tianji.promotion.mapper.UserCouponMapper;
import com.tianji.promotion.service.IExchangeCodeService;
import com.tianji.promotion.service.IUserCouponService;
import com.tianji.promotion.utils.CodeUtil;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.aop.framework.AopContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * <p>
 * 用户领取优惠券的记录，是真正使用的优惠券信息 服务实现类
 * </p>
 *
 * @author 小牛
 * @since 2025-01-18
 */
@Service
@RequiredArgsConstructor
public class UserCouponServiceImpl extends ServiceImpl<UserCouponMapper, UserCoupon> implements IUserCouponService {
    private final CouponMapper couponMapper;
    private final IExchangeCodeService codeService;
    private final RedissonClient redissonClient;

    /**
     * 用户领取优惠卷
     *
     * @param couponId 优惠卷id
     */
    @Override
    public void receiveCoupon(Long couponId) {
        if (couponId == null) {
            throw new RuntimeException("优惠卷id不能为空");
        }
        Coupon coupon = couponMapper.selectById(couponId);
        if (coupon == null) {
            throw new RuntimeException("优惠卷不存在");
        }
        LocalDateTime now = LocalDateTime.now();
        if (!now.isAfter(coupon.getIssueBeginTime()) && !now.isBefore(coupon.getIssueEndTime())) {
            throw new RuntimeException("优惠卷不在领取时间范围内");
        }
        // 3.校验库存
        if (coupon.getIssueNum() >= coupon.getTotalNum()) {
            throw new BadRequestException("优惠券库存不足");
        }
        Long userId = UserContext.getUser();
        // 4.校验每人限领数量
        // 4.1.统计当前用户对当前优惠券的已经领取的数量
        // 4.2.校验限领数量
        // 5.更新优惠券的已经发放的数量 + 1
        // 6.新增一个用户券
        String key = "lock:coupon:uid" + userId;
        RLock lock = redissonClient.getLock(key);
        try {
            boolean isLock = lock.tryLock();
            if (!isLock) {
                throw new RuntimeException("请稍后再试");
            }
            IUserCouponService userCouponService = (IUserCouponService) AopContext.currentProxy();
            userCouponService.checkAndCreateUserCoupon(coupon, userId, null);
        } finally {
            lock.unlock();
        }
    }

    @Transactional
    @Override
    public void exchangeCoupon(String code) {
        Long serialNum = CodeUtil.parseCode(code);
        // 2.校验是否已经兑换 SETBIT KEY 4 1 ，这里直接执行setbit，通过返回值来判断是否兑换过
        boolean exchanged = codeService.updateExchangeMark(serialNum, true);
        if (exchanged) {
            throw new BizIllegalException("兑换码已经被兑换过了");
        }
        try {
            // 3.查询兑换码对应的优惠券id
            ExchangeCode exchangeCode = codeService.getById(serialNum);
            if (exchangeCode == null) {
                throw new BizIllegalException("兑换码不存在");
            }
            // 4.是否过期
            LocalDateTime now = LocalDateTime.now();
            if (now.isAfter(exchangeCode.getExpiredTime())) {
                throw new BizIllegalException("兑换码已经过期");
            }
            // 5.校验并生成用户券
            // 5.1.查询优惠券
            Coupon coupon = couponMapper
                    .selectById(exchangeCode.getExchangeTargetId());
            // 5.2.查询用户
            Long userId = UserContext.getUser();
            // 5.3.校验并生成用户券，更新兑换码状态
            this.checkAndCreateUserCoupon(coupon, userId, serialNum.intValue());
        } catch (Exception e) {
            // 重置兑换的标记 0
            codeService.updateExchangeMark(serialNum, false);
            throw new BizIllegalException("兑换失败");

        }
    }

    /**
     * 保存用户券
     *
     * @param coupon 优惠卷
     * @param userId 用户id
     */
    private void saveUserCoupon(Coupon coupon, Long userId) {

        UserCoupon userCoupon = new UserCoupon();
        userCoupon.setCouponId(coupon.getId());
        userCoupon.setUserId(userId);
        // 2.有效期信息
        LocalDateTime termBeginTime = coupon.getTermBeginTime();
        LocalDateTime termEndTime = coupon.getTermEndTime();
        if (termBeginTime == null) {
            termBeginTime = LocalDateTime.now();
            termEndTime = termBeginTime.plusDays(coupon.getTermDays());
        }
        userCoupon.setTermBeginTime(termBeginTime);
        userCoupon.setTermEndTime(termEndTime);
        this.save(userCoupon);
    }

    /**
     * @param coupon    优惠卷
     * @param userId    用户id
     * @param serialNum 兑换码
     */
    @Transactional
    @Override
    public void checkAndCreateUserCoupon(Coupon coupon, Long userId, Integer serialNum) {
        // 1.校验每人限领数量
        // 1.1.统计当前用户对当前优惠券的已经领取的数量
        Integer count = lambdaQuery()
                .eq(UserCoupon::getUserId, userId)
                .eq(UserCoupon::getCouponId, coupon.getId())
                .count();
        // 1.2.校验限领数量
        if (count != null && count >= coupon.getUserLimit()) {
            throw new BadRequestException("超出领取数量");
        }
        // 2.更新优惠券的已经发放的数量 + 1
        int updateNumber = couponMapper.incrIssueNum(coupon.getId());
        if (updateNumber == 0) {
            throw new BizIllegalException("优惠券库存不足");
        }
        // 3.新增一个用户券
        saveUserCoupon(coupon, userId);
        // 4.更新兑换码状态
        if (serialNum != null) {
            codeService.lambdaUpdate()
                    .set(ExchangeCode::getUserId, userId)
                    .set(ExchangeCode::getStatus, ExchangeCodeStatus.USED)
                    .eq(ExchangeCode::getId, serialNum)
                    .update();
        }
    }

}
