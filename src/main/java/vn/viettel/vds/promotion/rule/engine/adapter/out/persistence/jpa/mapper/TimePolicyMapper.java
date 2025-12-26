package vn.viettel.vds.promotion.rule.engine.adapter.out.persistence.jpa.mapper;

import org.springframework.stereotype.Component;
import vn.viettel.vds.promotion.rule.engine.adapter.out.persistence.jpa.entity.TimePolicyEntity;
import vn.viettel.vds.promotion.rule.engine.adapter.out.persistence.jpa.entity.TimeWindowEntity;
import vn.viettel.vds.promotion.rule.engine.domain.model.TimePolicy;

import java.time.DayOfWeek;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class TimePolicyMapper {

    public TimePolicy toDomain(TimePolicyEntity entity) {
        if (entity == null) {
            return null;
        }

        List<TimePolicy.TimeWindow> windows = entity.getTimeWindows().stream()
                .map(this::toWindowDomain)
                .toList();

        return TimePolicy.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .active(entity.isActive())
                .timeWindows(windows)
                .build();
    }

    private TimePolicy.TimeWindow toWindowDomain(TimeWindowEntity entity) {
        Set<DayOfWeek> daysOfWeek = parseDaysOfWeek(entity.getDaysOfWeek());

        return TimePolicy.TimeWindow.builder()
                .daysOfWeek(daysOfWeek)
                .startTime(entity.getStartTime())
                .endTime(entity.getEndTime())
                .spansMidnight(entity.isSpansMidnight())
                .build();
    }

    private Set<DayOfWeek> parseDaysOfWeek(String daysOfWeekStr) {
        if (daysOfWeekStr == null || daysOfWeekStr.trim().isEmpty()) {
            return new HashSet<>();
        }

        return Arrays.stream(daysOfWeekStr.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(DayOfWeek::valueOf)
                .collect(Collectors.toSet());
    }
}
