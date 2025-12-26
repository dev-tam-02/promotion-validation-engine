package vn.viettel.vds.promotion.rule.engine.application.dto;

import vn.viettel.vds.promotion.rule.engine.domain.model.Decision;

import java.util.List;

public record ValidationResponse(
        List<Decision> decisions
) {
}