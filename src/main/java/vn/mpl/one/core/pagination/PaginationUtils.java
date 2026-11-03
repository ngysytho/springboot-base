package vn.mpl.one.core.pagination;

import java.util.*;
import org.springframework.data.domain.*;
import vn.mpl.one.core.exception.BadRequestException;

public final class PaginationUtils {
  private PaginationUtils() {}

  public static Pageable pageable(PaginationRequest request, Set<String> allowedFields) {
    List<Sort.Order> orders = new ArrayList<>();
    Set<String> seen = new HashSet<>();
    for (String group : request.sort()) {
      for (String item : group.split("&&", -1)) {
        String[] parts = item.split(":", -1);
        if (parts.length != 2 || !parts[0].matches("[A-Za-z][A-Za-z0-9_]*")
            || !allowedFields.contains(parts[0]) || !Set.of("asc", "desc").contains(parts[1])
            || !seen.add(parts[0])) throw new BadRequestException("Invalid or duplicate sort");
        orders.add(new Sort.Order(Sort.Direction.fromString(parts[1]), parts[0]));
      }
    }
    // Internal defaults are not user-controlled paths. BaseEntity provides both columns.
    if (orders.isEmpty()) orders.add(Sort.Order.desc("createdAt"));
    if (!seen.contains("id")) orders.add(Sort.Order.asc("id"));
    return PageRequest.of(request.page() - 1, request.size(), Sort.by(orders));
  }

  public static <T> PageResponse<T> from(Page<T> page) {
    return new PageResponse<>(page.getTotalElements(), page.getContent(), page.getNumber() + 1,
        page.getSize(), page.getTotalPages(), page.hasNext(), page.hasPrevious());
  }
}
