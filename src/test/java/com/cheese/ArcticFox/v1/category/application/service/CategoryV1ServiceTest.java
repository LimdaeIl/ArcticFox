package com.cheese.ArcticFox.v1.category.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cheese.ArcticFox.v1.category.application.dto.request.CreateCategoryRequest;
import com.cheese.ArcticFox.v1.category.application.dto.response.CreateCategoryResponse;
import com.cheese.ArcticFox.v1.category.application.dto.response.GetCategoryResponse;
import com.cheese.ArcticFox.v1.category.domain.entity.CategoryV1;
import com.cheese.ArcticFox.v1.category.domain.entity.CategoryV1Closure;
import com.cheese.ArcticFox.v1.category.domain.entity.CategoryV1ClosureId;
import com.cheese.ArcticFox.v1.category.domain.repository.CategoryV1ClosureRepository;
import com.cheese.ArcticFox.v1.category.domain.repository.CategoryV1Repository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@DisplayName("카테고리 서비스")
@ExtendWith(MockitoExtension.class)
class CategoryV1ServiceTest {

    @InjectMocks
    private CategoryV1Service categoryV1Service;

    @Mock
    private CategoryV1Repository categoryV1Repository;

    @Mock
    private CategoryV1ClosureRepository categoryV1ClosureRepository;

    // 공통 변수
    private String categoryName;
    private CreateCategoryRequest request;
    private CategoryV1 savedCategory;
    private CategoryV1Closure closure;

    @BeforeEach
    void beforeEach() {
        categoryName = "Electronics";
        savedCategory = CategoryV1.create(categoryName);

        // PK 생성은 AUTO INCREMENT 입니다. 따라서 테스트에서는 'ReflectionTestUtils' 를 사용해 강제로 ID를 주입합니다.
        ReflectionTestUtils.setField(savedCategory, "id", 1L);

        CategoryV1ClosureId closureId = CategoryV1ClosureId.create(1L, 1L);
        closure = CategoryV1Closure.create(closureId, 0, savedCategory, savedCategory); // 자기 자신의 경로
    }

    @DisplayName("성공: 루트 카테고리 생성(parent = null)")
    @Test
    public void create_root_category_success() {
        // given
        request = new CreateCategoryRequest(categoryName, null);
        when(categoryV1Repository.existsByName(categoryName)).thenReturn(false);
        when(categoryV1Repository.save(any(CategoryV1.class))).thenReturn(savedCategory);
        when(categoryV1ClosureRepository.save(any(CategoryV1Closure.class))).thenReturn(closure);

        // when
        CreateCategoryResponse response = categoryV1Service.create(request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(savedCategory.getId());
        assertThat(response.name()).isEqualTo(categoryName);
        assertThat(response.level()).isEqualTo(0);

        // Mock 객체가 예상대로 호출되었는지 검증
        verify(categoryV1Repository).existsByName(categoryName);
        verify(categoryV1Repository).save(any(CategoryV1.class));
        verify(categoryV1ClosureRepository).save(any(CategoryV1Closure.class));

        // 부모가 없으므로 이 메서드들은 호출하면 안 됨
        verify(categoryV1Repository, never()).findById(any());
        verify(categoryV1ClosureRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("성공: 하위 카테고리 생성 (parentId != null)")
    void create_child_category_success() {
        // === Given (준비) ===
        long parentId = 1L;
        long childId = 2L;
        String parentName = "Electronics";
        String childName = "Mobile";

        // 1. 부모 엔티티 준비
        CategoryV1 parentCategory = CategoryV1.create(parentName);
        ReflectionTestUtils.setField(parentCategory, "id", parentId);

        // 2. 새로 생성될 자식 엔티티 준비
        CategoryV1 childCategory = CategoryV1.create(childName);
        ReflectionTestUtils.setField(childCategory, "id", childId);

        // 3. 자식의 자기 참조 Closure 준비
        CategoryV1ClosureId childSelfId = CategoryV1ClosureId.create(childId, childId);
        CategoryV1Closure childSelfClosure = CategoryV1Closure.create(childSelfId, 0, childCategory,
                childCategory);

        // 4. 부모의 조상 경로 준비 (부모는 루트이므로, 자기 자신만 포함)
        CategoryV1ClosureId parentSelfId = CategoryV1ClosureId.create(parentId, parentId);
        CategoryV1Closure parentAncestor = CategoryV1Closure.create(parentSelfId, 0, parentCategory,
                parentCategory);
        List<CategoryV1Closure> parentAncestors = List.of(parentAncestor);

        // 5. 생성 요청 DTO
        CreateCategoryRequest childRequest = new CreateCategoryRequest(childName, parentId);

        // Mocking 설정
        when(categoryV1Repository.existsByName(childName)).thenReturn(false);
        when(categoryV1Repository.save(any(CategoryV1.class))).thenReturn(childCategory);
        when(categoryV1ClosureRepository.save(any(CategoryV1Closure.class))).thenReturn(
                childSelfClosure);
        when(categoryV1Repository.findById(parentId)).thenReturn(Optional.of(parentCategory));
        when(categoryV1ClosureRepository.findDepthOf(parentId)).thenReturn(0); // 부모 깊이 = 0
        when(categoryV1ClosureRepository.findByDescendant_Id(parentId)).thenReturn(parentAncestors);

        // === When (실행) ===
        CreateCategoryResponse response = categoryV1Service.create(childRequest);

        // === Then (검증) ===
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(childId);
        assertThat(response.name()).isEqualTo(childName);
        assertThat(response.level()).isEqualTo(1); // 부모(0) + 1 = 1

        // 부모-자식 관계 링크가 잘 저장되었는지 검증 (saveAll이 호출되었는지)
        verify(categoryV1ClosureRepository).saveAll(anyList());

        // saveAll에 넘어간 인자(List)를 캡처해서 내용물 확인
        ArgumentCaptor<List<CategoryV1Closure>> captor = ArgumentCaptor.forClass(List.class);
        verify(categoryV1ClosureRepository).saveAll(captor.capture());

        List<CategoryV1Closure> capturedLinks = captor.getValue();
        assertThat(capturedLinks).hasSize(1); // 조상이 부모 자신 1개
        CategoryV1Closure newLink = capturedLinks.get(0);
        assertThat(newLink.getId().getAncestorId()).isEqualTo(parentId);
        assertThat(newLink.getId().getDescendantId()).isEqualTo(childId);
        assertThat(newLink.getLevel()).isEqualTo(1);
    }

    @Test
    @DisplayName("실패: 카테고리 이름 중복")
    public void create_category_fail_duplicate_name() {
        // given
        request = new CreateCategoryRequest(categoryName, null);

        when(categoryV1Repository.existsByName(categoryName)).thenReturn(true);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> categoryV1Service.create(request)
        );

        assertThat(exception.getMessage()).isEqualTo(
                "Category already exists with name " + categoryName);

        verify(categoryV1Repository, never()).save(any(CategoryV1.class));
        verify(categoryV1ClosureRepository, never()).save(any(CategoryV1Closure.class));
    }

    @DisplayName("실패: 존재하지 않는 부모 카테고리 ID")
    @Test
    void create_child_category_fail_parent_not_found() {
        // given
        long nonExistentParentId = 999L;
        request = new CreateCategoryRequest(categoryName, nonExistentParentId);

        when(categoryV1Repository.existsByName(categoryName)).thenReturn(false);
        when(categoryV1Repository.save(any(CategoryV1.class))).thenReturn(savedCategory);
        when(categoryV1ClosureRepository.save(any(CategoryV1Closure.class))).thenReturn(closure);

        when(categoryV1Repository.findById(nonExistentParentId)).thenReturn(Optional.empty());

        // when & then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> categoryV1Service.create(request)
        );

        assertThat(exception.getMessage()).contains(
                "Parent category not found with id " + nonExistentParentId);
    }

    @DisplayName("실패: 최대 허용 깊이(MAX_DEPTH=3) 초과")
    @Test
    void create_child_category_fail_exceeds_max_depth() {
        // given
        long parentId = 1L;
        long childId = 2L;
        String parentName = "Depth3_category";
        String childName = "Depth4_category";
        int maxDepth = 3;

        CategoryV1 parentCategory = CategoryV1.create(parentName);
        ReflectionTestUtils.setField(parentCategory, "id", parentId);
        CategoryV1 childCategory = CategoryV1.create(childName);
        ReflectionTestUtils.setField(childCategory, "id", childId);

        CategoryV1ClosureId childClosureId = CategoryV1ClosureId.create(childId, childId);
        CategoryV1Closure childClosure = CategoryV1Closure.create(childClosureId, 0, childCategory,
                childCategory);

        CreateCategoryRequest childRequest = new CreateCategoryRequest(childName, parentId);

        when(categoryV1Repository.existsByName(childName)).thenReturn(false);
        when(categoryV1Repository.save(any(CategoryV1.class))).thenReturn(childCategory);
        when(categoryV1ClosureRepository.save(any(CategoryV1Closure.class))).thenReturn(
                childClosure);
        when(categoryV1Repository.findById(parentId)).thenReturn(Optional.of(parentCategory));

        when(categoryV1ClosureRepository.findDepthOf(parentId)).thenReturn(maxDepth);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> categoryV1Service.create(childRequest)
        );

        assertThat(exception.getMessage()).contains("카테고리 깊이는 최대 3단계까지 허용됩니다.");
    }

    @DisplayName("성공: 모든 루트 카테고리 조회")
    @Test
    public void get_all_root_category() {
        // given
        CategoryV1 root1 = CategoryV1.create("Electronics");
        ReflectionTestUtils.setField(root1, "id", root1.getId());
        CategoryV1 root2 = CategoryV1.create("Fashion");
        ReflectionTestUtils.setField(root2, "id", root2.getId());

        when(categoryV1Repository.findRoots()).thenReturn(List.of(root1, root2));

        // when
        GetCategoryResponse response = categoryV1Service.get(null);

        // then
        assertThat(response).isNotNull();
        assertThat(response.roots()).hasSize(2);
        assertThat(response.roots().get(0).name()).isEqualTo("Electronics");
        assertThat(response.roots().get(1).name()).isEqualTo("Fashion");

        assertThat(response.directChildren()).isEmpty();
        assertThat(response.descendants()).isEmpty();
        assertThat(response.breadcrumb()).isEmpty();

        verify(categoryV1Repository).findRoots();
        verify(categoryV1Repository, never()).findByName(any());
        verify(categoryV1ClosureRepository, never()).findDirectChildren(any());
        verify(categoryV1ClosureRepository, never()).existsDirectLink(any(), any());
    }

    @DisplayName("성공: 모든 루트+직계 카테고리 조회")
    @Test
    public void get_all_root_direct_category() {
        // given
        CategoryV1 root1 = CategoryV1.create("Electronics");
        ReflectionTestUtils.setField(root1, "id", 1L);
        CategoryV1 root2 = CategoryV1.create("Fashion");
        ReflectionTestUtils.setField(root2, "id", 2L);

        when(categoryV1Repository.findRoots()).thenReturn(List.of(root1, root2));
        when(categoryV1Repository.findByName("Electronics")).thenReturn(Optional.of(root1));

        CategoryV1 child1 = CategoryV1.create("Mobile");
        ReflectionTestUtils.setField(child1, "id", 10L);
        CategoryV1 child2 = CategoryV1.create("Laptop");
        ReflectionTestUtils.setField(child2, "id", 11L);

        when(categoryV1ClosureRepository.findDirectChildren(1L))
                .thenReturn(List.of(child1, child2));

        // when
        GetCategoryResponse response = categoryV1Service.get("Electronics");

        // then
        assertThat(response).isNotNull();
        assertThat(response.roots()).extracting(GetCategoryResponse.CategoryNodeResponse::name)
                .containsExactly("Electronics", "Fashion");
        assertThat(response.directChildren()).extracting(
                        GetCategoryResponse.CategoryNodeResponse::name)
                .containsExactly("Mobile", "Laptop");
        assertThat(response.descendants()).isEmpty();
        assertThat(response.breadcrumb().get(0).name()).isEqualTo("Electronics");

        verify(categoryV1Repository).findRoots();
        verify(categoryV1Repository).findByName("Electronics");
        verify(categoryV1ClosureRepository).findDirectChildren(1L);
        verify(categoryV1ClosureRepository, never()).existsDirectLink(any(), any());

    }

    @DisplayName("성공: 해당 카테고리의 모든 경로 조회")
    @Test
    public void get_root_direct_child_category() {
        // given
        CategoryV1 root = CategoryV1.create("Electronics");
        ReflectionTestUtils.setField(root, "id", 1L);
        when(categoryV1Repository.findRoots()).thenReturn(List.of(root));

        // path: "Electronics/Mobile"
        CategoryV1 child = CategoryV1.create("Mobile");
        ReflectionTestUtils.setField(child, "id", 10L);

        when(categoryV1Repository.findByName("Electronics")).thenReturn(Optional.of(root));
        when(categoryV1Repository.findByName("Mobile")).thenReturn(Optional.of(child));

        // 부모-자식 직계 검증
        when(categoryV1ClosureRepository.existsDirectLink(1L, 10L)).thenReturn(true);

        // firstChildren: 루트의 직계들
        CategoryV1 c1 = CategoryV1.create("Mobile");
        ReflectionTestUtils.setField(c1, "id", 10L);
        CategoryV1 c2 = CategoryV1.create("Laptop");
        ReflectionTestUtils.setField(c2, "id", 11L);
        when(categoryV1ClosureRepository.findDirectChildren(1L))
                .thenReturn(List.of(c1, c2));

        // lastChildren: 마지막 노드(Mobile)의 직계들
        CategoryV1 g1 = CategoryV1.create("Android");
        ReflectionTestUtils.setField(g1, "id", 100L);
        CategoryV1 g2 = CategoryV1.create("iOS");
        ReflectionTestUtils.setField(g2, "id", 101L);
        when(categoryV1ClosureRepository.findDirectChildren(10L))
                .thenReturn(List.of(g1, g2));

        // when
        GetCategoryResponse res = categoryV1Service.get("Electronics/Mobile");

        // then
        assertThat(res).isNotNull();

        // roots
        assertThat(res.roots()).hasSize(1);
        assertThat(res.roots().get(0).name()).isEqualTo("Electronics");

        // firstChildren = root 직계
        assertThat(res.directChildren()).extracting(GetCategoryResponse.CategoryNodeResponse::name)
                .containsExactlyInAnyOrder("Mobile", "Laptop");

        // lastChildren = 마지막 노드(Mobile)의 직계
        assertThat(res.descendants()).extracting(GetCategoryResponse.CategoryNodeResponse::name)
                .containsExactlyInAnyOrder("Android", "iOS");

        // breadcrumb = [Electronics, Mobile]
        assertThat(res.breadcrumb()).hasSize(2);
        assertThat(res.breadcrumb().get(0).name()).isEqualTo("Electronics");
        assertThat(res.breadcrumb().get(1).name()).isEqualTo("Mobile");

        verify(categoryV1Repository).findRoots();
        verify(categoryV1Repository).findByName("Electronics");
        verify(categoryV1Repository).findByName("Mobile");
        verify(categoryV1ClosureRepository).existsDirectLink(1L, 10L);
        verify(categoryV1ClosureRepository).findDirectChildren(1L);
        verify(categoryV1ClosureRepository).findDirectChildren(10L);
    }


    @DisplayName("실패: 경로의 첫 번째 카테고리가 존재하지 않음")
    @Test
    void get_category_fail_not_found_first_segment() {
        // given
        when(categoryV1Repository.findRoots()).thenReturn(List.of());
        when(categoryV1Repository.findByName("Nope")).thenReturn(Optional.empty());

        // when & then
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> categoryV1Service.get("Nope")
        );

        assertThat(ex.getMessage()).isEqualTo("Category not found: Nope");

        verify(categoryV1Repository).findRoots();
        verify(categoryV1Repository).findByName("Nope");
        verify(categoryV1ClosureRepository, never()).existsDirectLink(any(), any());
        verify(categoryV1ClosureRepository, never()).findDirectChildren(any());
    }

    @DisplayName("실패: 경로의 두 번째 카테고리가 존재하지 않음")
    @Test
    void get_category_fail_not_found_second_segment() {
        // given
        CategoryV1 root = CategoryV1.create("Electronics");
        ReflectionTestUtils.setField(root, "id", 1L);

        when(categoryV1Repository.findRoots()).thenReturn(List.of(root));
        when(categoryV1Repository.findByName("Electronics")).thenReturn(Optional.of(root));
        when(categoryV1Repository.findByName("NotExist")).thenReturn(Optional.empty());

        // when & then
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> categoryV1Service.get("Electronics/NotExist")
        );

        assertThat(ex.getMessage()).isEqualTo("Category not found: NotExist");

        verify(categoryV1Repository).findRoots();
        verify(categoryV1Repository).findByName("Electronics");
        verify(categoryV1Repository).findByName("NotExist");
        // 두 번째를 찾지 못해 직계 검증/자식 조회는 호출되지 않아야 한다.
        verify(categoryV1ClosureRepository, never()).existsDirectLink(any(), any());
        verify(categoryV1ClosureRepository, never()).findDirectChildren(any());
    }

    @DisplayName("실패: 경로가 직계(parent->child) 관계가 아님")
    @Test
    void get_category_fail_invalid_direct_link() {
        // given
        CategoryV1 parent = CategoryV1.create("Electronics");
        ReflectionTestUtils.setField(parent, "id", 1L);
        CategoryV1 nonDirect = CategoryV1.create("Tablet");
        ReflectionTestUtils.setField(nonDirect, "id", 20L);

        when(categoryV1Repository.findRoots()).thenReturn(List.of(parent));
        when(categoryV1Repository.findByName("Electronics")).thenReturn(Optional.of(parent));
        when(categoryV1Repository.findByName("Tablet")).thenReturn(Optional.of(nonDirect));

        // 직계가 아니라고 응답
        when(categoryV1ClosureRepository.existsDirectLink(1L, 20L)).thenReturn(false);

        // when & then
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> categoryV1Service.get("Electronics/Tablet")
        );

        assertThat(ex.getMessage()).isEqualTo("Invalid path: Electronics -> Tablet");

        verify(categoryV1Repository).findRoots();
        verify(categoryV1Repository).findByName("Electronics");
        verify(categoryV1Repository).findByName("Tablet");
        verify(categoryV1ClosureRepository).existsDirectLink(1L, 20L);
        // 직계가 아니므로 이후 자식 조회는 일어나지 않아야 한다.
        verify(categoryV1ClosureRepository, never()).findDirectChildren(any());
    }
}
