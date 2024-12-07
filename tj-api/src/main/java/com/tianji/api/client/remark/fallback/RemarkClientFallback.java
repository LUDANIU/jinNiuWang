package com.tianji.api.client.remark.fallback;

import com.tianji.api.client.remark.RemarkClient;
import com.tianji.common.utils.CollUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;

import java.util.List;
import java.util.Set;

/**
 * @author 鲁昊天
 * @date 2024/12/7
 */
@Slf4j
public class RemarkClientFallback implements FallbackFactory<RemarkClient> {
    @Override
    public RemarkClient create(Throwable cause) {
        log.error("Remark服务降级");
        return new RemarkClient() {
            @Override
            public Set<Long> isBizLiked(List<Long> bizIds) {
                return CollUtils.emptySet();
            }
        };
    }
}
