package com.tianji.learning;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tianji.api.client.course.CourseClient;
import com.tianji.api.dto.course.CourseFullInfoDTO;
import com.tianji.common.utils.BeanUtils;
import com.tianji.common.utils.DateUtils;
import com.tianji.learning.domain.po.LearningLesson;
import com.tianji.learning.domain.po.LearningRecord;
import com.tianji.learning.enums.LessonStatus;
import com.tianji.learning.enums.PlanStatus;
import com.tianji.learning.mapper.LearningRecordMapper;
import com.tianji.learning.service.ILearningLessonService;
import com.tianji.learning.service.Impl.LearningLessonServiceImpl;
import com.tianji.learning.service.Impl.LearningRecordServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
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

    @Autowired
    private LearningRecordMapper learningRecordMapper;
    @Test
    public void test() {
        LocalDateTime begin = DateUtils.getWeekBeginTime(LocalDate.now());
        LocalDateTime end = DateUtils.getWeekEndTime(LocalDate.now());
        LambdaQueryWrapper<LearningRecord> wrapperLearned = new LambdaQueryWrapper<>();
        wrapperLearned.eq(LearningRecord::getUserId, 2);
        wrapperLearned.ge(LearningRecord::getFinishTime, "2022-10-19 10:12:34");
        wrapperLearned.lt(LearningRecord::getFinishTime, end);
        wrapperLearned.eq(LearningRecord::getFinished, true);
        Integer num = learningRecordMapper.selectCount(wrapperLearned);
        System.out.println(num);
    }
}
