package com.tianji.learning.service;

import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.domain.query.PageQuery;
import com.tianji.learning.domain.dto.LearningPlanDTO;
import com.tianji.learning.domain.po.LearningLesson;
import com.baomidou.mybatisplus.extension.service.IService;
import com.tianji.learning.domain.vo.LearningLessonVO;
import com.tianji.learning.domain.vo.LearningPlanPageVO;

import java.util.List;

/**
 * <p>
 * 学生课表 服务类
 * </p>
 *
 * @author 鲁大牛
 * @since 2024-10-17
 */
public interface ILearningLessonService extends IService<LearningLesson> {
    /*
     * 添加课程到课表
     * */
    void addUserLessons(Long userId, List<Long> courseIds);

    /*
     *分页查询课表
     * */
    PageDTO<LearningLessonVO> queryMyLessons(PageQuery pageQuery);

    /*
     * 查看最近学习的课程
     * */
    LearningLessonVO queryMyCurrentLesson();

    /*
     * 从课表删除课程
     * */
    void deleteCourseFromLesson(Long userId, Long courseId);

    /*
     * 校验用户是否能学习该课程
     * */
    Long isLessonValid(Long courseId);

    /*
     * 查询用户课表中指定课程状态
     * */
    LearningLessonVO queryLessonByCourseId(Long courseId);

    /**
     *查询课程学习人数
     * */
    Integer countLearningLessonByCourse(Long courseId);

    /**
     * 制定学习计划
     * @param learningPlanDTO
     */
    void createLearningPlan(LearningPlanDTO learningPlanDTO);

    /**
     * 查看学习计划进度
     * @param pageQuery
     * @return
     */
    LearningPlanPageVO queryLearningPlanPage(PageQuery pageQuery);
}
