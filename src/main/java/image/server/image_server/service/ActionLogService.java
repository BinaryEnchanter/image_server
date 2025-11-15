// java d:\study\空间壁纸\image_server\src\main\java\image\server\image_server\service\ActionLogService.java
package image.server.image_server.service;

import image.server.image_server.model.UserActionLog;
import image.server.image_server.repository.UserActionLogRepository;
import image.server.image_server.repository.UserRepository;
import image.server.image_server.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class ActionLogService {
    @Autowired
    private UserActionLogRepository repo;

    @Autowired
    private UserRepository userRepository;

    public void log(UUID userUuid, String action, UUID targetUuid, String meta) {
        UserActionLog log = new UserActionLog();
        log.setUserUuid(userUuid);
        log.setAction(action);
        String username = null;
        try {
            User u = userRepository.findById(userUuid).orElse(null);
            if (u != null) username = u.getUsername();
        } catch (Exception ignored) {}
        log.setUsername(username == null ? "" : username);
        log.setTargetUuid(targetUuid);
        log.setMeta(meta);
        repo.save(log);
    }

    public Page<UserActionLog> listByUser(UUID userUuid, int page, int size) {
        Pageable p = PageRequest.of(Math.max(0, page), Math.max(1, size), Sort.by(Sort.Direction.DESC, "createdAt"));
        return repo.findAllByUserUuidOrderByCreatedAtDesc(userUuid, p);
    }
}