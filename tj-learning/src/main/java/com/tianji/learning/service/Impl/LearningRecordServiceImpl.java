package com.tianji.learning.service.Impl;

import com.tianji.api.dto.leanring.LearningLessonDTO;
import com.tianji.api.dto.leanring.LearningRecordDTO;
import com.tianji.common.exceptions.BizIllegalException;
import com.tianji.common.utils.BeanUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.domain.dto.LearningRecordFormDTO;
import com.tianji.learning.domain.po.LearningLesson;
import com.tianji.learning.domain.po.LearningRecord;
import com.tianji.learning.enums.SectionType;
import com.tianji.learning.mapper.LearningRecordMapper;
import com.tianji.learning.service.ILearningLessonService;
import com.tianji.learning.service.ILearningRecordService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

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
public class LearningRecordServiceImpl extends ServiceImpl<LearningRecordMapper, LearningRecord> implements ILearningRecordService {
    private final LearningRecordMapper learningRecordMapper;
    private final ILearningLessonService lessonService;

    /**
     * 根据courseId查询学习记录
     *
     * @param courseId
     * @return
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
        return;
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
        } else {
            //不是第一次学习。判断是否是第一次学完
            Boolean isFirstFinish =!record.getFinished()&& formDTO.getMoment()>=formDTO.getDuration() * 0.95;
            Boolean update = this.lambdaUpdate()
                    .set(isFirstFinish, LearningRecord::getFinished, isFirstFinish)
                    .set(isFirstFinish, LearningRecord::getFinishTime, LocalDateTime.now())
                    .set(LearningRecord::getMoment, formDTO.getMoment())
                    .eq(LearningRecord::getId, record.getId())
                    .update();
            if(!update){
                throw new BizIllegalException("更新失败");
            }
            return isFirstFinish;
        }
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
