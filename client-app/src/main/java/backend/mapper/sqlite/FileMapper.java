package backend.mapper.sqlite;

import backend.model.entity.File;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface FileMapper {

  boolean insert(File file);

  boolean update(File file);

  boolean deleteById(int id);

  List<File> selectById(int userId);

  File selectByFileId(int fileId);

  List<File> selectList(File file);

  File selectByPath(@Param("path") String path, @Param("userId") int userId);

  int countByAliasForUser(
      @Param("alias") String alias,
      @Param("userId") int userId,
      @Param("excludeId") Integer excludeId);

  /** 更新单条 File 记录的同步状态。 上行/下行同步完成后调用，同时刷新 update_time。 */
  boolean updateSyncById(@Param("id") int id, @Param("isSync") boolean isSync);

  /** 查询所有 File 记录（跨用户），供后台任务使用。 */
  List<File> selectAll();

  /** 仅将 is_sync 置为 false，不触碰 update_time。 供文件监视任务使用，以便下次扫描仍能感知变化。 */
  boolean markAsStale(@Param("id") int id);
}
