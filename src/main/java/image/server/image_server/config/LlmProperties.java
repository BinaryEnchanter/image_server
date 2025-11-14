package image.server.image_server.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "llm")
public class LlmProperties {
    private String baseUrl; // e.g. https://ark.cn-beijing.volces.com
    private String apiKey;
    private String model;   // doubao-seed-1-6-251015 (示例)
    private boolean useHistory = true;
    private int historyWindow = 20;
    private String systemPrompt = "你是壁纸中心的智能客服，熟悉上传/下载/购买/金币/撤回上传/账户管理与常见问题。请简洁回答。";
    private int rateLimitPerMinute = 30;

    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public boolean isUseHistory() { return useHistory; }
    public void setUseHistory(boolean useHistory) { this.useHistory = useHistory; }
    public int getHistoryWindow() { return historyWindow; }
    public void setHistoryWindow(int historyWindow) { this.historyWindow = historyWindow; }
    public String getSystemPrompt() { return systemPrompt; }
    public void setSystemPrompt(String systemPrompt) { this.systemPrompt = systemPrompt; }
    public int getRateLimitPerMinute() { return rateLimitPerMinute; }
    public void setRateLimitPerMinute(int rateLimitPerMinute) { this.rateLimitPerMinute = rateLimitPerMinute; }
}
