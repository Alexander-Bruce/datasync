package backend.service;

import backend.util.SyncStyle;
import java.io.InputStream;
import java.util.List;

public interface FileService {

  List<SyncStyle> compare(String email, String path, List<SyncStyle> fileList);

  /**
   * 下行同步第一步：返回服务端该范围下所有文件的相对路径列表（不含内容）。
   *
   * @param scopeName 同步范围名称
   * @return 相对路径列表
   */
  List<String> listDownloadFiles(String scopeName);

  /**
   * 下行同步第二步：返回单个文件的原始字节内容。
   *
   * @param scopeName 同步范围名称
   * @param relativePath 相对于 scopeDir 的路径
   * @return 文件字节数组
   */
  byte[] downloadFile(String scopeName, String relativePath);

  long uploadFile(String storagePath, String fileName, InputStream inputStream);
}
