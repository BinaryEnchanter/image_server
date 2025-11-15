package image.server.image_server.service;

import image.server.image_server.model.Comment;
import image.server.image_server.model.CommentVote;
import image.server.image_server.repository.CommentRepository;
import image.server.image_server.repository.CommentVoteRepository;
import image.server.image_server.repository.WallpaperRepository;
import image.server.image_server.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;
import java.util.UUID;

@Service
public class CommentService {
    @Autowired
    private CommentRepository commentRepository;
    @Autowired
    private CommentVoteRepository voteRepository;
    @Autowired
    private WallpaperRepository wallpaperRepository;
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ActionLogService actionLogService;

    @Transactional
    public Comment addComment(UUID userUuid, UUID wallpaperUuid, String content, Long parentId) {
        userRepository.findById(userUuid).orElseThrow(() -> new RuntimeException("user not found"));
        wallpaperRepository.findById(wallpaperUuid).orElseThrow(() -> new RuntimeException("wallpaper not found"));
        if (parentId != null) {
            Comment parent = commentRepository.findById(parentId).orElseThrow(() -> new RuntimeException("parent not found"));
            if (!parent.getWallpaperUuid().equals(wallpaperUuid)) throw new RuntimeException("parent mismatch");
        }
        Comment c = new Comment();
        c.setUserUuid(userUuid);
        c.setWallpaperUuid(wallpaperUuid);
        c.setParentId(parentId);
        c.setContent(content);
        Comment saved = commentRepository.save(c);
        String meta = "{\"comment_id\":" + saved.getId() + (parentId != null ? ",\"parent_id\":" + parentId : "") + "}";
        actionLogService.log(userUuid, (parentId == null ? "comment_add" : "comment_reply"), wallpaperUuid, meta);
        return saved;
    }

    @Transactional
    public void deleteComment(UUID actorUuid, Long commentId, boolean isAdmin) {
        Comment c = commentRepository.findById(commentId).orElseThrow(() -> new RuntimeException("comment not found"));
        if (!isAdmin && !c.getUserUuid().equals(actorUuid)) throw new RuntimeException("forbidden");
        commentRepository.delete(c);
        actionLogService.log(actorUuid, "comment_delete", c.getWallpaperUuid(), "{\"comment_id\":" + commentId + "}");
    }

    @Transactional
    public void vote(UUID userUuid, Long commentId, int v) {
        if (v != 1 && v != -1) throw new RuntimeException("invalid vote");
        Comment c = commentRepository.findById(commentId).orElseThrow(() -> new RuntimeException("comment not found"));
        Optional<CommentVote> existing = voteRepository.findByCommentIdAndUserUuid(commentId, userUuid);
        if (existing.isEmpty()) {
            CommentVote cv = new CommentVote();
            cv.setCommentId(commentId);
            cv.setUserUuid(userUuid);
            cv.setVote((short)v);
            voteRepository.save(cv);
            if (v == 1) c.setLikeCount(c.getLikeCount() + 1); else c.setDislikeCount(c.getDislikeCount() + 1);
        } else {
            CommentVote cv = existing.get();
            int prev = cv.getVote();
            if (prev == v) return;
            cv.setVote((short)v);
            voteRepository.save(cv);
            if (prev == 1) c.setLikeCount(c.getLikeCount() - 1); else c.setDislikeCount(c.getDislikeCount() - 1);
            if (v == 1) c.setLikeCount(c.getLikeCount() + 1); else c.setDislikeCount(c.getDislikeCount() + 1);
        }
        commentRepository.save(c);
        actionLogService.log(userUuid, (v == 1 ? "comment_like" : "comment_dislike"), c.getWallpaperUuid(), "{\"comment_id\":" + commentId + "}");
    }

    public Page<Comment> list(UUID wallpaperUuid, int page, int size) {
        return commentRepository.findByWallpaperUuidOrderByCreatedAtDesc(wallpaperUuid, PageRequest.of(Math.max(0, page), Math.max(1, size)));
    }
}