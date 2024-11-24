package com.tianji.learning;

import com.tianji.api.client.course.CourseClient;
import com.tianji.api.dto.course.CourseFullInfoDTO;
import com.tianji.common.utils.BeanUtils;
import com.tianji.learning.domain.po.LearningRecord;
import com.tianji.learning.service.Impl.LearningRecordServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;

/**
 * @author 鲁昊天
 * @date 2024/11/23
 */
@SpringBootTest
public class TestClass {
    @Autowired
    private CourseClient courseClient;
    @Test
    public void test(){

    }
}
