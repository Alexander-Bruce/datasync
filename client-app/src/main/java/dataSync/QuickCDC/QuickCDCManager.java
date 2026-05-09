package dataSync.QuickCDC;

import dataSync.Block;
import dataSync.CDCManager;
import java.io.*;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class QuickCDCManager extends CDCManager {
  private static final int FEATURE_N_SIZE = 3;
  private static final int FEATURE_M_SIZE = 3;
  private static final int MIN_CHUNK_SIZE_FOR_FEATURES = FEATURE_N_SIZE + FEATURE_M_SIZE;
  private static final int JUMP_LEN_UNIQUE = 8 * 1024;
  private static final int NORMAL_CHUNK_SIZE_FOR_MASK_DECISION = 16 * 1024;
  private static final int MASK_BITS_TARGET_LARGER_CHUNK = (1 << 14) - 1;
  private static final int MASK_BITS_TARGET_SMALLER_CHUNK = MASK_BITS_TARGET_LARGER_CHUNK >> 2;
  private static final long CDC_BREAK_CONDITION_VALUE = 0;
  private static final long[] GEAR_TABLE = new long[256];
  private static final int WINDOW_SIZE = 64;
  private final Map<ByteBuffer, Integer> frontFeatureToLength = new HashMap<>(65535);
  private final Map<ByteBuffer, Integer> endFeatureToLength = new HashMap<>(65535);
  public long count = 0;

  static {
    Random random = new Random(1);
    for (int i = 0; i < 256; i++) {
      GEAR_TABLE[i] = random.nextLong();
    }
  }

  public QuickCDCManager(String filePath) {
    File file = new File(filePath);
    if (!file.exists()) {
      throw new IllegalArgumentException("File not found: " + filePath);
    }
  }

  public QuickCDCManager() {}

  private long rollHash(long preHash, byte nextByte) {
    return (preHash << 1) + GEAR_TABLE[nextByte & 0xFF];
  }

  private void addChunk(byte[] buffer, int offset, int chunkSize, BigInteger index) {
    byte[] chunkData = new byte[chunkSize];
    System.arraycopy(buffer, offset, chunkData, 0, chunkSize);
    chunks.add(new Block(chunkSize, index, totalOffset, chunkData));
    totalOffset += chunkSize;
  }

  private ByteBuffer copyToByteBuffer(byte[] buffer, int offset, int length) {
    ByteBuffer bb = ByteBuffer.allocate(length);
    bb.put(buffer, offset, length);
    bb.flip();
    return bb;
  }

  @Override
  public void splitChunks(String filePath) {
    BigInteger chunkIndex = BigInteger.ZERO;
    byte[] buffer = new byte[MIN_SIZE * 1024 / 2];
    int dataLeftOverFromPreviousRead = 0;

    try (BufferedInputStream bis =
        new BufferedInputStream(Files.newInputStream(Paths.get(filePath)))) {
      int bytesReadFromStream;
      while ((bytesReadFromStream =
              bis.read(
                  buffer,
                  dataLeftOverFromPreviousRead,
                  buffer.length - dataLeftOverFromPreviousRead))
          != -1) {

        int currentDataLimitInBuffer = dataLeftOverFromPreviousRead + bytesReadFromStream;
        int currentParsePos = 0;

        while (currentParsePos < currentDataLimitInBuffer) {
          int chunkActualStartOffset = currentParsePos;
          boolean jumpedDuplicate = false;
          int chunkDefinedThisIterationLength = 0;

          if (currentDataLimitInBuffer - chunkActualStartOffset >= MIN_CHUNK_SIZE_FOR_FEATURES) {
            ByteBuffer frontFeature =
                copyToByteBuffer(buffer, chunkActualStartOffset, FEATURE_N_SIZE);
            Integer expectedLength = frontFeatureToLength.get(frontFeature);

            if (expectedLength != null
                && expectedLength > 0
                && expectedLength >= MIN_CHUNK_SIZE_FOR_FEATURES
                && chunkActualStartOffset + expectedLength <= currentDataLimitInBuffer) {

              ByteBuffer endFeature =
                  copyToByteBuffer(
                      buffer,
                      chunkActualStartOffset + expectedLength - FEATURE_M_SIZE,
                      FEATURE_M_SIZE);
              Integer lengthFromEndFeature = endFeatureToLength.get(endFeature);

              if (expectedLength.equals(lengthFromEndFeature)) {
                addChunk(buffer, chunkActualStartOffset, expectedLength, chunkIndex);
                chunkIndex = chunkIndex.add(BigInteger.ONE);
                count++;
                currentParsePos += expectedLength;
                jumpedDuplicate = true;
              }
            }
          }

          if (jumpedDuplicate) {
            continue;
          }

          int initialSkippedLength = 0;
          int cdcScanEffectiveStartOffset = chunkActualStartOffset;

          if (JUMP_LEN_UNIQUE > 0
              && (currentDataLimitInBuffer - chunkActualStartOffset) >= JUMP_LEN_UNIQUE) {
            initialSkippedLength = JUMP_LEN_UNIQUE;
            cdcScanEffectiveStartOffset = chunkActualStartOffset + JUMP_LEN_UNIQUE;
          }

          long rollingHashForCDC = 0;
          int cdcCutPointInBuffer = -1;

          for (int i = cdcScanEffectiveStartOffset; i < currentDataLimitInBuffer; i++) {
            byte currentByte = buffer[i];

            int scanOffset = i - cdcScanEffectiveStartOffset;

            rollingHashForCDC = rollHash(rollingHashForCDC, currentByte);

            if (scanOffset >= WINDOW_SIZE) {
              byte outByte = buffer[i - WINDOW_SIZE];
              rollingHashForCDC ^= GEAR_TABLE[outByte & 0xFF];
            }

            int currentTotalChunkCandidateSize = initialSkippedLength + scanOffset + 1;

            boolean cutConditionMetByCDC = false;

            if (currentTotalChunkCandidateSize >= MIN_SIZE) {
              if (currentTotalChunkCandidateSize >= MAX_SIZE) {
                cutConditionMetByCDC = true;
              } else {
                long currentDynamicMaskToUse =
                    (currentTotalChunkCandidateSize >= NORMAL_CHUNK_SIZE_FOR_MASK_DECISION)
                        ? MASK_BITS_TARGET_SMALLER_CHUNK
                        : MASK_BITS_TARGET_LARGER_CHUNK;

                if ((rollingHashForCDC & currentDynamicMaskToUse) == CDC_BREAK_CONDITION_VALUE) {
                  cutConditionMetByCDC = true;
                }
              }
            }

            if (cutConditionMetByCDC) {
              cdcCutPointInBuffer = i;
              break;
            }
          }

          if (cdcCutPointInBuffer != -1) {
            chunkDefinedThisIterationLength = cdcCutPointInBuffer - chunkActualStartOffset + 1;
            addChunk(buffer, chunkActualStartOffset, chunkDefinedThisIterationLength, chunkIndex);

            if (chunkDefinedThisIterationLength >= MIN_CHUNK_SIZE_FOR_FEATURES) {
              ByteBuffer frontKey =
                  copyToByteBuffer(buffer, chunkActualStartOffset, FEATURE_N_SIZE);
              ByteBuffer endKey =
                  copyToByteBuffer(
                      buffer,
                      chunkActualStartOffset + chunkDefinedThisIterationLength - FEATURE_M_SIZE,
                      FEATURE_M_SIZE);

              frontFeatureToLength.put(frontKey, chunkDefinedThisIterationLength);
              endFeatureToLength.put(endKey, chunkDefinedThisIterationLength);
            }

            chunkIndex = chunkIndex.add(BigInteger.ONE);
            currentParsePos = cdcCutPointInBuffer + 1;
          } else {
            int remainingToCarryOver = currentDataLimitInBuffer - chunkActualStartOffset;
            if (remainingToCarryOver > 0) {
              System.arraycopy(buffer, chunkActualStartOffset, buffer, 0, remainingToCarryOver);
            }
            dataLeftOverFromPreviousRead = remainingToCarryOver;
            break;
          }
        }

        if (currentParsePos >= currentDataLimitInBuffer) {
          dataLeftOverFromPreviousRead = 0;
        }
      }

      if (dataLeftOverFromPreviousRead > 0) {
        addChunk(buffer, 0, dataLeftOverFromPreviousRead, chunkIndex);

        if (dataLeftOverFromPreviousRead >= MIN_CHUNK_SIZE_FOR_FEATURES) {
          ByteBuffer frontKey = copyToByteBuffer(buffer, 0, FEATURE_N_SIZE);
          ByteBuffer endKey =
              copyToByteBuffer(
                  buffer, dataLeftOverFromPreviousRead - FEATURE_M_SIZE, FEATURE_M_SIZE);
          frontFeatureToLength.put(frontKey, dataLeftOverFromPreviousRead);
          endFeatureToLength.put(endKey, dataLeftOverFromPreviousRead);
        }
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

  public static String formatBytes(long bytes) {
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
    System.out.println("          QuickCDC algorithm");
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
    QuickCDCManager originalManager = new QuickCDCManager(filePathOriginal);
    long startTimeOriginal = System.currentTimeMillis();
    originalManager.splitChunks(filePathOriginal);
    long endTimeOriginal = System.currentTimeMillis();

    System.out.println("✓ ORIGINAL file processing complete.");
    System.out.println("  RESULTS:");
    System.out.println("    - Total Chunks: " + originalManager.getChunks().size());
    System.out.printf(
        "    - Average Chunk Size: %.2f KiB%n",
        (file1.length() / originalManager.getChunks().size() / 1024.0));
    System.out.println(originalManager.count);

    double durationOriginal = (endTimeOriginal - startTimeOriginal) / 1000.0;
    System.out.printf("    - Processing Time: %.3f seconds%n", durationOriginal);
    if (durationOriginal > 0 && file1.length() > 0) {
      System.out.printf(
          "    - Processing Speed: %.2f MB/s%n",
          file1.length() / (1024.0 * 1024.0) / durationOriginal);
    }

    System.out.println("\n--- [2/3] 🛰️  Processing UPDATED file... ---");
    QuickCDCManager updatedManager = new QuickCDCManager(filePathUpdate);
    long startTimeUpdate = System.currentTimeMillis();
    updatedManager.splitChunks(filePathUpdate);
    long endTimeUpdate = System.currentTimeMillis();

    System.out.println("✓ UPDATED file processing complete.");
    System.out.println("  RESULTS:");
    System.out.println("    - Total Chunks: " + updatedManager.getChunks().size());
    System.out.printf(
        "    - Average Chunk Size: %.2f KiB%n",
        (file2.length() / updatedManager.getChunks().size() / 1024.0));
    System.out.println(updatedManager.count);

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
