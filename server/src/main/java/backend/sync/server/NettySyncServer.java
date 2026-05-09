package backend.sync.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class NettySyncServer {

  @Value("${spring.netty.server.port}")
  private int port;

  @Value("${spring.netty.server.basePath}")
  private String basePath;

  private EventLoopGroup bossGroup;
  private EventLoopGroup workerGroup;

  public void start() {
    new Thread(
            () -> {
              bossGroup = new NioEventLoopGroup(1);
              workerGroup = new NioEventLoopGroup();

              try {
                ServerBootstrap b = new ServerBootstrap();
                b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(
                        new ChannelInitializer<SocketChannel>() {
                          @Override
                          public void initChannel(SocketChannel ch) {
                            ch.pipeline()
                                .addLast(
                                    new ObjectDecoder(
                                        10 * 1024 * 1024, ClassResolvers.weakCachingResolver(null)),
                                    new ObjectEncoder(),
                                    // 每次连接创建一个新的 Handler 实例
                                    new SyncServerHandler(basePath));
                          }
                        });

                ChannelFuture f = b.bind(port).sync();
                f.channel().closeFuture().sync(); // 阻塞等待关闭

              } catch (Exception e) {
                e.printStackTrace();
              } finally {
                stop();
              }
            })
        .start();
  }

  @PreDestroy
  public void stop() {
    if (bossGroup != null) bossGroup.shutdownGracefully();
    if (workerGroup != null) workerGroup.shutdownGracefully();
  }
}
