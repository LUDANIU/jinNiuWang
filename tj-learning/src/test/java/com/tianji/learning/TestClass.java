package com.tianji.learning;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianji.api.client.remark.RemarkClient;
import com.tianji.common.domain.query.PageQuery;
import com.tianji.learning.domain.po.InteractionQuestion;
import com.tianji.learning.domain.po.LearningLesson;
import com.tianji.learning.enums.LessonStatus;
import com.tianji.learning.enums.PlanStatus;
import com.tianji.learning.service.IInteractionQuestionService;
import com.tianji.learning.service.ILearningLessonService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.LocalDate;
import java.util.Map;

/**
 * @author 鲁昊天
 * @date 2024/11/23
 */
@Slf4j
@SpringBootTest
public class TestClass {
    @Autowired
    private RemarkClient remarkClient;
    @Autowired
    private ILearningLessonService learningLessonService;
    @Autowired
    private IInteractionQuestionService questionService;
    @Qualifier("redisTemplate")
    @Autowired
    private RedisTemplate redisTemplate;

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
        Page<InteractionQuestion> page = questionService.lambdaQuery()
                .select(InteractionQuestion.class,
                        q -> !q.getProperty().equals("description"))
                .page(new Page<>(1, 5));
        System.out.println(page.getRecords().toString());
    }
    @Test
    public void test4() {
        System.out.println(remarkClient.isBizLiked(null));
    }
    @Test
    public void test5() {
        int dayOfMonth = LocalDate.now().getDayOfMonth();
        System.out.println("Current day of the month: " + dayOfMonth);

    }
}
