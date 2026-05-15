package backend.service;

import backend.util.RemoteScope;
import backend.util.SyncStyle;
import java.io.InputStream;
import java.util.List;

public interface FileService {

  List<SyncStyle> compare(
      String email, String path, String scopeName, boolean isDir, List<SyncStyle> fileList);

  /**
   * 列出某用户在 bucket 中已有的所有 scope（{@code email/alias/rootName}
   * 这一层）。供"刚装好客户端、本地数据库为空"的场景使用，让前端能从远端恢复出任务列表。
   *
   * @param email 用户邮箱
   * @return 该用户名下的所有 scope，每条携带 alias / rootName / isDir / scopeName
   */
  List<RemoteScope> listUserScopes(String email);

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

  boolean deleteScope(String scopeName);

  long uploadFile(String storagePath, String fileName, InputStream inputStream);
}
