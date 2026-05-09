package backend.mapper.sqlite;

import backend.model.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper {

  void insert(User user);

  User selectByEmail(String email);

  User selectById(int id);

  void deleteByEmail(User user);

  boolean update(User user);
}
