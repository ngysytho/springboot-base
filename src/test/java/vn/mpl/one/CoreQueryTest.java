package vn.mpl.one;

import static org.assertj.core.api.Assertions.*;

import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import vn.mpl.one.core.exception.BadRequestException;
import vn.mpl.one.core.filter.*;
import vn.mpl.one.core.pagination.PaginationRequest;
import vn.mpl.one.core.specification.SpecificationBuilder;

class CoreQueryTest {
  @Test
  void parsesRepeatedAndFiltersAndColonInValue() {
    var c =
        FilterParser.parse(List.of("title:like:math&&title:neq:math:advanced"), Set.of("title"));
    assertThat(c.rules()).hasSize(2);
    assertThat(c.rules().get(1).value()).isEqualTo("math:advanced");
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "organizationId:eq:123",
        "title:invalid:x",
        "title:eq",
        "title:isnull:x",
        "title:like:",
        "owner.name:eq:x",
        "title:eq:a&&"
      })
  void rejectsInvalidFilters(String value) {
    assertThatThrownBy(() -> FilterParser.parse(List.of(value), Set.of("title")))
        .isInstanceOf(BadRequestException.class);
  }

  @Test
  void nullOperatorsNeedNoValue() {
    assertThat(
            FilterParser.parse(List.of("title:isnull", "title:notnull"), Set.of("title")).rules())
        .hasSize(2);
  }

  @Test
  void validatesTypesBeforeDatabase() {
    var criteria = FilterParser.parse(List.of("version:eq:no"), Set.of("version"));
    assertThatThrownBy(() -> SpecificationBuilder.build(criteria, Map.of("version", Long.class)))
        .isInstanceOf(BadRequestException.class);
  }

  @Test
  void paginationUsesOneBasedBoundaryAndStableSort() {
    var p = new PaginationRequest(2, 100, List.of("title:asc")).pageable(Set.of("title"));
    assertThat(p.getPageNumber()).isEqualTo(1);
    assertThat(p.getOffset()).isEqualTo(100);
    assertThat(p.getSort().getOrderFor("id")).isNotNull();
  }

  @Test
  void rejectsPageBounds() {
    assertThatThrownBy(() -> new PaginationRequest(0, 10, null))
        .isInstanceOf(BadRequestException.class);
    assertThatThrownBy(() -> new PaginationRequest(1, 101, null))
        .isInstanceOf(BadRequestException.class);
    assertThatThrownBy(() -> new PaginationRequest(1, 0, null))
        .isInstanceOf(BadRequestException.class);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "organizationId:asc",
        "owner.name:asc",
        "title:sideways",
        "title",
        "title:asc:desc"
      })
  void rejectsSorts(String sort) {
    assertThatThrownBy(() -> new PaginationRequest(1, 10, List.of(sort)).pageable(Set.of("title")))
        .isInstanceOf(BadRequestException.class);
  }
}
