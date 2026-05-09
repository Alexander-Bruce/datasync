package backend.sync.server;

import backend.sync.utils.Encryptor;
import backend.sync.utils.StartSyncRequest;
import backend.sync.utils.SyncPacket;
import dataSync.Block;
import dataSync.CDCManager;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;

public class SyncServerHandler extends SimpleChannelInboundHandler<Object> {

  private static final Set<String> ALLOWED_CDC_CLASSES =
      Set.of(
          "dataSync.FastCDC.FastCDCManager",
          "dataSync.FlipCDC.FlipCDCManager",
          "dataSync.QuickCDC.QuickCDCManager",
          "dataSync.RabinCDC.RabinCDCManager");

  private final String basePath;
  private File targetFile;
  private File tempFile;
  private BufferedOutputStream bos;
  private RandomAccessFile oldFileRaf;
  private Map<String, BlockInfo> localManifest;
  private byte[] AESKey;

  public SyncServerHandler(String basePath) {
    this.basePath = basePath;
  }

  @Override
  protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
    if (msg instanceof StartSyncRequest) {
      handleHandshake(ctx, (StartSyncRequest) msg);
    } else if (msg instanceof SyncPacket) {
      handleSyncPacket(ctx, (SyncPacket) msg);
    }
  }

  private void handleHandshake(ChannelHandlerContext ctx, StartSyncRequest req) throws Exception {

    // 1. 确保之前的资源已清理
    resetState();

    AESKey = Encryptor.decryptAESBytes(req.EncryptedAESKey);

    // 2. 准备路径和文件
    String subPath = req.storagePath != null ? req.storagePath : "";
    File dir = new File(basePath, subPath);
    if (!dir.exists()) dir.mkdirs();

    this.targetFile = new File(dir, req.fileName);
    this.tempFile = new File(dir, req.fileName + ".part");
    this.bos = new BufferedOutputStream(new FileOutputStream(tempFile));
    this.localManifest = new HashMap<>();

    // 3. 反射加载 CDC（仅允许白名单中的类）
    CDCManager cdcManager;
    if (!ALLOWED_CDC_CLASSES.contains(req.cdcClassName)) {
      ctx.close();
      return;
    }
    try {
      Class<?> clazz = Class.forName(req.cdcClassName);
      cdcManager = (CDCManager) clazz.getDeclaredConstructor().newInstance();
    } catch (Exception e) {
      ctx.close();
      return;
    }

    // 4. 计算旧文件指纹
    if (targetFile.exists()) {
      cdcManager.splitChunks(targetFile.getAbsolutePath());
      List<Block> blocks = cdcManager.getChunks();
      for (Block b : blocks) {
        localManifest.put(b.getHashCode(), new BlockInfo(b.getOffset(), b.getChunkSize()));
      }
      this.oldFileRaf = new RandomAccessFile(targetFile, "r");
    }

    // 5. 回复 Hash 清单
    ctx.writeAndFlush(new HashSet<>(localManifest.keySet()));
  }

  private void handleSyncPacket(ChannelHandlerContext ctx, SyncPacket packet) throws IOException {
    if (packet.type == SyncPacket.Type.EOF) {
      finalizeCurrentFile(ctx);
      return;
    }

    if (packet.type == SyncPacket.Type.REFERENCE) {
      BlockInfo info = localManifest.get(packet.hash);
      if (info != null) {
        byte[] buffer = new byte[info.length];
        oldFileRaf.seek(info.offset);
        oldFileRaf.readFully(buffer);
        bos.write(buffer);
      }
    } else if (packet.type == SyncPacket.Type.DATA) {
      bos.write(packet.getData(AESKey));
    }
  }

  private void finalizeCurrentFile(ChannelHandlerContext ctx) throws IOException {
    // 关闭流
    if (bos != null) bos.close();
    if (oldFileRaf != null) oldFileRaf.close();

    // 原子替换
    Files.move(
        tempFile.toPath(),
        targetFile.toPath(),
        StandardCopyOption.REPLACE_EXISTING,
        StandardCopyOption.ATOMIC_MOVE);

    ctx.writeAndFlush("SUCCESS");

    resetState();
  }

  private void resetState() {
    try {
      if (bos != null) bos.close();
      if (oldFileRaf != null) oldFileRaf.close();
    } catch (IOException e) {
      /* ignore */
    }

    this.targetFile = null;
    this.tempFile = null;
    this.bos = null;
    this.oldFileRaf = null;
    this.localManifest = null;
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    resetState();
    ctx.close();
  }

  private static class BlockInfo {
    long offset;
    int length;

    BlockInfo(long offset, int length) {
      this.offset = offset;
      this.length = length;
    }
  }
}
