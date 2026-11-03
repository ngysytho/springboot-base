package vn.mpl.one.core.response;

import java.util.List;
import org.springframework.data.domain.Page;

public record PageResponse<T>(
    long count,
    List<T> rows,
    int page,
    int size,
    int totalPages,
    boolean hasNext,
    boolean hasPrevious) {
  public static <T> PageResponse<T> from(Page<T> p) {
    return new PageResponse<>(
        p.getTotalElements(),
        p.getContent(),
        p.getNumber() + 1,
        p.getSize(),
        p.getTotalPages(),
        p.hasNext(),
        p.hasPrevious());
  }
}
