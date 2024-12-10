package com.tianji.learning.service;

import com.tianji.learning.domain.vo.SignResultVO;

/**
 * @author 鲁昊天
 * @date 2024/12/9
 */
public interface ISignRecordService {
    /*
     * 用户登录
     * */
    SignResultVO sign();

    /*
     *查询本月签到记录
     * */
    Byte[] getMonthSignRecords();
}
