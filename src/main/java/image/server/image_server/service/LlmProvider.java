package image.server.image_server.service;

import java.util.List;
import java.util.Map;

public interface LlmProvider {
    /**
     * messages: List<Map<String,Object>> each item has role and content structure appropriate to provider
     * return assistant text
     */
    String chat(List<Map<String,Object>> messages) throws Exception;
}
