package backend.sync;

import backend.sync.server.NettySyncServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class NettyServerBootstrap {

  @Autowired private NettySyncServer syncServer;

  @EventListener(ApplicationReadyEvent.class)
  public void start() {
    syncServer.start();
  }
}
