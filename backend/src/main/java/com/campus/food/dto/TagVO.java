package com.campus.food.dto;

import com.campus.food.entity.Tag;
import lombok.Data;

@Data
public class TagVO {

    private Long id;

    private String name;

    private String code;

    public static TagVO from(Tag tag) {
        TagVO vo = new TagVO();
        vo.setId(tag.getId());
        vo.setName(tag.getName());
        vo.setCode(tag.getCode());
        return vo;
    }
}