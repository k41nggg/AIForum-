package com.zhidao.demo.dto;

import lombok.Data;

@Data
public class UserSummaryVO {
    private Long id;
    private String nickname;
    private String avatar;
    private String bio;
}
