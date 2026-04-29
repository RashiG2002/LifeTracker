package com.lifetracker.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GuidedActionDTO {
    private String title;
    private String category;
    private String summary;
    private String icon;
    private String level;
    private String effort;
    private List<String> steps;
}