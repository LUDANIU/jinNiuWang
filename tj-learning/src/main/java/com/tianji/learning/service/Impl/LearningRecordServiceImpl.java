package com.tianji.learning.service.Impl;

import com.tianji.api.client.course.CourseClient;
import com.tianji.api.dto.course.CourseFullInfoDTO;
import com.tianji.api.dto.leanring.LearningLessonDTO;
import com.tianji.api.dto.leanring.LearningRecordDTO;
import com.tianji.common.exceptions.BizIllegalException;
import com.tianji.common.utils.BeanUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.domain.dto.LearningRecordFormDTO;
import com.tianji.learning.domain.po.LearningLesson;
import com.tianji.learning.domain.po.LearningRecord;
import com.tianji.learning.enums.LessonStatus;
import com.tianji.learning.enums.SectionType;
import com.tianji.learning.mapper.LearningRecordMapper;
import com.tianji.learning.service.ILearningLessonService;
import com.tianji.learning.service.ILearningRecordService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.learning.utils.LearningRecordDelayTaskHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * <p>
 * 学习记录表 服务实现类
 * </p>
 *
 * @author 鲁小牛
 * @since 2024-11-06
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LearningRecordServiceImpl extends ServiceImpl<LearningRecordMapper, LearningRecord> implements ILearningRecordService {
    private final ILearningLessonService lessonService;
    private final CourseClient courseClient;
    private final LearningRecordDelayTaskHandler learningRecordDelayTaskHandler;

    /**
     * 根据courseId查询学习记录
     */
    @Override
    public LearningLessonDTO queryLearningRecordByCourse(Long courseId) {
        //查看当前用户id
        Long userId = UserContext.getUser();
        //查看课表id
        LearningLesson lesson = lessonService.lambdaQuery()
                .eq(LearningLesson::getCourseId, courseId)
                .eq(LearningLesson::getUserId, userId)
                .one();
        Long lessonId = lesson.getId();
        if (lessonId == null) {
            throw new BizIllegalException("该课程不在课表中");
        }
        //根据课表id查询课程学习记录
        List<LearningRecord> list = this.lambdaQuery()
                .eq(LearningRecord::getLessonId, lessonId)
                .list();
        List<LearningRecordDTO> dtoList = BeanUtils.copyList(list, LearningRecordDTO.class);
        //封装结果返回
        LearningLessonDTO dto = new LearningLessonDTO();
        dto.setId(lessonId);
        dto.setRecords(dtoList);
        dto.setLatestSectionId(lesson.getLatestSectionId());
        return dto;
    }

    /**
     * 提交学习记录
     *
     * @param formDTO
     */
    @Override
    public void addLearningRecord(LearningRecordFormDTO formDTO) {
        //获取用户id
        Long userId = UserContext.getUser();
        //表示是否第一次学完
        Boolean isFirstFinish = null;
        //判断是考试还是视频
        if (formDTO.getSectionType() == SectionType.EXAM) {
            isFirstFinish = this.changeExamRecord(formDTO, userId);
        } else if (formDTO.getSectionType() == SectionType.VIDEO) {
            isFirstFinish = this.changeLessonRecord(formDTO, userId);
        } else {
            throw new BizIllegalException("小节类型不正确");
        }
        this.changeLesson(formDTO, isFirstFinish);
    }

    /**
     * 修改课表
     */
    private void changeLesson(LearningRecordFormDTO formDTO, Boolean isFirstFinish) {
        //根据lessonId查询课表信息
        LearningLesson lesson = lessonService.getById(formDTO.getLessonId());
        if (lesson == null) {
            throw new BizIllegalException("您没有该课程");
        }
        //allFinish表示是否完成了该课程的全部学习
        Boolean allFinish = false;
        //判断是否学完全部课程
        if (isFirstFinish) {
            CourseFullInfoDTO courseInfo = courseClient.getCourseInfoById(lesson.getCourseId(), false, false);
            if (courseInfo == null) {
                throw new BizIllegalException("课程不存在");
            }
            allFinish = lesson.getLearnedSections() + 1 >= courseInfo.getSectionNum();
        }
        //更新课表
        lessonService.lambdaUpdate()
                .set(lesson.getLearnedSections() == 0, LearningLesson::getStatus, LessonStatus.LEARNING.getValue())
                .set(allFinish, LearningLesson::getStatus, LessonStatus.FINISHED.getValue())
                .set(isFirstFinish, LearningLesson::getLearnedSections, lesson.getLearnedSections() + 1)
                .set(LearningLesson::getLatestSectionId, formDTO.getSectionId())
                .set(LearningLesson::getLatestLearnTime, formDTO.getCommitTime())
                .eq(LearningLesson::getId, lesson.getId())
                .update();
    }

    /**
     * 修改学习记录
     */
    private Boolean changeLessonRecord(LearningRecordFormDTO formDTO, Long userId) {
        //判断是否是第一次学习这章
        LearningRecord record = this.lambdaQuery()
                .eq(LearningRecord::getLessonId, formDTO.getLessonId())
                .eq(LearningRecord::getSectionId, formDTO.getSectionId())
                .one();
        //是第一次学习，新增学习记录
        if (record == null) {
            LearningRecord newRecord = BeanUtils.copyBean(formDTO, LearningRecord.class);
            newRecord.setUserId(userId);
            this.save(newRecord);
            return false;
        } //不是第一次学习
        else {
            //不是第一次学习。判断是否是第一次学完
            Boolean isFirstFinish = !record.getFinished() && formDTO.getMoment() >= formDTO.getDuration() * 0.95;
            /*
             * 在缓存中查询学习记录
             * */
            if (!isFirstFinish) {
                //先更新缓存中的记录，20秒后判断是否停止学习
                LearningRecord learningRecord =
                        this.updateRecordInRedis(formDTO.getLessonId(), formDTO.getSectionId(), formDTO.getMoment());
                log.info("最新的学习记录:{}", learningRecord);
                learningRecordDelayTaskHandler.addLearningRecordTask(learningRecord);
                return false;
            }
            /*
             * 第一次学完，更新数据并删除缓存
             * */
            Boolean update = this.lambdaUpdate()
                    .set(isFirstFinish, LearningRecord::getFinished, isFirstFinish)
                    .set(isFirstFinish, LearningRecord::getFinishTime, LocalDateTime.now())
                    .set(LearningRecord::getMoment, formDTO.getMoment())
                    .eq(LearningRecord::getId, record.getId())
                    .update();
            if (!update) {
                throw new BizIllegalException("更新失败");
            }
            this.learningRecordDelayTaskHandler.cleanRecordCache(formDTO.getLessonId(), formDTO.getSectionId());
            return isFirstFinish;
        }
    }

    /**
     * 更新缓存中的学习记录，如果没有则增加一条缓存
     *
     * @param lessonId
     * @param sectionId
     * @return LearningRecord
     */
    private LearningRecord updateRecordInRedis(Long lessonId, Long sectionId, Integer moment) {
        LearningRecord learningRecord = this.learningRecordDelayTaskHandler
                .readRecordCache(lessonId, sectionId);
        if (learningRecord == null) {
            LearningRecord recordInMysql = this.lambdaQuery()
                    .eq(LearningRecord::getLessonId, lessonId)
                    .eq(LearningRecord::getSectionId, sectionId)
                    .one();
            recordInMysql.setMoment(moment);
            this.learningRecordDelayTaskHandler
                    .writeRecordCache(recordInMysql);
            return recordInMysql;
        }
        learningRecord.setMoment(moment);
        learningRecord.setLessonId(lessonId);
        learningRecord.setSectionId(sectionId);
        this.learningRecordDelayTaskHandler
                .writeRecordCache(learningRecord);
        return learningRecord;

    }

    /**
     * 修改考试记录
     */
    private Boolean changeExamRecord(LearningRecordFormDTO formDTO, Long userId) {
        LearningRecord record = BeanUtils.copyBean(formDTO, LearningRecord.class);
        record.setUserId(userId);
        record.setFinished(true);
        record.setFinishTime(LocalDateTime.now());
        Boolean isSave = this.save(record);
        return isSave;
    }

}
