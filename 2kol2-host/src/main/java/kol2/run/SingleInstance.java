package kol2.run;

import kol2.Halt;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/** 用文件锁保证同一时间只有一个实例。 */
public final class SingleInstance implements AutoCloseable {
    private final FileChannel channel;
    private final FileLock lock;

    private SingleInstance(FileChannel channel, FileLock lock) {
        this.channel = channel;
        this.lock = lock;
    }

    /** 抢不到锁时拒绝启动。 */
    public static SingleInstance acquire(Path file) {
        try {
            Path parent = file.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            FileChannel channel = FileChannel.open(file, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
            FileLock lock;
            try {
                lock = channel.tryLock();
            } catch (OverlappingFileLockException ex) {
                channel.close();
                throw new Halt(2, "已有一个程序实例在运行。");
            }
            if (lock == null) {
                channel.close();
                throw new Halt(2, "已有一个程序实例在运行。");
            }
            return new SingleInstance(channel, lock);
        } catch (Halt halt) {
            throw halt;
        } catch (IOException ex) {
            throw new Halt(2, "无法创建单实例锁: " + file);
        }
    }

    /** 释放锁。 */
    @Override
    public void close() {
        try {
            lock.release();
        } catch (IOException ignored) {
            // 进程退出时锁也会消失。
        }
        try {
            channel.close();
        } catch (IOException ignored) {
            // 同上。
        }
    }
}
