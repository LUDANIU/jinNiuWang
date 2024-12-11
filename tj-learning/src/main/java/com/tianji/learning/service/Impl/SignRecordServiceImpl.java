package com.tianji.learning.service.Impl;

import com.tianji.common.autoconfigure.mq.RabbitMqHelper;
import com.tianji.common.constants.MqConstants;
import com.tianji.common.utils.CollUtils;
import com.tianji.common.utils.DateUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.constants.RedisConstants;
import com.tianji.learning.domain.vo.SignResultVO;
import com.tianji.learning.mq.message.SignInMessage;
import com.tianji.learning.service.ISignRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.BitFieldSubCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * @author 鲁昊天
 * @date 2024/12/9
 */
@Service
@RequiredArgsConstructor
public class SignRecordServiceImpl implements ISignRecordService {

    private final StringRedisTemplate redisTemplate;
    private final RabbitMqHelper mqHelper;

    @Override
    public SignResultVO sign() {
        //获取登录用户
        Long user = UserContext.getUser();
        //拼接用户签到的key
        LocalDate now = LocalDate.now();
        String key = RedisConstants.SIGN_RECORD_KEY_PREFIX
                + user + now.format(DateUtils.SIGN_DATE_SUFFIX_FORMATTER);
        /*
         * 添加签到记录
         * */
        Boolean wasSign = redisTemplate.opsForValue().setBit(
                key,
                LocalDate.now().getDayOfMonth() - 1,
                true
        );
        if (Boolean.TRUE.equals(wasSign)) {
            //如果签到成功，则返回签到结果
            throw new RuntimeException("重复签到");
        }
        /*
         * 计算连续签到天数
         * */
        Integer count = this.countSignDays(key, LocalDate.now().getDayOfMonth());
        SignResultVO result = new SignResultVO();
        result.setSignDays(count);
        result.setRewardPoints(0);
        /*
         * 根据连续签到天数进行奖励
         * */
        switch (count) {
            case 7:
                result.setRewardPoints(10);
            case 14:
                result.setRewardPoints(20);
            case 28:
                result.setRewardPoints(40);
        }
        //发送消息，签到积分
        mqHelper.send(MqConstants.Exchange.LEARNING_EXCHANGE,
                MqConstants.Key.SIGN_IN,
                SignInMessage.of(user, result.getRewardPoints()+1));
        return result;
    }

    @Override
    public Byte[] getMonthSignRecords() {
        Byte[] bytes = new Byte[LocalDate.now().getDayOfMonth()];
        //获取登录用户
        Long user = UserContext.getUser();
        //拼接用户签到的key
        LocalDate now = LocalDate.now();
        String key = RedisConstants.SIGN_RECORD_KEY_PREFIX + user + now.format(DateUtils.SIGN_DATE_SUFFIX_FORMATTER);
        //查询签到记录
        List<Long> list = redisTemplate.opsForValue()
                .bitField(key, BitFieldSubCommands.create()
                        .get(BitFieldSubCommands.BitFieldType.unsigned(LocalDate.now().getDayOfMonth())).valueAt(0));
        if (CollUtils.isEmpty(list)) {
            return new Byte[0];
        }
        int sign = list.get(0).intValue();
        int dayNumber = LocalDate.now().getDayOfMonth();
        while (dayNumber > 0) {
            if ((sign & 1) == 1) {
                bytes[dayNumber - 1] = 1;
            } else {
                bytes[dayNumber - 1] = 0;
            }
            sign >>>= 1;
            dayNumber--;
        }
        return bytes;
    }

    /*
     * 计算连续签到天数
     * */
    private Integer countSignDays(String key, int dayOfMonth) {
        //计数器
        Integer count = 0;
        List<Long> list = redisTemplate.opsForValue()
                .bitField(key,
                        BitFieldSubCommands.create()
                                .get(BitFieldSubCommands.BitFieldType.unsigned(dayOfMonth)).valueAt(0)
                );
        if (list == null) {
            return 0;
        }
        int sign = list.get(0).intValue();
        while ((sign & 1) == 1) {
            count++;
            sign >>>= 1;
        }
        return count;
    }
}
