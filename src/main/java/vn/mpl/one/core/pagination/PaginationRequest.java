package vn.mpl.one.core.pagination;

import java.util.*;
import org.springframework.data.domain.*;
import vn.mpl.one.core.exception.BadRequestException;

public record PaginationRequest(int page, int size, List<String> sort) {
  public PaginationRequest {
    if (page < 1 || size < 1 || size > 100)
      throw new BadRequestException("page must be >= 1 and size between 1 and 100");
    sort = sort == null ? List.of() : List.copyOf(sort);
  }

  public Pageable pageable(Set<String> fields) {
    List<Sort.Order> orders = new ArrayList<>();
    for (String group : sort)
      for (String item : group.split("&&", -1)) {
        String[] p = item.split(":", -1);
        if (p.length != 2 || !fields.contains(p[0]) || !Set.of("asc", "desc").contains(p[1]))
          throw new BadRequestException("Invalid sort field or direction");
        orders.add(new Sort.Order(Sort.Direction.fromString(p[1]), p[0]));
      }
    if (orders.isEmpty()) orders.add(Sort.Order.desc("createdAt"));
    orders.add(Sort.Order.asc("id"));
    return PageRequest.of(page - 1, size, Sort.by(orders));
  }
}
