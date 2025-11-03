package image.server.image_server.service;

import net.coobird.thumbnailator.Thumbnails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.*;

@Service
public class LocalStorageService implements StorageService {

    private final Path basePath;

    public LocalStorageService(@Value("${app.storage.base-dir}") String baseDir) {
        // 规范化成绝对路径，避免依赖工作目录
        this.basePath = Paths.get(baseDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.basePath);
        } catch (IOException e) {
            throw new RuntimeException("Unable to create base storage dir: " + this.basePath, e);
        }
        System.out.println("LocalStorageService baseDir = " + this.basePath);
    }

    @Override
    public String store(MultipartFile file, String logicalPath) throws IOException {
        Path dest = basePath.resolve(logicalPath).normalize();
        // 安全：防止路径越界
        if (!dest.startsWith(basePath)) {
            throw new IOException("Invalid storage path: " + logicalPath);
        }
        Path parent = dest.getParent();
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
        }
        // 使用流拷贝写入，避免依赖容器的临时文件处理
        try (InputStream in = file.getInputStream()) {
            Files.copy(in, dest, StandardCopyOption.REPLACE_EXISTING);
        }
        return basePath.relativize(dest).toString().replace('\\', '/'); // 返回逻辑 path（统一斜杠）
    }

    @Override
    public String generateThumbnail(String sourceLogicalPath, String thumbLogicalPath, int maxWidth, int maxHeight) throws IOException {
        Path source = basePath.resolve(sourceLogicalPath).normalize();
        Path dest = basePath.resolve(thumbLogicalPath).normalize();
        Path parent = dest.getParent();
        if (parent != null && !Files.exists(parent)) Files.createDirectories(parent);
        Thumbnails.of(source.toFile()).size(maxWidth, maxHeight).toFile(dest.toFile());
        return basePath.relativize(dest).toString().replace('\\', '/');
    }

    @Override
    public Resource loadAsResource(String logicalPath) throws IOException {
        Path p = basePath.resolve(logicalPath).normalize();
        if (!Files.exists(p)) throw new FileNotFoundException("File not found: " + p);
        return new FileSystemResource(p);
    }

    @Override
    public boolean delete(String logicalPath) throws IOException {
        Path p = basePath.resolve(logicalPath).normalize();
        return Files.deleteIfExists(p);
    }
}
