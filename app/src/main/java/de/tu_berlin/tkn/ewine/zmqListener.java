package de.tu_berlin.tkn.ewine;

import java.util.List;

public interface zmqListener {
    void zmqReceive(List<byte[]> msg);
    void zmqInitialized();
}
