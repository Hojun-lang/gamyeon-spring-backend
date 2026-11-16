package com.gamyeon.intv.application.dto.result;

import java.util.List;

public record IntvListInfo(
    List<IntvListItemInfo> content, int page, int size, long totalElements, int totalPages) {}
