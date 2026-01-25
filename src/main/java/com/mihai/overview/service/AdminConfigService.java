package com.mihai.overview.service;

import com.mihai.overview.request.CreateCriticalConditionPoolItemRequest;
import com.mihai.overview.request.CreateInteractionTypeRequest;
import com.mihai.overview.request.CreateKpiPoolItemRequest;
import com.mihai.overview.response.CriticalConditionPoolItemResponse;
import com.mihai.overview.response.InteractionTypeResponse;
import com.mihai.overview.response.KpiPoolItemResponse;

public interface AdminConfigService {

    InteractionTypeResponse createInteractionType(CreateInteractionTypeRequest request);

    KpiPoolItemResponse createKpiPoolItem(String interactionTypeCode, CreateKpiPoolItemRequest request);

    CriticalConditionPoolItemResponse createCriticalConditionPoolItem(String interactionTypeCode, CreateCriticalConditionPoolItemRequest request);

}