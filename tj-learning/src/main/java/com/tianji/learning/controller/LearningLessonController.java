package com.tianji.learning.controller;


import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.domain.query.PageQuery;
import com.tianji.learning.domain.dto.LearningPlanDTO;
import com.tianji.learning.domain.vo.LearningLessonVO;
import com.tianji.learning.domain.vo.LearningPlanPageVO;
import com.tianji.learning.service.ILearningLessonService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * <p>
 * 学生课表 前端控制器
 * </p>
 *
 * @author 鲁大牛
 * @since 2024-10-17
 */
@RestController
@Api(tags = "课表相关接口")
@RequestMapping("/lessons")
public class LearningLessonController {
    @Resource
    private ILearningLessonService lessonService;

    @ApiOperation("查询我的课表，排序字段 latest_learn_time:学习时间排序，create_time:购买时间排序")
    @GetMapping("/page")
    public PageDTO<LearningLessonVO> queryMyLessons(PageQuery pageQuery) {
        return lessonService.queryMyLessons(pageQuery);
    }

    @ApiOperation("查询最近在学习的课程")
    @GetMapping("/now")
    public LearningLessonVO queryNowLearningLesson() {
        return lessonService.queryMyCurrentLesson();
    }

    @DeleteMapping("/{courseId}")
    @ApiOperation("删除指定课程信息")
    public void deleteCourseFromLesson(
            @ApiParam(value = "课程id", example = "1") @PathVariable("courseId") Long courseId) {
        lessonService.deleteCourseFromLesson(null, courseId);
    }

    /**
     * 校验当前用户是否可以学习当前课程
     *
     * @param courseId 课程id
     * @return lessonId，如果是报名了则返回lessonId，否则返回空
     */
    @GetMapping("/{courseId}/valid")
    @ApiOperation("校验该用户是否可以学习课程")
    Long isLessonValid(@PathVariable("courseId") Long courseId) {
        return lessonService.isLessonValid(courseId);
    }

    @GetMapping("{courseId}")
    @ApiOperation("查询用户课表中指定课程状态")
    LearningLessonVO queryLessonByCourseId(@ApiParam @PathVariable("courseId") Long courseId) {
        return lessonService.queryLessonByCourseId(courseId);
    }

    @GetMapping("/{courseId}/count")
    @ApiOperation("查看课程的学习人数")
    Integer countLearningLessonByCourse(@PathVariable("courseId") Long courseId) {
        return lessonService.countLearningLessonByCourse(courseId);
    }

    @PostMapping("plans")
    @ApiOperation("制定学习计划")
    void createLearningPlan(@RequestBody @Validated LearningPlanDTO learningPlanDTO) {
        lessonService.createLearningPlan(learningPlanDTO);
    }
    @GetMapping("plans")
    @ApiOperation("查询学习计划进度表")
    LearningPlanPageVO queryLearningPlanPage(PageQuery pageQuery) {
        return lessonService.queryLearningPlanPage(pageQuery);
    }
}
