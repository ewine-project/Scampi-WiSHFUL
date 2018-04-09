package de.tu_berlin.tkn.ewine.monitor;

import fi.tkk.netlab.dtn.scampi.core.monitor.impl.tcpjsonmonitor.Event;
import fi.tkk.netlab.dtn.scampi.core.monitor.impl.tcpjsonmonitor.LinkUpEvent;

/**
 * Link up event that includes the SSID.
 *
 * @author teemuk
 */
public final class ExtendedLinkUpEvent
extends Event {
    public static final String NAME = "extendedLinkUpEvent";

    public final LinkUpEvent linkUpEvent;
    public final String ssid;

    public ExtendedLinkUpEvent(
            final LinkUpEvent linkUpEvent,
            final String ssid ) {
        super( NAME );
        this.linkUpEvent = linkUpEvent;
        this.ssid = ssid;
    }
}
