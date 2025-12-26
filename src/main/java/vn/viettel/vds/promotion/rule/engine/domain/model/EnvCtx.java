package vn.viettel.vds.promotion.rule.engine.domain.model;

import java.time.ZoneId;

public class EnvCtx {
    private String channel;
    private Geo geo;
    private ZoneId tz;

    public EnvCtx() {
    }

    public EnvCtx(String channel, Geo geo, ZoneId tz) {
        this.channel = channel;
        this.geo = geo;
        this.tz = tz;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public Geo getGeo() {
        return geo;
    }

    public void setGeo(Geo geo) {
        this.geo = geo;
    }

    public ZoneId getTz() {
        return tz;
    }

    public void setTz(ZoneId tz) {
        this.tz = tz;
    }
}