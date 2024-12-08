package com.tianji.remark.task;

import com.tianji.remark.service.ILikedRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author 鲁昊天
 * @date 2024/12/7
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class LikeTimesCheckTask {

    private static final List<String> BIZ_TYPES = List.of("QA", "NOTE");
    private static final int MAX_BIZ_SIZE = 30;
    private final ILikedRecordService recordService;


    @Scheduled(cron = "20 * * * * ?")
    public void checkLikedTimes(){
        log.info("springTask定时任务生效");
        for(String bizType:BIZ_TYPES){
            recordService.handleLikedTimes(bizType, MAX_BIZ_SIZE);
        }
    }
}
