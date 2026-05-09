package dataSync;

import java.util.ArrayList;
import java.util.List;

public class CDCManager {

  protected static final int MIN_SIZE = 8 * 1024;
  protected static final int AVG_SIZE = 16 * 1024;
  protected static final int MAX_SIZE = 32 * 1024;
  protected long totalOffset = 0;

  protected final List<Block> chunks = new ArrayList<>(65536);

  public List<Block> getChunks() {
    return chunks;
  }

  public CDCManager() {}

  public void splitChunks(String filePath) {}
}
