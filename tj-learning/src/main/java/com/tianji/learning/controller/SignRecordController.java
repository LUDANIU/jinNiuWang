package com.tianji.learning.controller;

import com.tianji.learning.domain.vo.SignResultVO;
import com.tianji.learning.service.ISignRecordService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.Mapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author 鲁昊天
 * @date 2024/12/9
 */
@RestController
@RequiredArgsConstructor
@Api(tags = "签到接口")
public class SignRecordController {
    private final ISignRecordService signRecordService;
    /**
    * 用户签到
    * */
    @ApiOperation("用户签到")
    @PostMapping
    public SignResultVO sign() {
        return signRecordService.sign();
    }
    /**
     * 本月签到记录
     * */
    @ApiOperation("本月签到记录")
    @GetMapping("/sign-records")
    public Byte[] getMonthSignRecords() {
        return signRecordService.getMonthSignRecords();
    }
}
