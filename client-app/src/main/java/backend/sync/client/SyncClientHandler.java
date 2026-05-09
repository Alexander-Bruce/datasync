package backend.sync.client;

import backend.sync.utils.Encryptor;
import backend.sync.utils.StartSyncRequest;
import backend.sync.utils.SyncPacket;
import dataSync.Block;
import dataSync.CDCManager;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import java.io.File;
import java.io.RandomAccessFile;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class SyncClientHandler extends SimpleChannelInboundHandler<Object> {
  private File currentFile;
  private CDCManager currentCdc;
  private CompletableFuture<Void> currentFuture;
  private byte[] AESKey;

  /**
   * 对外接口：开始同步一个新文件
   *
   * @return 用于等待完成的 Future
   */
  public CompletableFuture<Void> syncFile(
      Channel channel, File file, CDCManager cdc, String storagePath) {
    this.currentFile = file;
    this.currentCdc = cdc;
    this.currentFuture = new CompletableFuture<>();
    this.AESKey = Encryptor.genKey();

    // 发送握手包，启动流程
    channel.writeAndFlush(
        new StartSyncRequest(file.getName(), storagePath, cdc.getClass().getName(), AESKey));

    return currentFuture;
  }

  @Override
  protected void channelRead0(ChannelHandlerContext ctx, Object msg) {
    if (msg instanceof Set) {
      // CASE 1: 收到服务端 Hash 集合 -> 开始计算并发送数据
      @SuppressWarnings("unchecked")
      Set<String> serverHashes = (Set<String>) msg;

      // 使用 ctx.executor() 或者自定义线程池
      new Thread(() -> processTransfer(ctx, serverHashes)).start();

    } else if ("SUCCESS".equals(msg)) {
      // CASE 2: 服务端确认文件接收完毕
      if (currentFuture != null) {
        // 解锁主线程的 .get() 阻塞
        currentFuture.complete(null);
      }
    }
  }

  private void processTransfer(ChannelHandlerContext ctx, Set<String> serverHashes) {
    try (RandomAccessFile raf = new RandomAccessFile(currentFile, "r")) {
      // 1. 本地分块
      currentCdc.splitChunks(currentFile.getAbsolutePath());
      List<Block> chunks = currentCdc.getChunks();

      // 2. 发送块
      for (Block block : chunks) {
        if (serverHashes.contains(block.getHashCode())) {
          ctx.write(new SyncPacket(SyncPacket.Type.REFERENCE, block.getHashCode(), null, null));
        } else {
          raf.seek(block.getOffset());
          byte[] data = new byte[block.getChunkSize()];
          raf.readFully(data);
          ctx.write(new SyncPacket(SyncPacket.Type.DATA, null, data, AESKey));
        }
      }
      ctx.flush();

      // 3. 发送 EOF
      ctx.writeAndFlush(new SyncPacket(SyncPacket.Type.EOF, null, null, null));

    } catch (Exception e) {
      if (currentFuture != null) {
        currentFuture.completeExceptionally(e);
      }
    }
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    if (currentFuture != null) {
      currentFuture.completeExceptionally(cause);
    }
    ctx.close(); // 网络层异常，需要断开连接
  }
}
