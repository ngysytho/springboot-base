package vn.mpl.one.core.pagination;

import java.util.List;
import vn.mpl.one.core.exception.BadRequestException;

/** Framework-free, one-based pagination value shared across application ports. */
public record PaginationRequest(int page, int size, List<String> sort) {
  public static final int DEFAULT_PAGE = 1;
  public static final int DEFAULT_SIZE = 10;
  public static final int MAX_SIZE = 100;

  public PaginationRequest {
    if (page < 1 || size < 1 || size > MAX_SIZE)
      throw new BadRequestException("page must be >= 1 and size between 1 and 100");
    sort = sort == null ? List.of() : List.copyOf(sort);
  }
  public static PaginationRequest defaults() { return new PaginationRequest(DEFAULT_PAGE, DEFAULT_SIZE, List.of()); }
}
