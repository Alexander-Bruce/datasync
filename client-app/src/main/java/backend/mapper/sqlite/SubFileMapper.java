package backend.mapper.sqlite;

import backend.model.entity.SubFile;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SubFileMapper {

  void insert(SubFile subFile);

  void update(SubFile subFile);

  void deleteById(int id);

  void deleteByFileId(@Param("fileId") int fileId);

  List<SubFile> selectByFileId(int fileId);

  SubFile selectByPath(String path);

  List<SubFile> selectByParent(int parent);

  List<SubFile> selectByFileIdAndParentIsNon(int fileId);

  SubFile selectByParentLimit(int field);

  SubFile selectByFileIdAndPath(
      @Param("fileId") Integer fileId, @Param("relativePath") String relativePath);

  /** 批量更新某个 File 下所有 SubFile 的同步状态。 上行/下行同步完成后调用。 */
  boolean updateSyncByFileId(@Param("fileId") int fileId, @Param("isSync") boolean isSync);

  /** 仅将单条 SubFile 的 is_sync 置为 false。 供文件监视任务使用。 */
  boolean markAsStale(@Param("id") int id);
}
