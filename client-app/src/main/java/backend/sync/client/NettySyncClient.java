package backend.sync.client;

import backend.config.ClientConfigStore;
import backend.model.ClientConfig;
import backend.util.SyncStyle;
import dataSync.CDCManager;
import dataSync.FastCDC.FastCDCManager;
import dataSync.FlipCDC.FlipCDCManager;
import dataSync.QuickCDC.QuickCDCManager;
import dataSync.RabinCDC.RabinCDCManager;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.File;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.springframework.stereotype.Component;

@Component
public class NettySyncClient {

  private EventLoopGroup group;

  @PostConstruct
  public void init() {
    this.group = new NioEventLoopGroup();
  }

  @PreDestroy
  public void destroy() {
    if (group != null) group.shutdownGracefully();
  }

  public void sync(List<SyncStyle> fileList) {
    if (fileList == null || fileList.isEmpty()) return;

    // 1. 建立一次连接
    Bootstrap b = new Bootstrap();
    SyncClientHandler handler = new SyncClientHandler();

    b.group(group)
        .channel(NioSocketChannel.class)
        .handler(
            new ChannelInitializer<SocketChannel>() {
              @Override
              public void initChannel(SocketChannel ch) {
                ch.pipeline()
                    .addLast(
                        new ObjectDecoder(10 * 1024 * 1024, ClassResolvers.cacheDisabled(null)),
                        new ObjectEncoder(),
                        handler // 使用同一个 Handler 实例
                        );
              }
            });

    Channel channel = null;
    try {
      ClientConfig config = ClientConfigStore.requireConfigured();
      // 连接服务端
      ChannelFuture f = b.connect(config.getSyncHost(), config.getSyncPort()).sync();
      channel = f.channel();

      // 2. 循环处理文件，共用此 Channel
      for (SyncStyle style : fileList) {
        File file = style.file;
        if (!file.exists()) continue;

        // 工厂方法创建 CDC 实例
        CDCManager algorithm = getCDCAlgorithm(style.syncType);

        // 3. 调用 Handler 发送文件，并获得一个 Future
        CompletableFuture<Void> fileFuture =
            handler.syncFile(channel, file, algorithm, style.storagePath);

        // 4. 阻塞！直到当前这个文件传输显示 "SUCCESS"
        fileFuture.get();
      }

    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      // 5. 所有文件传完了，或者出错了，关闭连接
      if (channel != null) {
        channel.close();
      }
    }
  }

  private CDCManager getCDCAlgorithm(SyncStyle.SyncType type) {
    if (type == null) {
      return new FlipCDCManager();
    }

    switch (type) {
      case FastCDC:
        return new FastCDCManager();

      case FlipCDC:
        return new FlipCDCManager();

      case RabinCDC:
        return new RabinCDCManager();

      case QuickCDC:
        return new QuickCDCManager();

      default:
        return new FlipCDCManager();
    }
  }
}
