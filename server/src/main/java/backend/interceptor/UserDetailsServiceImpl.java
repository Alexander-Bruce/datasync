package backend.interceptor;

import backend.mapper.mysql.UserMapper;
import backend.model.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * 用户详情服务实现
 *
 * @field userMapper: 用户mapper
 * @function loadUserByUsername: 获取当前用户
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

  @Autowired private UserMapper userMapper;

  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

    return null;
  }

  public User loadUserById(Integer id) throws UsernameNotFoundException {
    User user;

    try {
      user = userMapper.selectById(id);
    } catch (Exception e) {
      throw new UsernameNotFoundException("user not found");
    }

    return user;
  }
}
