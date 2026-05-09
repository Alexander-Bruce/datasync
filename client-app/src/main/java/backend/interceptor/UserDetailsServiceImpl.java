package backend.interceptor;

import backend.model.entity.User;
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

  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

    return null;
  }

  public User loadUserById(Integer id) throws UsernameNotFoundException {
    return null;
  }
}
