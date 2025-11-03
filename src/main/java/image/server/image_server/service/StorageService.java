package image.server.image_server.service;


import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Duration;

/**
 * 存储服务抽象：当前实现 LocalStorageService（本地磁盘）
 * 未来可增加 S3/MinIO 实现
 */
public interface StorageService {
    /**
     * 存储文件并返回逻辑路径（例如 "wallpapers/{uuid}/original.jpg"）
     */
    String store(MultipartFile file, String logicalPath) throws IOException;

    /**
     * 生成缩略图并返回 thumbnail logicalPath
     */
    String generateThumbnail(String sourceLogicalPath, String thumbLogicalPath, int maxWidth, int maxHeight) throws IOException;

    /**
     * 读取文件为 Resource（用于流式返回）
     */
    Resource loadAsResource(String logicalPath) throws IOException;

    /**
     * 删除文件（可选）
     */
    boolean delete(String logicalPath) throws IOException;
}
