package dataSync.RabinCDC;

import dataSync.Block;
import dataSync.CDCManager;
import java.io.*;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RabinCDCManager extends CDCManager {
  private static final long RABIN_PRIME_BASE = 31L;
  private static final long MASK = AVG_SIZE - 1;
  private static final long CUT_VALUE = 0;
  private String filePath = null;
  private static final int WINDOW_SIZE = 48;
  private long power;
  private BigInteger index = BigInteger.ZERO;

  public RabinCDCManager(String filePath) {
    File file = new File(filePath);
    if (!file.exists()) {
      throw new IllegalArgumentException("文件未找到: " + filePath);
    }
    this.filePath = filePath;

    this.power = 1L;
    for (int i = 0; i < WINDOW_SIZE - 1; i++) {
      this.power = this.power * RABIN_PRIME_BASE;
    }
  }

  public RabinCDCManager() {
    this.power = 1L;
    for (int i = 0; i < WINDOW_SIZE - 1; i++) {
      this.power = this.power * RABIN_PRIME_BASE;
    }
  }

  private long rollHash(long preHash, byte nextByte) {
    return (preHash * RABIN_PRIME_BASE) + (nextByte & 0xFF);
  }

  private void addChunk(byte[] buffer, int offset, int length) {
    byte[] chunkData = new byte[length];
    System.arraycopy(buffer, offset, chunkData, 0, length);
    chunks.add(new Block(length, index.add(BigInteger.ONE), totalOffset, chunkData));
    totalOffset += length;
  }

  @Override
  public void splitChunks(String filePath) {
    byte[] buffer = new byte[MAX_SIZE * 1024 / 2];
    int dataLeftOver = 0;
    long rollingHash = 0;

    try (BufferedInputStream bis =
        new BufferedInputStream(Files.newInputStream(Paths.get(filePath)))) {
      int bytesRead;
      while ((bytesRead = bis.read(buffer, dataLeftOver, buffer.length - dataLeftOver)) != -1) {
        int currentDataLimit = dataLeftOver + bytesRead;
        int chunkStartOffset = 0;

        for (int i = 0; i < currentDataLimit; i++) {
          int currentChunkSize = i - chunkStartOffset + 1;

          if (currentChunkSize < WINDOW_SIZE) {
            rollingHash = rollHash(rollingHash, buffer[i]);
            continue;
          }

          if (currentChunkSize > WINDOW_SIZE) {
            byte outByte = buffer[i - WINDOW_SIZE];
            rollingHash -= (outByte & 0xFF) * this.power;
          }

          rollingHash = rollHash(rollingHash, buffer[i]);

          boolean shouldCut = false;
          if (currentChunkSize >= MAX_SIZE) {
            shouldCut = true;
          } else if (currentChunkSize >= MIN_SIZE && (rollingHash & MASK) == CUT_VALUE) {
            shouldCut = true;
          }

          if (shouldCut) {
            addChunk(buffer, chunkStartOffset, currentChunkSize);
            chunkStartOffset = i + 1;
          }
        }

        int remaining = currentDataLimit - chunkStartOffset;
        if (remaining > 0) {
          System.arraycopy(buffer, chunkStartOffset, buffer, 0, remaining);
        }
        dataLeftOver = remaining;
      }

      if (dataLeftOver > 0) {
        addChunk(buffer, 0, dataLeftOver);
      }

    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public static double calculateDiscoveredDataRate(List<Block> listA, List<Block> listB) {
    if (listA == null || listA.isEmpty() || listB == null || listB.isEmpty()) {
      return 1.0;
    }

    Set<String> hashesInListA = new HashSet<>();
    for (Block blockA : listA) {
      if (blockA != null && blockA.getHashCode() != null) {
        hashesInListA.add(blockA.getHashCode());
      }
    }

    long totalSizeListB = 0;
    long diffSizeInListB = 0;

    for (Block blockB : listB) {
      if (blockB != null && blockB.getHashCode() != null) {
        long chunkSize = blockB.getChunkSize();
        totalSizeListB += chunkSize;

        if (!hashesInListA.contains(blockB.getHashCode())) {
          diffSizeInListB += chunkSize;
        }
      }
    }

    if (totalSizeListB == 0) {
      return 0.0;
    }

    return 1.0 * diffSizeInListB / totalSizeListB;
  }

  private static String formatBytes(long bytes) {
    if (bytes < 1024) return bytes + " B";
    int exp = (int) (Math.log(bytes) / Math.log(1024));
    String pre = "KMGTPE".charAt(exp - 1) + "i";
    return String.format("%.2f %sB", bytes / Math.pow(1024, exp), pre);
  }

  public static void main(String[] args) {
    if (args.length < 2) {
      System.err.println("Usage: java Main <original_file_path> <updated_file_path>");
      System.err.println("Example: java Main /path/to/original.db /path/to/updated.db");
      return;
    }

    System.out.println("=====================================================");
    System.out.println("          RabinCDC algorithm");
    System.out.println("=====================================================");

    String filePathOriginal = args[0];
    String filePathUpdate = args[1];

    File file1 = new File(filePathOriginal);
    File file2 = new File(filePathUpdate);

    if (!file1.exists()) {
      System.err.println("❌ ERROR: Original file not found: " + filePathOriginal);
      return;
    }
    if (!file2.exists()) {
      System.err.println("❌ ERROR: Update file not found: " + filePathUpdate);
      return;
    }

    System.out.println("SETUP:");
    System.out.println(
        "  - Original File: " + filePathOriginal + " (" + formatBytes(file1.length()) + ")");
    System.out.println(
        "  - Updated File:  " + filePathUpdate + " (" + formatBytes(file2.length()) + ")");

    long totalProcessingStartTime = System.currentTimeMillis();

    System.out.println("\n--- [1/3] 🚀 Processing ORIGINAL file... ---");
    long startTimeOriginal = System.currentTimeMillis();
    RabinCDCManager originalManager = new RabinCDCManager(filePathOriginal);
    originalManager.splitChunks(filePathOriginal);
    long endTimeOriginal = System.currentTimeMillis();

    System.out.println("✓ ORIGINAL file processing complete.");
    System.out.println("  RESULTS:");
    System.out.println("    - Total Chunks: " + originalManager.getChunks().size());
    System.out.printf(
        "    - Average Chunk Size: %.2f KiB%n",
        (file1.length() / originalManager.getChunks().size() / 1024.0));

    double durationOriginal = (endTimeOriginal - startTimeOriginal) / 1000.0;
    System.out.printf("    - Processing Time: %.3f seconds%n", durationOriginal);
    if (durationOriginal > 0 && file1.length() > 0) {
      System.out.printf(
          "    - Processing Speed: %.2f MB/s%n",
          file1.length() / (1024.0 * 1024.0) / durationOriginal);
    }

    System.out.println("\n--- [2/3] 🛰️  Processing UPDATED file... ---");
    long startTimeUpdate = System.currentTimeMillis();
    RabinCDCManager updatedManager = new RabinCDCManager(filePathUpdate);
    updatedManager.splitChunks(filePathUpdate);
    long endTimeUpdate = System.currentTimeMillis();

    System.out.println("✓ UPDATED file processing complete.");
    System.out.println("  RESULTS:");
    System.out.println("    - Total Chunks: " + updatedManager.getChunks().size());
    System.out.printf(
        "    - Average Chunk Size: %.2f KiB%n",
        (file2.length() / updatedManager.getChunks().size() / 1024.0));

    double durationUpdate = (endTimeUpdate - startTimeUpdate) / 1000.0;
    System.out.printf("    - Processing Time: %.3f seconds%n", durationUpdate);
    if (durationUpdate > 0 && file2.length() > 0) {
      System.out.printf(
          "    - Processing Speed: %.2f MB/s%n",
          file2.length() / (1024.0 * 1024.0) / durationUpdate);
    }

    System.out.println("\n--- [3/3] 📊 Calculating Comparison... ---");
    double discoveredRate =
        calculateDiscoveredDataRate(originalManager.getChunks(), updatedManager.getChunks());
    System.out.printf("✓ Comparison complete.%n");
    System.out.printf("  Discovered Data Rate: %.2f%%%n", discoveredRate * 100);
    System.out.println(
        "    (This means "
            + String.format("%.2f%%", discoveredRate * 100)
            + " of the updated file's content wasn't found in the original file)");

    long totalProcessingEndTime = System.currentTimeMillis();
    System.out.println("\n=====================================================");
    System.out.printf(
        "✨ All tasks finished in a total of %.3f seconds.%n",
        (totalProcessingEndTime - totalProcessingStartTime) / 1000.0);
    System.out.println("=====================================================");
  }
}
