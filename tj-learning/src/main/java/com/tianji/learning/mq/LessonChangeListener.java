package com.tianji.learning.mq;

import com.tianji.api.dto.trade.OrderBasicDTO;
import com.tianji.common.constants.MqConstants;
import com.tianji.learning.service.ILearningLessonService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class LessonChangeListener {
    final ILearningLessonService iLearningLessonService;

    @RabbitListener(
            bindings = @QueueBinding(value = @Queue(value = "learning.lesson.pay.queue", durable = "true"),
                    exchange = @Exchange(name = MqConstants.Exchange.ORDER_EXCHANGE, type = "topic"),
                    key = MqConstants.Key.ORDER_PAY_KEY
            )
    )
    public void listenLessonPay(OrderBasicDTO order) {

        if (order == null || order.getOrderId() == null || order.getCourseIds().size() == 0 || order.getCourseIds() == null) {
            log.error("课程报名失败，信息不全");
        }
        // 2.添加课程
        log.debug("监听到用户{}的订单{}，需要添加课程{}到课表中", order.getUserId(), order.getOrderId(), order.getCourseIds());
        iLearningLessonService.addUserLessons(order.getUserId(), order.getCourseIds());
    }
}
