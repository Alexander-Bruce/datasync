package backend.util;

/** 表示远端 bucket 里某个用户的一条 scope 入口，用于"刚安装本地无 task"场景下的远端列表恢复。 */
public class RemoteScope {

  public String alias;
  public String rootName;
  public boolean isDir;
  public String scopeName;

  public RemoteScope() {}

  public RemoteScope(String alias, String rootName, boolean isDir, String scopeName) {
    this.alias = alias;
    this.rootName = rootName;
    this.isDir = isDir;
    this.scopeName = scopeName;
  }

  public String getAlias() {
    return alias;
  }

  public void setAlias(String alias) {
    this.alias = alias;
  }

  public String getRootName() {
    return rootName;
  }

  public void setRootName(String rootName) {
    this.rootName = rootName;
  }

  public boolean isDir() {
    return isDir;
  }

  public void setDir(boolean dir) {
    isDir = dir;
  }

  public String getScopeName() {
    return scopeName;
  }

  public void setScopeName(String scopeName) {
    this.scopeName = scopeName;
  }
}
