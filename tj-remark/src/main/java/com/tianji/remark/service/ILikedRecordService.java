package com.tianji.remark.service;

import com.tianji.remark.domain.dto.LikeRecordFormDTO;
import com.tianji.remark.domain.po.LikedRecord;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Set;

/**
 * <p>
 * 点赞记录表 服务类
 * </p>
 *
 * @author ldn
 * @since 2024-12-05
 */
public interface ILikedRecordService extends IService<LikedRecord> {

    /**
     * 点赞或取消点赞
     * @param recordDTO
     */
    void likeOrCancel(LikeRecordFormDTO recordDTO);

    /**
     * 批量查询已经点赞的业务id
     * @param bizIds
     * @return
     */
    Set<Long> isBizLiked(List<Long> bizIds);

    void handleLikedTimes(String bizType, int maxBizSize);
}
