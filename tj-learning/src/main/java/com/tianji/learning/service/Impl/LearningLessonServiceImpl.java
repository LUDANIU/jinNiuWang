package com.tianji.learning.service.Impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianji.api.client.course.CatalogueClient;
import com.tianji.api.client.course.CourseClient;
import com.tianji.api.dto.course.CataSimpleInfoDTO;
import com.tianji.api.dto.course.CourseFullInfoDTO;
import com.tianji.api.dto.course.CourseSimpleInfoDTO;
import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.domain.query.PageQuery;
import com.tianji.common.exceptions.BadRequestException;
import com.tianji.common.exceptions.BizIllegalException;
import com.tianji.common.utils.BeanUtils;
import com.tianji.common.utils.CollUtils;
import com.tianji.common.utils.DateUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.domain.dto.LearningPlanDTO;
import com.tianji.learning.domain.po.LearningLesson;
import com.tianji.learning.domain.po.LearningRecord;
import com.tianji.learning.domain.vo.LearningLessonVO;
import com.tianji.learning.domain.vo.LearningPlanPageVO;
import com.tianji.learning.domain.vo.LearningPlanVO;
import com.tianji.learning.enums.LessonStatus;
import com.tianji.learning.enums.PlanStatus;
import com.tianji.learning.mapper.LearningLessonMapper;
import com.tianji.learning.mapper.LearningRecordMapper;
import com.tianji.learning.service.ILearningLessonService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 * 学生课表 服务实现类
 * </p>
 *
 * @author 鲁大牛
 * @since 2024-10-17
 */
@Service
@RequiredArgsConstructor
public class LearningLessonServiceImpl extends ServiceImpl<LearningLessonMapper, LearningLesson> implements ILearningLessonService {
    private final CourseClient courseClient;
    private final CatalogueClient catalogueClient;
    private final LearningRecordMapper learningRecordMapper;

    /*
     * 添加课程到课表
     * */
    @Override
    public void addUserLessons(Long userId, List<Long> courseIds) {
        List<CourseSimpleInfoDTO> cInfos = courseClient.getSimpleInfoList(courseIds);
        //判断课程是否存在
        if (cInfos == null || cInfos.size() == 0) {
            // 课程不存在，无法添加
            log.error("课程信息不存在，无法添加到课表");
            return;
        }
        //创建课程集合
        List<LearningLesson> lessons = new ArrayList<>();
        //批量查询课程信息
        for (CourseSimpleInfoDTO cInfo : cInfos) {
            LearningLesson lesson = new LearningLesson();
            // 获取过期时间
            Integer validDuration = cInfo.getValidDuration();
            if (validDuration != null && validDuration > 0) {
                LocalDateTime now = LocalDateTime.now();
                lesson.setExpireTime(now.plusMonths(validDuration));
            }
            lesson.setCourseId(cInfo.getId());
            lesson.setUserId(userId);
            lessons.add(lesson);
        }
        //添加入课程表
        this.saveBatch(lessons);
    }

    /*
     * 课表分页查询
     * */
    @Override
    public PageDTO<LearningLessonVO> queryMyLessons(PageQuery pageQuery) {
        //获取用户id
        Long userId = UserContext.getUser();
        if (userId == null) {
            return null;
        }
        Page<LearningLesson> page = this.lambdaQuery()
                .eq(LearningLesson::getUserId, userId)
                .page(pageQuery.toMpPage("latest_learn_time", pageQuery.getIsAsc()));
        //判断用户是否购买了课程
        if (page.getRecords().size() == 0) {
            return PageDTO.empty(page);
        }
        //创建vo集合
        List<LearningLessonVO> list = new ArrayList<>();
        //查询课程详细信息
        Map<Long, CourseSimpleInfoDTO> courseMap = this.queryCourseSimpleInfoList(page.getRecords());
        //查询课程封面等信息
        for (LearningLesson record : page.getRecords()) {
            LearningLessonVO learningLessonVO = BeanUtils.copyBean(record, LearningLessonVO.class);
            CourseSimpleInfoDTO courseSimpleInfoDTO = courseMap.get(record.getCourseId());
            //补充封面等信息
            learningLessonVO.setSections(courseSimpleInfoDTO.getSectionNum());
            learningLessonVO.setCourseName(courseSimpleInfoDTO.getName());
            learningLessonVO.setCourseCoverUrl(courseSimpleInfoDTO.getCoverUrl());
            list.add(learningLessonVO);

        }
        return PageDTO.of(page, list);
    }

    /*
     *查询最近正在学习的课程
     * */
    @Override
    public LearningLessonVO queryMyCurrentLesson() {
        //获取用户id
        Long userId = UserContext.getUser();
        //查询一条最近的课表信息
        LearningLesson lesson = this.lambdaQuery()
                .eq(LearningLesson::getUserId, userId)
                .orderByDesc(LearningLesson::getLatestLearnTime)
                .last("limit 1")
                .one();
        //判断用户是否拥有课程
        if (lesson == null) {
            return null;
        }
        //转成vo类型
        LearningLessonVO learningLessonVO = BeanUtil.copyProperties(lesson, LearningLessonVO.class);
        //查询课程信息
        CourseFullInfoDTO courseFullInfoDTO = courseClient.getCourseInfoById(lesson.getCourseId(), false, false);
        //判断该课程是否存在
        if (courseFullInfoDTO == null) {
            throw new BadRequestException("课程不存在");
        }
        //统计用户的课程数量
        Integer count = this.lambdaQuery()
                .eq(LearningLesson::getUserId, userId)
                .count();
        learningLessonVO.setCourseName(courseFullInfoDTO.getName());
        learningLessonVO.setCourseCoverUrl(courseFullInfoDTO.getCoverUrl());
        learningLessonVO.setSections(courseFullInfoDTO.getSectionNum());
        learningLessonVO.setCourseAmount(count);
        //查询小结信息
        List<CataSimpleInfoDTO> cataSimpleInfoDTOS = catalogueClient.batchQueryCatalogue(CollUtils.singletonList(lesson.getLatestSectionId()));
        if (!CollUtils.isEmpty(cataSimpleInfoDTOS)) {
            CataSimpleInfoDTO cataInfo = cataSimpleInfoDTOS.get(0);
            learningLessonVO.setLatestSectionName(cataInfo.getName());
            learningLessonVO.setLatestSectionIndex(cataInfo.getCIndex());
        }
        return learningLessonVO;
    }

    /*
     * 从课表删除课程
     * */
    @Override
    public void deleteCourseFromLesson(Long userId, Long courseId) {
        //判断是否携带了userId
        if (userId == null) {
            userId = UserContext.getUser();
        }
        this.lambdaUpdate()
                .eq(LearningLesson::getUserId, userId)
                .eq(LearningLesson::getCourseId, courseId)
                .remove();
    }

    /*
     *
     * 校验用户学习课程的权限
     * */
    @Override
    public Long isLessonValid(Long courseId) {
        Long userId = UserContext.getUser();
        LearningLesson lesson = this.lambdaQuery()
                .eq(LearningLesson::getUserId, userId)
                .eq(LearningLesson::getCourseId, courseId)
                .one();
        if (lesson == null) {
            return null;
        }
        if (lesson.getExpireTime() != null && LocalDateTime.now().isAfter(lesson.getExpireTime())) {
            return null;
        }
        return lesson.getId();
    }

    /*
     * 查询课程信息
     * */
    @Override
    public LearningLessonVO queryLessonByCourseId(Long courseId) {
        Long userId = UserContext.getUser();
        LearningLesson lesson = this.lambdaQuery()
                .eq(LearningLesson::getUserId, userId)
                .eq(LearningLesson::getCourseId, courseId)
                .one();
        if (lesson == null) {
            throw new BadRequestException("还没有该课程");
        }
        return BeanUtils.copyBean(lesson, LearningLessonVO.class);
    }

    /*
     * 查询课程的学习人数
     * */
    @Override
    public Integer countLearningLessonByCourse(Long courseId) {
        return this.lambdaQuery()
                .eq(LearningLesson::getCourseId, courseId)
                .count();
    }

    /**
     * 制定学习计划
     *
     * @param learningPlanDTO
     */
    @Override
    public void createLearningPlan(LearningPlanDTO learningPlanDTO) {
        //获取用户id
        Long userId = UserContext.getUser();
        //查询课表信息
        LearningLesson lesson = this.lambdaQuery()
                .eq(LearningLesson::getUserId, userId)
                .eq(LearningLesson::getCourseId, learningPlanDTO.getCourseId())
                .one();
        if (lesson == null) {
            throw new BizIllegalException("还没有该课程");
        }
        //更新
        this.lambdaUpdate()
                .set(LearningLesson::getPlanStatus, PlanStatus.PLAN_RUNNING.getValue())
                .set(LearningLesson::getWeekFreq, learningPlanDTO.getFreq())
                .eq(LearningLesson::getId, lesson.getId())
                .update();
    }

    /**
     * 查看学习计划进度
     *
     * @param pageQuery
     * @return
     */
    @Override
    public LearningPlanPageVO queryLearningPlanPage(PageQuery pageQuery) {
        LearningPlanPageVO learningPlanPageVO = new LearningPlanPageVO();
        //1,获取用户id
        Long userId = UserContext.getUser();
        /*
         * 2,TODO 查询本周学习积分
         * */
        /*
         * 3,查询本周要学习的小结总数
         * */
        LambdaQueryWrapper<LearningLesson> wrapperToLearn = new LambdaQueryWrapper<>();
        wrapperToLearn.eq(LearningLesson::getUserId, userId);
        wrapperToLearn.in(LearningLesson::getStatus, LessonStatus.NOT_BEGIN, LessonStatus.LEARNING);
        wrapperToLearn.eq(LearningLesson::getPlanStatus, PlanStatus.PLAN_RUNNING);
        wrapperToLearn.select(LearningLesson::getWeekFreq);
        Map<String, Object> map = this.getMap(wrapperToLearn);
        //获取到本周要学的小结总数
        Integer allWeekFreq = 0;
        if (map.isEmpty()) {
            allWeekFreq = Integer.valueOf(map.get("week_freq").toString());
        }
        /*
         * 4,查询本周已学习的小结数量
         * */
        //4.1获取最近一周的时间节点
        LocalDateTime begin = DateUtils.getWeekBeginTime(LocalDate.now());
        LocalDateTime end = DateUtils.getWeekEndTime(LocalDate.now());
        //4.2查询已学习的小结数量
        LambdaQueryWrapper<LearningRecord> wrapperLearned = new LambdaQueryWrapper<>();
        wrapperLearned.eq(LearningRecord::getUserId, userId);
        wrapperLearned.gt(LearningRecord::getFinishTime, begin);
        wrapperLearned.lt(LearningRecord::getFinishTime, end);
        wrapperLearned.eq(LearningRecord::getFinished, true);
        Integer learnedSections = learningRecordMapper.selectCount(wrapperLearned);
        /*
         * 5,查询该课程本周已学习的小结数量
         * */
        QueryWrapper<LearningRecord> wrapperLearnedOfLesson = new QueryWrapper<>();
        wrapperLearnedOfLesson.select("lesson_id as lessonId", "COUNT(*) as userId");
        wrapperLearnedOfLesson.eq("user_id", userId);
        wrapperLearnedOfLesson.eq("finished", true);
        wrapperLearnedOfLesson.gt("finish_time", begin);
        wrapperLearnedOfLesson.lt("finish_time", end);
        wrapperLearnedOfLesson.groupBy("lesson_id");
        List<LearningRecord> learningRecords = learningRecordMapper.selectList(wrapperLearnedOfLesson);
        Map<Long, Long> learnedSectionsMap = learningRecords.stream()
                .collect(Collectors.toMap(LearningRecord::getLessonId, r -> r.getUserId()));
        /*
         * 6.分页查询课表
         * */
        Page<LearningLesson> page = this.lambdaQuery()
                .eq(LearningLesson::getUserId, userId)
                .ne(LearningLesson::getStatus, LessonStatus.FINISHED)
                .eq(LearningLesson::getPlanStatus, PlanStatus.PLAN_RUNNING)
                .lt(LearningLesson::getExpireTime, LocalDateTime.now())
                .page(pageQuery.toMpPage("latest_learn_time", false));
        if (page.getRecords().isEmpty()) {
            learningPlanPageVO.isEmpty();
        }
        /*
         *7,查询该课程的课程名和课程总章节数
         */
        Map<Long, CourseSimpleInfoDTO> courseInfoMap = this.queryCourseSimpleInfoList(page.getRecords());
        /*
         * 8,封装集合数据
         * */
        List<LearningPlanVO> learningPlanVOS = BeanUtils.copyList(page.getRecords(), LearningPlanVO.class);
        for (LearningPlanVO learningPlanVO : learningPlanVOS) {
            //补充课程信息
            CourseSimpleInfoDTO courseSimpleInfoDTO = courseInfoMap.get(learningPlanVO.getCourseId());
            learningPlanVO.setCourseName(courseSimpleInfoDTO.getName());
            learningPlanVO.setSections(courseSimpleInfoDTO.getSectionNum());
            //补充课程本周已学习章节数
            Long num = learnedSectionsMap.get(learningPlanVO.getId());
            learningPlanVO.setWeekLearnedSections(num == null ? 0 : num.intValue());
        }
        /*
         *9.封装返回数据
         * */
        learningPlanPageVO.setWeekTotalPlan(allWeekFreq);
        learningPlanPageVO.setWeekFinished(learnedSections);
        return learningPlanPageVO;
    }

    private Map<Long, CourseSimpleInfoDTO> queryCourseSimpleInfoList(List<LearningLesson> records) {
        Set<Long> courseIds = records.stream().map(LearningLesson::getCourseId).collect(Collectors.toSet());
        List<CourseSimpleInfoDTO> simpleInfoList = courseClient.getSimpleInfoList(courseIds);
        if (simpleInfoList.isEmpty()) {
            throw new BadRequestException("课程信息不存在！");
        }
        return simpleInfoList.stream().collect(Collectors.toMap(CourseSimpleInfoDTO::getId, c -> c));
    }

}
