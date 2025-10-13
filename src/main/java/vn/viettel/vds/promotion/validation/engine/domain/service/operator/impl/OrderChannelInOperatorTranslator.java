package vn.viettel.vds.promotion.validation.engine.domain.service.operator.impl;

import org.springframework.stereotype.Component;
import vn.viettel.vds.promotion.validation.engine.domain.service.operator.OperatorTranslator;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class OrderChannelInOperatorTranslator implements OperatorTranslator {

    @Override
    public String translate(String nodeId, Map<String, Object> params, String reasonCode) {
        Object channelsParam = params.get("channels");
        if (channelsParam == null) {
            throw new IllegalArgumentException("Missing required parameter 'channels' for order.channel.in operator");
        }

        List<String> channels;
        if (channelsParam instanceof List) {
            @SuppressWarnings("unchecked")
            List<String> castedList = (List<String>) channelsParam;
            channels = castedList;
        } else if (channelsParam instanceof String str) {
            // Handle single channel as a list
            channels = List.of(str);
        } else {
            throw new IllegalArgumentException("Parameter 'channels' must be a List<String> or String for order.channel.in operator");
        }

        if (channels.isEmpty()) {
            throw new IllegalArgumentException("Parameter 'channels' cannot be empty for order.channel.in operator");
        }

        StringBuilder sb = new StringBuilder();

        if (channels.size() == 1) {
            // Single channel check
            sb.append("        $order: Order(channel == \"").append(channels.get(0)).append("\")\n");
        } else {
            // Multiple channels - check if order channel is in the target list
            String channelList = channels.stream()
                    .map(c -> "\"" + c + "\"")
                    .collect(Collectors.joining(", "));

            sb.append("        $order: Order(channel != null && eval(java.util.Arrays.asList(")
                    .append(channelList).append(").contains(channel)))\n");
        }

        return sb.toString();
    }

    @Override
    public String getOperatorName() {
        return "order.channel.in";
    }

    @Override
    public Integer getVersion() {
        return 1;
    }

    @Override
    public boolean supports(String operatorName, Integer version) {
        return "order.channel.in".equals(operatorName) &&
                (version == null || version.equals(getVersion()));
    }
}