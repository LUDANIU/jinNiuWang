package com.tianji.learning;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianji.api.client.course.CourseClient;
import com.tianji.api.dto.course.CourseFullInfoDTO;
import com.tianji.common.domain.query.PageQuery;
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
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * @author 鲁昊天
 * @date 2024/11/23
 */
@Slf4j
@SpringBootTest
public class TestClass {
    @Autowired
    private  StringRedisTemplate redisTemplate;
    @Autowired
    private ILearningLessonService learningLessonService;
    @Autowired
    private LearningRecordMapper learningRecordMapper;
    @Test
    public void test() {
        QueryWrapper<LearningLesson> wrapperToLearn = new QueryWrapper<>();
        wrapperToLearn.select("sum(week_freq) as plansTotal");
        wrapperToLearn.eq("user_id", 2);
        wrapperToLearn.in("status", LessonStatus.NOT_BEGIN, LessonStatus.LEARNING);
        wrapperToLearn.eq("plan_status", PlanStatus.PLAN_RUNNING);
        Map<String, Object> map = learningLessonService.getMap(wrapperToLearn);
        System.out.println(map);
    }
    @Test
    public void test2() {
        PageQuery pageQuery = new PageQuery();
        Page<LearningLesson> page = learningLessonService.lambdaQuery()
                .eq(LearningLesson::getUserId, 2)
                .ne(LearningLesson::getStatus, LessonStatus.FINISHED)
                .eq(LearningLesson::getPlanStatus, PlanStatus.PLAN_RUNNING)
                .page(pageQuery.toMpPage("latest_learn_time", false));
        System.out.println(page.getRecords());
    }
    @Test
    public void test3() {
        String test1="test1";
        try{
            redisTemplate.opsForHash().put(test1, "1", "private final StringRedisTemplate redisTemplate;2323");
        }catch (Throwable e){
            log.error("缓存读取异常", e);
        }
        /*try{
            redisTemplate.opsForHash().put(test1, "1", test1);
        }catch (Throwable e){
            log.error("缓存读取异常", e);
        }*/
    }
}
