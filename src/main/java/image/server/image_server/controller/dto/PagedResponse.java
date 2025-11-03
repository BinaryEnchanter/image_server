package image.server.image_server.controller.dto;

import java.util.List;

public class PagedResponse<T> {
    private int page;
    private int perPage;
    private long total;
    private List<T> items;

    public PagedResponse(int page, int perPage, long total, List<T> items) {
        this.page = page;
        this.perPage = perPage;
        this.total = total;
        this.items = items;
    }

    // getters
    public int getPage() { return page; }
    public int getPerPage() { return perPage; }
    public long getTotal() { return total; }
    public List<T> getItems() { return items; }
}
