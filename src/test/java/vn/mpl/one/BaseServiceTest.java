package vn.mpl.one;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import vn.mpl.one.core.exception.NotFoundException;
import vn.mpl.one.core.service.BaseService;
import vn.mpl.one.modules.lesson.domain.Lesson;
import vn.mpl.one.modules.lesson.infrastructure.LessonRepository;

class BaseServiceTest {
  LessonRepository repo;
  BaseService<Lesson> core;
  Specification<Lesson> scope;

  @BeforeEach
  void setup() {
    repo = mock(LessonRepository.class);
    core = new BaseService<>(repo);
    scope = (r, q, b) -> b.conjunction();
  }

  @Test
  void missingScopedResourceFails() {
    when(repo.findOne(scope)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> core.findOneOrFailByCriteria(scope))
        .isInstanceOf(NotFoundException.class);
  }

  @Test
  void updateLoadsByScopeAndFlushes() {
    var lesson = new Lesson(UUID.randomUUID(), "Before", null);
    when(repo.findOne(scope)).thenReturn(Optional.of(lesson));
    when(repo.saveAndFlush(lesson)).thenReturn(lesson);
    assertThat(core.update(scope, e -> e.edit("After", null)).getTitle()).isEqualTo("After");
    verify(repo).saveAndFlush(lesson);
  }

  @Test
  void deleteMissingDoesNotDelete() {
    when(repo.findOne(scope)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> core.delete(scope)).isInstanceOf(NotFoundException.class);
    verify(repo, never()).delete(any(Lesson.class));
  }

  @Test
  void paginatePreservesScopeAndPage() {
    var page = PageRequest.of(1, 10);
    when(repo.findAll(scope, page)).thenReturn(Page.empty(page));
    assertThat(core.paginate(scope, page).getNumber()).isEqualTo(1);
    verify(repo).findAll(scope, page);
  }

  @Test
  void delegatesExistenceCountAndBulkCreate() {
    when(repo.exists(scope)).thenReturn(true);
    when(repo.count(scope)).thenReturn(4L);
    assertThat(core.exists(scope)).isTrue();
    assertThat(core.count(scope)).isEqualTo(4);
    var rows = List.of(new Lesson(UUID.randomUUID(), "Math", null));
    when(repo.saveAllAndFlush(rows)).thenReturn(rows);
    assertThat(core.bulkCreate(rows)).isEqualTo(rows);
  }
}
