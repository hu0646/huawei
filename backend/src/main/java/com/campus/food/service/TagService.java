package com.campus.food.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.campus.food.dto.TagVO;
import com.campus.food.entity.Tag;
import com.campus.food.mapper.TagMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TagService {

    private final TagMapper tagMapper;

    public List<TagVO> list() {
        return tagMapper.selectList(Wrappers.<Tag>lambdaQuery()
                        .eq(Tag::getStatus, 1)
                        .orderByAsc(Tag::getSortNo)
                        .orderByAsc(Tag::getId))
                .stream()
                .map(TagVO::from)
                .toList();
    }
}