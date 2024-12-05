package com.tianji.learning.service.Impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianji.api.client.user.UserClient;
import com.tianji.api.dto.user.UserDTO;
import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.exceptions.BadRequestException;
import com.tianji.common.exceptions.BizIllegalException;
import com.tianji.common.utils.BeanUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.domain.dto.QuestionFormDTO;
import com.tianji.learning.domain.po.InteractionQuestion;
import com.tianji.learning.domain.po.InteractionReply;
import com.tianji.learning.domain.query.QuestionPageQuery;
import com.tianji.learning.domain.vo.QuestionVO;
import com.tianji.learning.mapper.InteractionQuestionMapper;
import com.tianji.learning.service.IInteractionQuestionService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.learning.service.IInteractionReplyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * <p>
 * 互动提问的问题表 服务实现类
 * </p>
 *
 * @author ldn
 * @since 2024-12-05
 */
@Service
@RequiredArgsConstructor
public class InteractionQuestionServiceImpl extends ServiceImpl<InteractionQuestionMapper, InteractionQuestion> implements IInteractionQuestionService {
    private final UserClient userClient;
    private final IInteractionReplyService replyService;

    /**
     * 创建问题
     */
    @Override
    public void saveQuestion(QuestionFormDTO questionFormDTO) {
        //获取用户id
        // 1.获取登录用户
        Long userId = UserContext.getUser();
        InteractionQuestion interactionQuestion = BeanUtils.copyBean(questionFormDTO, InteractionQuestion.class);
        interactionQuestion.setUserId(userId);
        this.save(interactionQuestion);
    }

    /**
     * 更新问题
     */
    @Override
    public void updateQuestion(QuestionFormDTO questionFormDTO, Long id) {
        InteractionQuestion question = this.getById(id);
        if (question == null) {
            throw new BizIllegalException("该问题不存在");
        }
        if (!Objects.equals(question.getUserId(), UserContext.getUser())) {
            throw new BizIllegalException("您没有权限修改该问题");
        }
        question.setAnonymity(questionFormDTO.getAnonymity());
        question.setDescription(questionFormDTO.getDescription());
        question.setTitle(questionFormDTO.getTitle());
        this.updateById(question);
    }

    /**
     * 分页查询问题
     */
    @Override
    public PageDTO<QuestionVO> queryQuestionByPage(QuestionPageQuery query) {
        //参数校验
        Long courseId = query.getCourseId();
        Long sectionId = query.getSectionId();
        if (courseId == null && sectionId == null) {
            throw new BadRequestException("课程id和小节id不能都为空");
        }
        //分页查询
        Page<InteractionQuestion> page =
                this.lambdaQuery()
                        .select(InteractionQuestion.class,
                                q -> !q.getProperty().equals("description"))
                        .eq(query.getOnlyMine(),
                                InteractionQuestion::getUserId,
                                UserContext.getUser())
                        .eq(query.getCourseId() != null,
                                InteractionQuestion::getCourseId,
                                query.getCourseId())
                        .eq(query.getSectionId() != null,
                                InteractionQuestion::getSectionId,
                                query.getSectionId())
                        .eq(InteractionQuestion::getHidden, false)
                        .page(query.toMpPageDefaultSortByCreateTimeDesc());
        List<InteractionQuestion> records = page.getRecords();
        //转化成vo对象
        List<QuestionVO> vos = records
                .stream()
                .map(q -> BeanUtils.copyBean(q, QuestionVO.class))
                .collect(Collectors.toList());
        //查询最新回答者信息
        Map<Long, QuestionVO> latestReplyUserMap =
                this.searchLatestReplyUser(records);
        //查询提问者信息
        Map<Long, UserDTO> userInfoMap =
                this.searchUserInfo(records);
        List<QuestionVO> newVos = vos.stream()
                .peek(
                        vo -> {
                            if (!vo.getAnonymity()) {
                                vo.setUserName(userInfoMap.get(vo.getId()).getName());
                                vo.setUserIcon(userInfoMap.get(vo.getId()).getIcon());
                            }
                            vo.setLatestReplyUser(latestReplyUserMap.get(vo.getId()).getLatestReplyUser());
                            vo.setLatestReplyContent(latestReplyUserMap.get(vo.getId()).getLatestReplyContent());
                        }
                )
                .collect(Collectors.toList());
        //填写返回信息
        PageDTO<QuestionVO> pageDTO = new PageDTO<>();
        pageDTO.setTotal(page.getTotal());
        pageDTO.setPages(page.getPages());
        pageDTO.setList(newVos);
        return pageDTO;
    }

    private Map<Long, UserDTO> searchUserInfo(List<InteractionQuestion> records) {
        Map<Long, UserDTO> map = new HashMap<>();
        for (InteractionQuestion q : records) {
            UserDTO user = userClient.queryUserById(q.getUserId());
            map.put(q.getId(), user);
        }
        return map;
    }

    private Map<Long, QuestionVO> searchLatestReplyUser(List<InteractionQuestion> records) {
        Map<Long, QuestionVO> map = new HashMap<>();
        for (InteractionQuestion q : records) {
            QuestionVO vo = new QuestionVO();
            InteractionReply reply = replyService.getById(q.getLatestAnswerId());
            vo.setLatestReplyContent(reply.getContent());
            if (!reply.getAnonymity()) {
                vo.setLatestReplyUser(userClient.queryUserById(reply.getUserId()).getName());
            }
            if (!reply.getHidden()) {
                map.put(q.getId(), vo);
            }
        }
        return map;
    }
}
