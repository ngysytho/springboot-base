package vn.mpl.one.core.pagination;

import java.util.List;
import java.util.function.Function;

public record PageResponse<T>(long count, List<T> rows, int page, int size,
    int totalPages, boolean hasNext, boolean hasPrevious) {
  public PageResponse { rows = List.copyOf(rows); }

  public <R> PageResponse<R> map(Function<? super T, ? extends R> mapper) {
    List<R> mapped = new java.util.ArrayList<>(rows.size());
    rows.forEach(value -> mapped.add(mapper.apply(value)));
    return new PageResponse<>(count, mapped, page, size, totalPages, hasNext, hasPrevious);
  }
}
