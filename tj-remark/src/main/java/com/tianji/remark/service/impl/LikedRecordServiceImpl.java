package com.tianji.remark.service.impl;

import com.tianji.common.autoconfigure.mq.RabbitMqHelper;
import com.tianji.common.constants.MqConstants;
import com.tianji.common.utils.CollUtils;
import com.tianji.common.utils.StringUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.remark.constants.RedisConstants;
import com.tianji.remark.domain.dto.LikeRecordFormDTO;
import com.tianji.remark.domain.dto.LikedTimesDTO;
import com.tianji.remark.domain.po.LikedRecord;
import com.tianji.remark.mapper.LikedRecordMapper;
import com.tianji.remark.service.ILikedRecordService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * <p>
 * 点赞记录表 服务实现类
 * </p>
 *
 * @author ldn
 * @since 2024-12-05
 */
@Service
@RequiredArgsConstructor
public class LikedRecordServiceImpl extends ServiceImpl<LikedRecordMapper, LikedRecord> implements ILikedRecordService {
    private final RabbitMqHelper rabbitMqHelper;
    private final StringRedisTemplate redisTemplate;

    /**
     * 点赞或取消赞
     */
    @Override
    public void likeOrCancel(LikeRecordFormDTO recordDTO) {
        //获取userid
        Long userId = UserContext.getUser();
        //判断是否点赞或取消赞成功
        boolean success = recordDTO.getLiked() ?
                like(userId, recordDTO) : cancelLike(userId, recordDTO);
        //计算点赞次数
        /*Integer count = this.lambdaQuery()
                .eq(LikedRecord::getBizId, recordDTO.getBizId())
                .eq(LikedRecord::getBizType, recordDTO.getBizType())
                .count();*/
        //构造消息对象
        /*LikedTimesDTO msg=LikedTimesDTO.builder()
                .bizId(recordDTO.getBizId())
                .likedTimes(count)
                .build();*/
        // 2.判断是否执行成功，如果失败，则直接结束
        if (!success) {
            return;
        }
        // 3.如果执行成功，统计点赞总数
        Long likedTimes = redisTemplate.opsForSet()
                .size(RedisConstants.LIKE_BIZ_KEY_PREFIX + recordDTO.getBizId());
        if (likedTimes == null) {
            return;
        }
        // 4.缓存点总数到Redis
        redisTemplate.opsForZSet().add(
                RedisConstants.LIKE_COUNT_KEY_PREFIX + recordDTO.getBizType(),
                recordDTO.getBizId().toString(),
                likedTimes
        );

    }

    /**
     *
     */
    @Override
    public Set<Long> isBizLiked(List<Long> bizIds) {
       /* if (CollUtils.isEmpty(bizIds)) {
            return new HashSet<>();
        }
        List<LikedRecord> reocord = this.lambdaQuery()
                .in(LikedRecord::getBizId, bizIds)
                .eq(LikedRecord::getUserId, UserContext.getUser())
                .list();
        return reocord.stream().map(
                LikedRecord::getBizId
        ).collect(Collectors.toSet());*/
        Set<Long> set = new HashSet<>();
        redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            for (Long bizId : bizIds) {
                Boolean isMember = redisTemplate.opsForSet().isMember(RedisConstants.LIKE_BIZ_KEY_PREFIX + bizId,
                        UserContext.getUser().toString());
                if (isMember) {
                    set.add(bizId);
                }
            }
            return null;
        });
        return set;
/*        Long userId = UserContext.getUser();
        List<Object> objects = redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            StringRedisConnection src = (StringRedisConnection) connection;
            for (Long bizId : bizIds) {
                String key = RedisConstants.LIKE_BIZ_KEY_PREFIX + bizId;
                src.sIsMember(key, userId.toString());
            }
            return null;
        });
        return IntStream.range(0, objects.size())
                .filter(i -> (boolean) objects.get(i))
                .mapToObj(bizIds::get)
                .collect(Collectors.toSet());*/
    }

    /*
     *定时发送更新点赞数的消息队列
     * */
    @Override
    public void handleLikedTimes(String bizType, int maxBizSize) {
        List<LikedTimesDTO> list = new ArrayList<>();
        Set<ZSetOperations.TypedTuple<String>> typedTuples = redisTemplate.opsForZSet()
                .popMax(RedisConstants.LIKE_COUNT_KEY_PREFIX + bizType, maxBizSize);
        if (typedTuples == null) {
            return;
        }
        for (ZSetOperations.TypedTuple<String> tuple : typedTuples) {
            if (tuple == null || tuple.getValue() == null || tuple.getScore() == null || tuple.getValue().isEmpty()) {
                continue;
            }
            list.add(LikedTimesDTO.builder()
                    .likedTimes(tuple.getScore().intValue())
                    .bizId(Long.valueOf(tuple.getValue()))
                    .build());
        }
        if (CollUtils.isEmpty(list)) {
            return;
        }
        String routingKey = StringUtils.format(MqConstants.Key.LIKED_TIMES_KEY_TEMPLATE, bizType);
        rabbitMqHelper.send(MqConstants.Exchange.LIKE_RECORD_EXCHANGE, routingKey, list);
    }

    private boolean cancelLike(Long userId, LikeRecordFormDTO recordDTO) {
        /*return this.remove(new QueryWrapper<LikedRecord>().lambda()
                .eq(LikedRecord::getUserId, UserContext.getUser())
                .eq(LikedRecord::getBizId, recordDTO.getBizId()));*/
        //改进的点赞记录删除方法
        Long remove = redisTemplate.opsForSet()
                .remove(RedisConstants.LIKE_BIZ_KEY_PREFIX + recordDTO.getBizId(), userId);
        return remove != null && remove > 0;
    }

    private boolean like(Long userId, LikeRecordFormDTO recordDTO) {
        /*
         *查询是否有点赞记录
         * */
       /* LikedRecord record = this.getOne(new LambdaQueryWrapper<LikedRecord>()
                .eq(LikedRecord::getUserId, userId)
                .eq(LikedRecord::getBizId, recordDTO.getBizId()));
        if(record!=null){
            return false;
        }*/
        /*
         * 新增点赞记录
         * */
        /*LikedRecord newRecord = LikedRecord.builder()
                .userId(userId)
                .bizId(recordDTO.getBizId())
                .bizType(recordDTO.getBizType())
                .build();
        return this.save(newRecord);*/
        //改进的新增点赞记录
        String key = RedisConstants.LIKE_BIZ_KEY_PREFIX + recordDTO.getBizId();
        Long add = redisTemplate.opsForSet().add(key, String.valueOf(userId));
        return add != null && add > 0;
    }
}
