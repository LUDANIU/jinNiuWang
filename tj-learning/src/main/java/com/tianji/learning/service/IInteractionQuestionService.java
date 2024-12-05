package com.tianji.learning.service;

import com.tianji.common.domain.dto.PageDTO;
import com.tianji.learning.domain.dto.QuestionFormDTO;
import com.tianji.learning.domain.po.InteractionQuestion;
import com.baomidou.mybatisplus.extension.service.IService;
import com.tianji.learning.domain.query.QuestionPageQuery;
import com.tianji.learning.domain.vo.QuestionVO;

/**
 * <p>
 * 互动提问的问题表 服务类
 * </p>
 *
 * @author ldn
 * @since 2024-12-05
 */
public interface IInteractionQuestionService extends IService<InteractionQuestion> {
    /**
     * 保存问题
     * @param questionFormDTO
     */
    void saveQuestion(QuestionFormDTO questionFormDTO);

    /**
     * 更新问题
     * @param questionFormDTO
     */
    void updateQuestion(QuestionFormDTO questionFormDTO,Long id);

    /**
     * 分页查询问题
     * @param query
     * @return
     */
    PageDTO<QuestionVO> queryQuestionByPage(QuestionPageQuery query);
    /**
     *
     */
}
