package image.server.image_server.controller;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import image.server.image_server.service.WallpaperService;

import java.io.IOException;

/**
 * 静态文件访问端点（用于返回本地存储的文件）
 * 注意：/files/** 是公开的路径（在此实现中公开），如果你希望保护原始文件，请改为非公开并在 download 接口里流式返回 Resource。
 */
@RestController
@RequestMapping("/files")
public class FileController {

    @Autowired
    private WallpaperService wallpaperService;

    @GetMapping("/{year}/{id}/{file:.+}")
    public ResponseEntity<?> serveFile(@PathVariable String year, @PathVariable String id, @PathVariable String file) {
        // 注意：路径构造为 wallpapers/{uuid}/original.jpg，或 wallpapers/{uuid}/thumb.jpg
        String logicalPath = String.format("wallpapers/%s/%s", id, file);
        try {
            Resource res = wallpaperService.loadAsResource(logicalPath);
            // try to set content type by filename
            String filename = res.getFilename();
            MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
            if (filename.endsWith(".jpg") || filename.endsWith(".jpeg")) mediaType = MediaType.IMAGE_JPEG;
            else if (filename.endsWith(".png")) mediaType = MediaType.IMAGE_PNG;
            else if (filename.endsWith(".webp")) mediaType = MediaType.valueOf("image/webp");
            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + res.getFilename() + "\"")
                    .body(res);
        } catch (IOException ex) {
            return ResponseEntity.status(404).body("file not found");
        }
    }
}
