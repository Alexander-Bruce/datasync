package backend.util;

import dataSync.CDCManager;
import java.io.File;

public class SyncStyle {

  public File file;

  public SyncType syncType;

  public String storagePath;

  public enum SyncType {
    FastCDC,
    FlipCDC,
    RabinCDC,
    QuickCDC
  }

  public SyncStyle() {}

  public SyncStyle(File file, CDCManager cdcManager, String storagePath) {
    this.file = file;

    String name = cdcManager.getClass().getName();

    if (name.startsWith("dataSync.FlipCDC")) this.syncType = SyncType.FlipCDC;
    else if (name.startsWith("dataSync.FastCDC")) this.syncType = SyncType.FastCDC;
    else if (name.startsWith("dataSync.RabinCDC")) this.syncType = SyncType.RabinCDC;
    else if (name.startsWith("dataSync.QuickCDC")) this.syncType = SyncType.QuickCDC;
    else this.syncType = SyncType.FlipCDC;

    this.storagePath = storagePath;
  }
}
