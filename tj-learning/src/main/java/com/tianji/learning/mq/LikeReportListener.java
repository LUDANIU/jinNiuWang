package com.tianji.learning.mq;

import com.tianji.common.domain.dto.LikedTimesDTO;
import com.tianji.learning.domain.po.InteractionReply;
import com.tianji.learning.service.IInteractionReplyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static com.tianji.common.constants.MqConstants.Exchange.LIKE_RECORD_EXCHANGE;
import static com.tianji.common.constants.MqConstants.Key.QA_LIKED_TIMES_KEY;
/**
 * @author 鲁昊天
 * @date 2024/12/6
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LikeReportListener {
    private final IInteractionReplyService replyService;
    @RabbitListener(
            bindings = @QueueBinding(
                    value = @Queue(value = "like.report.queue", durable = "true"),
                    exchange = @Exchange(name = LIKE_RECORD_EXCHANGE, type = ExchangeTypes.TOPIC),
                    key = QA_LIKED_TIMES_KEY
            )
    )
    public void LikedTimesListener(List<LikedTimesDTO> dtos){
        log.info("收到点赞数上报消息，{}", dtos);
        List<InteractionReply> replies=new ArrayList<>();
        for (LikedTimesDTO dto : dtos) {
            InteractionReply reply=InteractionReply.builder()
                    .likedTimes(dto.getLikedTimes())
                    .id(dto.getBizId())
                    .build();
            replies.add(reply);
        }
        replyService.updateBatchById(replies);
    }
}
