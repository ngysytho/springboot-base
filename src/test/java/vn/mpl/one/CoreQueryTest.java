package vn.mpl.one;

import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.assertj.core.api.Assertions.*;
import vn.mpl.one.core.exception.*;
import vn.mpl.one.core.filter.*;
import vn.mpl.one.core.pagination.*;
import vn.mpl.one.core.specification.SpecificationBuilder;
import vn.mpl.one.modules.lesson.infrastructure.specification.LessonSpecifications;
import static vn.mpl.one.core.filter.FilterOperator.*;

class CoreQueryTest {
  @Test void repeatedPredicatesUseAndAndPreserveColons() {
    var criteria = FilterParser.parse(List.of("title:like:math&&title:neq:math:advanced"), LessonSpecifications.FILTERS);
    assertThat(criteria.rules()).hasSize(2);
    assertThat(criteria.rules().get(1).value()).isEqualTo("math:advanced");
  }
  @ParameterizedTest @ValueSource(strings={"organizationId:eq:123", "title:invalid:x", "title:eq",
      "title:isnull:x", "title:like:", "owner.name:eq:x", "title:eq:a&&", "status:like:DRAFT", "title:gt:a"})
  void rejectsInvalidOrDisallowedFilters(String value) {
    assertThatThrownBy(() -> FilterParser.parse(List.of(value), LessonSpecifications.FILTERS)).isInstanceOf(InvalidFilterException.class);
  }
  @Test void nullOperatorAliasesRemainCompatible() {
    var rules = FilterParser.parse(List.of("description:isnull", "description:is_not_null", "description:notnull"), LessonSpecifications.FILTERS).rules();
    assertThat(rules).extracting(FilterRule::operator).containsExactly(IS_NULL, IS_NOT_NULL, IS_NOT_NULL);
  }
  @ParameterizedTest @ValueSource(strings={"version:eq:no", "version:eq:9223372036854775808", "createdAt:eq:yesterday", "status:eq:UNKNOWN", "status:in:DRAFT,"})
  void rejectsInvalidTypedValues(String value) {
    var criteria = FilterParser.parse(List.of(value), LessonSpecifications.FILTERS);
    assertThatThrownBy(() -> SpecificationBuilder.build(criteria, LessonSpecifications.FILTERS)).isInstanceOf(InvalidFilterException.class);
  }
  @Test void rejectsMalformedUuidAndDate() {
    var fields = Map.of("id", new FilterField(UUID.class, Set.of(EQ)), "day", new FilterField(LocalDate.class, Set.of(EQ)));
    for (String filter : List.of("id:eq:1-1-1-1-1", "id:eq:not-a-uuid", "day:eq:2026-02-30")) {
      assertThatThrownBy(() -> SpecificationBuilder.build(FilterParser.parse(List.of(filter), fields), fields)).isInstanceOf(InvalidFilterException.class);
    }
  }
  @Test void specificationRevalidatesManuallyConstructedCriteria() {
    var forged = new FilterCriteria(List.of(new FilterRule("organizationId", EQ, UUID.randomUUID().toString())));
    assertThatThrownBy(() -> SpecificationBuilder.build(forged, LessonSpecifications.FILTERS)).isInstanceOf(InvalidFilterException.class);
  }
  @Test void paginationDefaultsAndStableSort() {
    assertThat(PaginationRequest.defaults().page()).isEqualTo(1);
    var page = PaginationUtils.pageable(new PaginationRequest(2, 100, List.of("title:asc")), Set.of("title"));
    assertThat(page.getOffset()).isEqualTo(100);
    assertThat(page.getSort().getOrderFor("id")).isNotNull();
    var byId = PaginationUtils.pageable(new PaginationRequest(1, 10, List.of("id:desc")), Set.of("id"));
    assertThat(byId.getSort().stream().count()).isEqualTo(1);
  }
  @Test void rejectsPageBounds() {
    assertThatThrownBy(() -> new PaginationRequest(0, 10, null)).isInstanceOf(BadRequestException.class);
    assertThatThrownBy(() -> new PaginationRequest(1, 101, null)).isInstanceOf(BadRequestException.class);
    assertThatThrownBy(() -> new PaginationRequest(1, 0, null)).isInstanceOf(BadRequestException.class);
  }
  @ParameterizedTest @ValueSource(strings={"organizationId:asc", "owner.name:asc", "title:sideways", "title", "title:asc:desc", "title:asc&&title:desc"})
  void rejectsUnknownOrDuplicateSort(String value) {
    assertThatThrownBy(() -> PaginationUtils.pageable(new PaginationRequest(1, 10, List.of(value)), Set.of("title"))).isInstanceOf(BadRequestException.class);
  }
}
