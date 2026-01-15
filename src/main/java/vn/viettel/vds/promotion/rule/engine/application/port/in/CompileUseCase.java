package vn.viettel.vds.promotion.rule.engine.application.port.in;

import vn.viettel.vds.promotion.rule.engine.application.dto.CompileJobResponse;
import vn.viettel.vds.promotion.rule.engine.application.dto.CompileRequest;
import vn.viettel.vds.promotion.rule.engine.application.dto.CompileResponse;

import java.util.List;

public interface CompileUseCase {

    CompileResponse compile(CompileRequest request);

    List<CompileJobResponse> getCompileJobs(String ruleId, String status,
                                            String from, String to, int page, int size);

    CompileJobResponse getCompileJob(String jobId);
}