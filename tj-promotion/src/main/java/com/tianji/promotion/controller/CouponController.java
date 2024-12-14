package com.tianji.promotion.controller;


import com.baomidou.mybatisplus.extension.plugins.pagination.PageDto;
import com.tianji.common.domain.query.PageQuery;
import com.tianji.promotion.domain.dto.CouponFormDTO;
import com.tianji.promotion.domain.query.CouponQuery;
import com.tianji.promotion.domain.vo.CouponPageVO;
import com.tianji.promotion.service.ICouponService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.jaxb.SpringDataJaxb;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * <p>
 * 优惠券的规则信息 前端控制器
 * </p>
 *
 * @author ldn
 * @since 2024-12-13
 */
@Api(tags = "优惠券管理接口")
@RestController
@RequestMapping("/coupons")
@RequiredArgsConstructor
public class CouponController {
    private final ICouponService couponService;

    @ApiOperation("新增优惠券接口")
    @PostMapping
    public void saveCoupon(@RequestBody @Valid CouponFormDTO dto) {
        couponService.saveCoupon(dto);
    }
    @ApiOperation("优惠券列表分页查询")
    @GetMapping("page")
    public PageDto<CouponPageVO> queryCouponPage(CouponQuery query) {
        return couponService.queryCouponByPage(query);
    }
}
