package com.tianji.learning.controller;


import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.utils.StringUtils;
import com.tianji.learning.domain.dto.QuestionFormDTO;
import com.tianji.learning.domain.query.QuestionPageQuery;
import com.tianji.learning.domain.vo.QuestionVO;
import com.tianji.learning.service.IInteractionQuestionService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * <p>
 * 互动提问的问题表 前端控制器
 * </p>
 *
 * @author ldn
 * @since 2024-12-05
 */
@Api(tags = "互动提问的问题相关接口")
@RestController
@RequiredArgsConstructor
@RequestMapping("/questions")
public class InteractionQuestionController {
    private final IInteractionQuestionService questionService;

    @PostMapping
    @ApiOperation("创建互动提问")
    void saveQuestion(@RequestBody @Valid QuestionFormDTO questionFormDTO) {
        questionService.saveQuestion(questionFormDTO);
    }

    @PutMapping("{id}")
    @ApiOperation("修改互动提问")
    void updateQuestion(@PathVariable Long id, @RequestBody QuestionFormDTO questionFormDTO) {
        if(StringUtils.isBlank(questionFormDTO.getTitle())||
                StringUtils.isBlank(questionFormDTO.getDescription())||
                questionFormDTO.getAnonymity()==null
        ){
            throw new IllegalArgumentException("参数不能为空");
        }
        questionService.updateQuestion(questionFormDTO,id);
    }
    @ApiOperation("分页查询互动问题")
    @GetMapping("page")
   PageDTO<QuestionVO> queryQuestionPage(QuestionPageQuery query){
return questionService.queryQuestionByPage(query);
    }
}
