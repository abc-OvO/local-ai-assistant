package com.example.localai.service;

import com.example.localai.model.QueryIntent;

public interface PlannerService {

    QueryIntent plan(String question);
}
