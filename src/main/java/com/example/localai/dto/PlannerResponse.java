package com.example.localai.dto;

import com.example.localai.model.QueryIntent;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlannerResponse {

    private QueryIntent intent;
}
