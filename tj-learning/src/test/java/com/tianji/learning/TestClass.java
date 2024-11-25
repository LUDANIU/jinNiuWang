package com.tianji.learning;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tianji.api.client.course.CourseClient;
import com.tianji.api.dto.course.CourseFullInfoDTO;
import com.tianji.common.utils.BeanUtils;
import com.tianji.learning.domain.po.LearningLesson;
import com.tianji.learning.domain.po.LearningRecord;
import com.tianji.learning.enums.LessonStatus;
import com.tianji.learning.enums.PlanStatus;
import com.tianji.learning.service.ILearningLessonService;
import com.tianji.learning.service.Impl.LearningLessonServiceImpl;
import com.tianji.learning.service.Impl.LearningRecordServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * @author 鲁昊天
 * @date 2024/11/23
 */
@SpringBootTest
public class TestClass {
    @Autowired
    private ILearningLessonService learningLessonService;

    @Test
    public void test(){
        LambdaQueryWrapper<LearningLesson> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LearningLesson::getUserId, 2);
        wrapper.in(LearningLesson::getStatus, LessonStatus.NOT_BEGIN,LessonStatus.LEARNING);
        wrapper.eq(LearningLesson::getPlanStatus, PlanStatus.PLAN_RUNNING);
        wrapper.select(LearningLesson::getWeekFreq);
        Map<String, Object> map = learningLessonService.getMap(wrapper);
        System.out.println(map);
    }
}
