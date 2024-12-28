package com.tianji.promotion.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.plugins.pagination.PageDto;
import com.tianji.common.exceptions.BizIllegalException;
import com.tianji.common.utils.BeanUtils;
import com.tianji.common.utils.StringUtils;
import com.tianji.promotion.domain.dto.CouponFormDTO;
import com.tianji.promotion.domain.dto.CouponIssueFormDTO;
import com.tianji.promotion.domain.po.Coupon;
import com.tianji.promotion.domain.po.CouponScope;
import com.tianji.promotion.domain.query.CouponQuery;
import com.tianji.promotion.domain.vo.CouponPageVO;
import com.tianji.promotion.enums.CouponStatus;
import com.tianji.promotion.mapper.CouponMapper;
import com.tianji.promotion.service.ICouponScopeService;
import com.tianji.promotion.service.ICouponService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * 优惠券的规则信息 服务实现类
 * </p>
 *
 * @author ldn
 * @since 2024-12-13
 */
@Service
@RequiredArgsConstructor
public class CouponServiceImpl extends ServiceImpl<CouponMapper, Coupon> implements ICouponService {
    private final ICouponScopeService couponScopeService;

    /*
     *新增优惠卷
     * */
    @Override
    @Transactional
    public void saveCoupon(CouponFormDTO dto) {
        Coupon coupon = BeanUtils.copyBean(dto, Coupon.class);
        this.save(coupon);
        //判断是否有范围
        if (!dto.getSpecific()) {
            //没范围
            return;
        }
        //有范围，保存范围
        List<Long> scopes = dto.getScopes();
        if (scopes == null) {
            throw new BizIllegalException("优惠券范围不能为空");
        }
        List<CouponScope> couponScopes = scopes.stream()
                .map(scopeId -> CouponScope.builder()
                        .couponId(coupon.getId())
                        .bizId(scopeId)
                        .build())
                .collect(Collectors.toList());
        couponScopeService.saveBatch(couponScopes);
    }

    @Override
    public PageDto<CouponPageVO> queryCouponByPage(CouponQuery query) {
        Page<Coupon> coupons = this.lambdaQuery()
                .like(!StringUtils.isBlank(query.getName()), Coupon::getName, query.getName())
                .eq(query.getStatus() != null, Coupon::getDiscountType, query.getStatus())
                .eq(query.getType() != null, Coupon::getType, query.getType())
                .page(query.toMpPage());
        //封装对象
        PageDto<CouponPageVO> page = new PageDto<>();
        page.setTotal(coupons.getTotal());
        page.setCurrent(coupons.getCurrent());
        page.setRecords(BeanUtils.copyList(coupons.getRecords(), CouponPageVO.class));
        return page;
    }

    /*
     *发放优惠卷
     * */
    @Override
    public void issueCoupon(Long id, CouponIssueFormDTO dto) {
        if (id==null) {
            throw new BizIllegalException("优惠卷id不能为空");
        }
        Coupon coupon = this.lambdaQuery()
                .eq(Coupon::getId, id)
                .one();
        if(coupon==null){
            throw new BizIllegalException("优惠卷不存在");
        }
        // 3.判断是否是立刻发放
        LocalDateTime issueBeginTime = dto.getIssueBeginTime();
        LocalDateTime now = LocalDateTime.now();
        boolean isBegin = issueBeginTime == null || !issueBeginTime.isAfter(now);
        // 4.更新优惠券
        // 4.1.拷贝属性到PO
        Coupon c = BeanUtils.copyBean(dto, Coupon.class);
        // 4.2.更新状态
        if (isBegin) {
            c.setStatus(CouponStatus.ISSUING);
            c.setIssueBeginTime(now);
        }else{
            c.setStatus(CouponStatus.UN_ISSUE);
        }
        // 4.3.写入数据库
        updateById(c);
        // TODO 兑换码生成
    }
}
