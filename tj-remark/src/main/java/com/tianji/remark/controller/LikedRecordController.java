package com.tianji.remark.controller;


import com.tianji.remark.domain.dto.LikeRecordFormDTO;
import com.tianji.remark.service.ILikedRecordService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Set;

/**
 * <p>
 * 点赞记录表 前端控制器
 * </p>
 *
 * @author ldn
 * @since 2024-12-05
 */
@RestController
@Api(tags = "点赞记录")
@RequiredArgsConstructor
@RequestMapping("/liked-record")
public class LikedRecordController {
    private final ILikedRecordService likedRecordService;

    @PostMapping
    @ApiOperation("点赞或取消赞")
    public void likeOrCancel(@Valid @RequestBody LikeRecordFormDTO recordDTO) {
        this.likedRecordService.likeOrCancel(recordDTO);
    }

    @GetMapping("list")
    @ApiOperation("查询指定业务id的点赞状态")
    public Set<Long> isBizLiked(@RequestParam("bizIds") List<Long> bizIds) {
        return likedRecordService.isBizLiked(bizIds);
    }
}
