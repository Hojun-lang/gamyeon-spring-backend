package com.gamyeon.intv.presentation.dto.response;

import com.gamyeon.intv.application.dto.result.IntvListInfo;
import java.util.List;

public record IntvListResponse(
    List<IntvListItemResponse> content, int page, int size, long totalElements, int totalPages) {

  public static IntvListResponse from(IntvListInfo info) {
    return new IntvListResponse(
        info.content().stream().map(IntvListItemResponse::from).toList(),
        info.page(),
        info.size(),
        info.totalElements(),
        info.totalPages());
  }
}
