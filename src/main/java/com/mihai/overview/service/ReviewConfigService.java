package com.mihai.overview.service;

import com.mihai.overview.request.CreateCriticalConditionPoolItemRequest;
import com.mihai.overview.request.CreateInteractionTypeRequest;
import com.mihai.overview.request.CreateKpiPoolItemRequest;
import com.mihai.overview.response.CriticalConditionPoolItemResponse;
import com.mihai.overview.response.InteractionTypeResponse;
import com.mihai.overview.response.KpiPoolItemResponse;

import java.util.List;

public interface ReviewConfigService {

    InteractionTypeResponse createInteractionType(CreateInteractionTypeRequest request);

    KpiPoolItemResponse createKpiPoolItem(String interactionTypeCode, CreateKpiPoolItemRequest request);

    CriticalConditionPoolItemResponse createCriticalConditionPoolItem(String interactionTypeCode, CreateCriticalConditionPoolItemRequest request);

    List<InteractionTypeResponse> listInteractionTypes();

    List<KpiPoolItemResponse> listKpiPoolItems(String interactionTypeCode, boolean includeArchived);

    List<CriticalConditionPoolItemResponse> listCriticalConditionPoolItems(String interactionTypeCode, boolean includeArchived);

    void archiveKpiPoolItem(String interactionTypeCode, Long kpiId);
    void unarchiveKpiPoolItem(String interactionTypeCode, Long kpiId);

    void archiveCriticalPoolItem(String interactionTypeCode, Long criticalId);
    void unarchiveCriticalPoolItem(String interactionTypeCode, Long criticalId);


}