package org.gusdb.fgputil;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

public class IoUtil {
  
  /**
   * Converts binary data into an input stream.  This can be used if the result
   * type is a stream, and the content to be returned already exists in memory
   * as a string.  This is simply a wrapper around the ByteArrayInputStream
   * constructor.
   * 
   * @param data data to be converted
   * @return stream representing the data
   */
  public static InputStream getStreamFromBytes(byte[] data) {
    return new ByteArrayInputStream(data);
  }
  
  /**
   * Converts a string into an open input stream.  This can be used if the
   * result type is a stream, and the content to be returned already exists in
   * memory as a string.
   * 
   * @param str string to be converted
   * @return input stream representing the string
   */
  public static InputStream getStreamFromString(String str) {
    return getStreamFromBytes(str.getBytes(Charset.defaultCharset()));
  }
  
  // NOTE: this does the same thing as deleteDirectoryTree(Path); pick one!
  public static void deleteDir(File dir) throws IOException {
    if (!dir.exists())
      throw new IOException("Unable to find directory at path: " + dir.getAbsolutePath());
    for (File f : dir.listFiles()) {
      if (f.isDirectory())
        deleteDir(f);
      else
        if (!f.delete())
          throw new IOException("Unable to delete file at path: " + f.getAbsolutePath());
      if (!dir.delete())
        throw new IOException("Unable to delete directory at path: " + dir.getAbsolutePath());
    }
  }

  // NOTE: this does the same thing as deleteDir(File); pick one!
  public static void deleteDirectoryTree(Path directory) throws IOException {
    Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
      @Override
      public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        Files.delete(file);
        return FileVisitResult.CONTINUE;
      }
      @Override
      public FileVisitResult postVisitDirectory(Path dir, IOException e) throws IOException {
        if (e == null) {
          Files.delete(dir);
          return FileVisitResult.CONTINUE;
        }
        else {
          // directory iteration failed
          throw e;
        }
      }
    });
  }

  public static File getWritableDirectoryOrDie(String directoryName) {
    File f = new File(directoryName);
    if (!f.isDirectory() || !f.canWrite()) {
      System.err.println("ERROR: " + f.getAbsolutePath()
          + " is not a writable directory.");
      System.exit(2);
    }
    return f;

  }

  public static File getReadableFileOrDie(String fileName) {
    File f = new File(fileName);
    if (!f.isFile() || !f.canRead()) {
      System.err.println("ERROR: " + f.getAbsolutePath()
          + " is not a readable file.");
      System.exit(2);
    }
    return f;
  }
  
  public static void closeQuietly(Closeable... closeable) {
    for (Closeable each : closeable) {
      try { if (closeable != null) each.close(); } catch (Exception ex) { /* do nothing */ }
    }
  }
  
  /**
   * Transfers data from input stream to the output stream until no more data
   * is available, then closes input stream (but not output stream).
   * 
   * @param outputStream output stream data is written to
   * @param inputStream input stream data is read from
   * @throws IOException if problem reading/writing data occurs
   */
  public static void transferStream(OutputStream outputStream, InputStream inputStream)
      throws IOException {
    try {
      byte[] buffer = new byte[1024]; // send 1kb at a time
      int bytesRead;
      while ((bytesRead = inputStream.read(buffer)) != -1) {
        outputStream.write(buffer, 0, bytesRead);
      }
    }
    finally {
      // only close input stream; container will close output stream
      inputStream.close();
    }
  }
  
  public static byte[] serialize(Serializable obj) throws IOException {
    try (ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
         ObjectOutputStream objStream = new ObjectOutputStream(byteStream)) {
      objStream.writeObject(obj);
      return byteStream.toByteArray();
    }
  }
  
  public static Object deserialize(byte[] bytes) throws IOException, ClassNotFoundException {
    try (ByteArrayInputStream byteStream = new ByteArrayInputStream(bytes);
         ObjectInputStream objStream = new ObjectInputStream(byteStream)) {
      return objStream.readObject();
    }
  }
}
