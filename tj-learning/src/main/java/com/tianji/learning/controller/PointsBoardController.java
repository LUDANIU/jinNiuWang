package com.tianji.learning.controller;


import com.tianji.learning.domain.query.PointsBoardQuery;
import com.tianji.learning.domain.vo.PointsBoardVO;
import com.tianji.learning.service.IPointsBoardSeasonService;
import com.tianji.learning.service.IPointsBoardService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * 学霸天梯榜 前端控制器
 * </p>
 *
 * @author ldn
 * @since 2024-12-09
 */
@Api(tags = "积分排行榜相关接口")
@RestController
@RequestMapping("/boards")
@RequiredArgsConstructor
public class PointsBoardController {
    private final IPointsBoardService pointsBoardService;

    @ApiOperation("查询赛季排行榜")
    @GetMapping
    PointsBoardVO queryPointsBoard(PointsBoardQuery query) {
        return pointsBoardService.queryPointsBoard(query);
    }
}
