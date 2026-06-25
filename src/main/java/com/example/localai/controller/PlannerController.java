package com.example.localai.controller;

import com.example.localai.common.Result;
import com.example.localai.dto.PlannerRequest;
import com.example.localai.dto.PlannerResponse;
import com.example.localai.service.PlannerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/planner")
@RequiredArgsConstructor
public class PlannerController {

    private final PlannerService plannerService;

    @PostMapping
    public Result<PlannerResponse> plan(@Valid @RequestBody PlannerRequest request) {
        return Result.success(new PlannerResponse(plannerService.plan(request.getQuestion())));
    }
}
