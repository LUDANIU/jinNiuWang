package com.tianji.learning.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tianji.common.utils.CollUtils;
import com.tianji.common.utils.DateUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.constants.RedisConstants;
import com.tianji.learning.domain.po.PointsRecord;
import com.tianji.learning.domain.vo.PointsStatisticsVO;
import com.tianji.learning.enums.PointsRecordType;
import com.tianji.learning.mapper.PointsRecordMapper;
import com.tianji.learning.mq.message.SignInMessage;
import com.tianji.learning.service.IPointsRecordService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * 学习积分记录，每个月底清零 服务实现类
 * </p>
 *
 * @author ldn
 * @since 2024-12-09
 */
@Service
@RequiredArgsConstructor
public class PointsRecordServiceImpl extends ServiceImpl<PointsRecordMapper, PointsRecord> implements IPointsRecordService {
private final StringRedisTemplate redisTemplate;
    @Override
    public void insertSignPoints(SignInMessage message) {
        PointsRecord record = new PointsRecord();
        record.setType(PointsRecordType.SIGN);
        record.setUserId(message.getUserId());
        record.setPoints(message.getPoints());
        this.save(record);
        // 4.更新总积分到Redis
        LocalDate now = LocalDate.now();
        Long userId = message.getUserId();
        String key = RedisConstants.POINTS_BOARD_KEY_PREFIX + now.format(DateUtils.POINTS_BOARD_SUFFIX_FORMATTER);
        redisTemplate.opsForZSet().incrementScore(key, userId.toString(), message.getPoints());
    }

    /*
     *查询今日积分情况
     * */
    @Override
    public List<PointsStatisticsVO> queryMyPointsToday() {
        //获取用户id
        Long userId = UserContext.getUser();
        //构造查询条件
        QueryWrapper<PointsRecord> wrapper
                = new QueryWrapper<>();
        LocalDateTime start= DateUtils.getDayStartTime(LocalDateTime.now());
        LocalDateTime end= DateUtils.getDayEndTime(LocalDateTime.now());
        wrapper.eq("user_id", userId);
        wrapper.between("create_time", start, end);
        wrapper.groupBy("type");
        wrapper.select("type","sum(points) as user_id");
        List<PointsRecord> list = this.list(wrapper);
        //分类把积分封装
        if(CollUtils.isEmpty(list)){
            return CollUtils.emptyList();
        }
        List<PointsStatisticsVO> vos=new ArrayList<>();
        for(PointsRecord p:list){
            PointsStatisticsVO vo=new PointsStatisticsVO();
            vo.setType(p.getType().getDesc());
            vo.setPoints(p.getUserId().intValue());
            vo.setMaxPoints(p.getType().getMaxPoints());
            vos.add(vo);
        }
        return vos;
    }
}
