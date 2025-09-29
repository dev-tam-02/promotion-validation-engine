package vn.viettel.vds.promotion.validation.engine.application.dto;

import vn.viettel.vds.promotion.validation.engine.domain.model.Decision;

import java.util.List;

public record ValidationResponse(
    List<Decision> decisions
) {}