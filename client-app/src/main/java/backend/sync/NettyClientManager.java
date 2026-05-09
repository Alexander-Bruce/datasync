package backend.sync;

import backend.sync.client.NettySyncClient;
import backend.util.SyncStyle;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
@Lazy
public class NettyClientManager implements InitializingBean {

  @Autowired public NettySyncClient client;

  public volatile BlockingQueue<SyncStyle> queue = new LinkedBlockingDeque<>();

  public boolean offer(List<SyncStyle> list) {

    for (SyncStyle f : list) {
      boolean success = queue.offer(f);
      if (!success) return false;
    }

    return true;
  }

  public void bgSync() {

    Thread syncFile =
        new Thread(
            () -> {
              try {
                while (true) {
                  SyncStyle f = queue.take();

                  List<SyncStyle> fileList = new LinkedList<>();
                  fileList.add(f);
                  queue.drainTo(fileList);

                  client.sync(fileList);
                }
              } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
              }
            });

    syncFile.start();
  }

  @Override
  public void afterPropertiesSet() throws Exception {
    bgSync();
  }
}
