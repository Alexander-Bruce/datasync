package backend.mapper.mysql;

import backend.model.entity.User;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper {

  User selectById(int id);

  User selectByEmail(String email);

  List<User> searchByQuery(@org.apache.ibatis.annotations.Param("q") String q);

  void insert(User user);

  void update(User user);

  void delete(int id);
}
