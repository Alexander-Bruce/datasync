package backend.service;

import backend.model.entity.File;
import backend.model.entity.SubFile;
import java.util.List;

public interface FileService {

  boolean upload(String path, String email);

  boolean upload(int fileId, String path, String email);

  List<File> getBriefFileList(String email);

  File updateFileTask(
      int fileId,
      String alias,
      String path,
      String email,
      String scheduled,
      String alg,
      String desc,
      String remoteHost,
      boolean isDir);

  Boolean deleteFileTask(String path, String email);

  Boolean deleteFileTask(int fileId, String path, String email);

  List<SubFile> getDetailedFileList(int fileId);

  boolean download(String path, String email);

  boolean download(int fileId, String path, String email);

  List<SubFile> getDetailedFileListByParent(int fileId);

  boolean deleteFile(String email, String path);

  /**
   * 扫描目录在磁盘上的实际内容，将 SubFile 表中缺失的条目补录进去（is_sync = false）。 不会删除或修改已有记录，仅做增量插入。 由文件监视任务在检测到目录结构变更时调用。
   *
   * @param fileId File 表中的主键
   */
  void refreshSubFiles(int fileId);

  boolean downloadScope(String scopeName, String localPath);
}
