package com.tianji.promotion;

import com.tianji.promotion.domain.po.Coupon;
import com.tianji.promotion.service.IExchangeCodeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * @author 鲁昊天
 * @date 2024/12/29
 */
@SpringBootTest
public class TestClass {
    @Autowired
    private IExchangeCodeService exchangeCodeService;
    @Test
    public void test(){
        System.out.println("调用异步线程池的方法");
        exchangeCodeService.asyncGenerateCode(new Coupon());
    }
}