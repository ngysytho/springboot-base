package vn.mpl.one.core.specification;

import jakarta.persistence.criteria.*;
import java.time.Instant;
import java.util.*;
import org.springframework.data.jpa.domain.Specification;
import vn.mpl.one.core.exception.InvalidFilterException;
import vn.mpl.one.core.filter.*;

public final class SpecificationBuilder {
  private SpecificationBuilder() {}

  public static <T> Specification<T> build(FilterCriteria criteria, Map<String, FilterField> fields) {
    FilterParser.validate(criteria, fields);
    Specification<T> result = (r, q, b) -> b.conjunction();
    for (FilterRule rule : criteria.rules()) {
      Class<?> type = fields.get(rule.field()).type();
      if (type == null) throw new InvalidFilterException("Unknown filter field");
      if (rule.operator() == FilterOperator.LIKE && type != String.class)
        throw new InvalidFilterException("LIKE requires text");
      List<Object> values =
          rule.value() == null
              ? List.of()
              : Arrays.stream(
                      rule.operator() == FilterOperator.IN
                          ? rule.value().split(",", -1)
                          : new String[] {rule.value()})
                  .map(v -> convert(v, type))
                  .toList();
      result = result.and((r, q, b) -> predicate(r.get(rule.field()), b, rule.operator(), values));
    }
    return result;
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private static Object convert(String value, Class<?> type) {
    try {
      if (value.isBlank()) throw new InvalidFilterException("Empty filter value");
      if (type == String.class) return value;
      if (type == Instant.class) return Instant.parse(value);
      if (type == UUID.class) {
        if (!value.matches("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}"))
          throw new InvalidFilterException("Invalid UUID");
        return UUID.fromString(value);
      }
      if (type == java.time.LocalDate.class) return java.time.LocalDate.parse(value);
      if (type == java.time.OffsetDateTime.class) return java.time.OffsetDateTime.parse(value);
      if (type == Integer.class) return Integer.valueOf(value);
      if (type == java.math.BigDecimal.class) return new java.math.BigDecimal(value);
      if (type == Boolean.class) {
        if (!value.equals("true") && !value.equals("false")) throw new InvalidFilterException("Invalid boolean");
        return Boolean.valueOf(value);
      }
      if (type == Long.class) return Long.valueOf(value);
      if (type.isEnum()) return Enum.valueOf((Class) type, value);
    } catch (RuntimeException e) {
      throw new InvalidFilterException("Invalid filter value");
    }
    throw new InvalidFilterException("Unsupported filter type");
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  private static Predicate predicate(
      Path path, CriteriaBuilder b, FilterOperator op, List<Object> values) {
    Object value = values.isEmpty() ? null : values.getFirst();
    return switch (op) {
      case EQ -> b.equal(path, value);
      case NEQ -> b.notEqual(path, value);
      case GT -> b.greaterThan(path, (Comparable) value);
      case GTE -> b.greaterThanOrEqualTo(path, (Comparable) value);
      case LT -> b.lessThan(path, (Comparable) value);
      case LTE -> b.lessThanOrEqualTo(path, (Comparable) value);
      case LIKE ->
          b.like(
              b.lower(path),
              "%"
                  + value
                      .toString()
                      .toLowerCase(Locale.ROOT)
                      .replace("!", "!!")
                      .replace("%", "!%")
                      .replace("_", "!_")
                  + "%",
              '!');
      case IN -> path.in(values);
      case IS_NULL -> b.isNull(path);
      case IS_NOT_NULL -> b.isNotNull(path);
    };
  }
}
