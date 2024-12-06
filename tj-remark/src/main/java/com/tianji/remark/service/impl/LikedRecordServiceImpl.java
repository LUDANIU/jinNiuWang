package com.tianji.remark.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tianji.common.autoconfigure.mq.RabbitMqHelper;
import com.tianji.common.utils.StringUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.remark.domain.dto.LikeRecordFormDTO;
import com.tianji.remark.domain.dto.LikedTimesDTO;
import com.tianji.remark.domain.po.LikedRecord;
import com.tianji.remark.mapper.LikedRecordMapper;
import com.tianji.remark.service.ILikedRecordService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.tianji.common.constants.MqConstants.Exchange.LIKE_RECORD_EXCHANGE;
import static com.tianji.common.constants.MqConstants.Key.LIKED_TIMES_KEY_TEMPLATE;

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
    /**
     * 点赞或取消赞
     *
     */
    @Override
    public void likeOrCancel(LikeRecordFormDTO recordDTO) {
        //获取userid
        Long userId = UserContext.getUser();
        //判断是否点赞或取消赞成功
        boolean success = recordDTO.getLiked() ?
                like(userId, recordDTO) : cancelLike(userId, recordDTO);
        //计算点赞次数
        Integer count = this.lambdaQuery()
                .eq(LikedRecord::getBizId, recordDTO.getBizId())
                .eq(LikedRecord::getBizType, recordDTO.getBizType())
                .count();
        //构造消息对象
        LikedTimesDTO msg=LikedTimesDTO.builder()
                .bizId(recordDTO.getBizId())
                .likedTimes(count)
                .build();
        if (success) {
            //更新点赞数
            rabbitMqHelper.sendAsync(LIKE_RECORD_EXCHANGE,
                    StringUtils.format(LIKED_TIMES_KEY_TEMPLATE, recordDTO.getBizType()),
                    msg);
        }
    }

    /**
     *
     * @param bizIds
     * @return
     */
    @Override
    public Set<Long> isBizLiked(List<Long> bizIds) {
        List<LikedRecord> reocord=this.lambdaQuery()
                .in(LikedRecord::getBizId,bizIds)
                .eq(LikedRecord::getUserId,UserContext.getUser())
                .list();
        return reocord.stream().map(
                LikedRecord::getBizId
        ).collect(Collectors.toSet());
    }

    private boolean cancelLike(Long userId, LikeRecordFormDTO recordDTO) {
        return this.remove(new QueryWrapper<LikedRecord>().lambda()
                .eq(LikedRecord::getUserId, UserContext.getUser())
                .eq(LikedRecord::getBizId, recordDTO.getBizId()));
    }

    private boolean like(Long userId, LikeRecordFormDTO recordDTO) {
        /*
         *查询是否有点赞记录
         * */
        LikedRecord record = this.getOne(new LambdaQueryWrapper<LikedRecord>()
                .eq(LikedRecord::getUserId, userId)
                .eq(LikedRecord::getBizId, recordDTO.getBizId()));
        if(record!=null){
            return false;
        }
        /*
        * 新增点赞记录
        * */
        LikedRecord newRecord = LikedRecord.builder()
                .userId(userId)
                .bizId(recordDTO.getBizId())
                .bizType(recordDTO.getBizType())
                .build();
        return this.save(newRecord);
    }
}
