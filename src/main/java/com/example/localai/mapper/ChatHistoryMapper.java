package com.example.localai.mapper;

import com.example.localai.model.ChatMessage;
import com.example.localai.dto.ChatSessionResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ChatHistoryMapper {

    void insert(@Param("sessionId") String sessionId, @Param("message") ChatMessage message);

    List<ChatMessage> findRecentMessages(@Param("sessionId") String sessionId, @Param("limit") int limit);

    void deleteBySessionId(@Param("sessionId") String sessionId);

    List<ChatSessionResponse> findSessions();
}
