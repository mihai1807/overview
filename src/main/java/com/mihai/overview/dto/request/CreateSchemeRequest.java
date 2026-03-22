package com.mihai.overview.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CreateSchemeRequest {

    @NotEmpty
    @Size(max = 120)
    private String name;

    @NotNull
    private List<SchemeKpiInput> kpis;

    @NotNull
    private List<SchemeCriticalInput> criticals;

    @Getter @Setter
    public static class SchemeKpiInput {
        @NotNull
        private Long kpiId;
        private int orderIndex;
        private boolean required = true;
    }

    @Getter @Setter
    public static class SchemeCriticalInput {
        @NotNull
        private Long criticalId;
        private int orderIndex;
    }
}
