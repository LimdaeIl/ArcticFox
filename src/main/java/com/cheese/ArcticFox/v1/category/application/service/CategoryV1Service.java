package com.cheese.ArcticFox.v1.category.application.service;

import com.cheese.ArcticFox.v1.category.application.dto.request.CreateCategoryRequest;
import com.cheese.ArcticFox.v1.category.application.dto.request.UpdateCategoryRequest;
import com.cheese.ArcticFox.v1.category.application.dto.response.CreateCategoryResponse;
import com.cheese.ArcticFox.v1.category.application.dto.response.GetCategoryResponse;
import com.cheese.ArcticFox.v1.category.application.dto.response.GetCategoryResponse.CategoryNodeResponse;
import com.cheese.ArcticFox.v1.category.application.dto.response.UpdateCategoryResponse;
import com.cheese.ArcticFox.v1.category.domain.entity.CategoryV1;
import com.cheese.ArcticFox.v1.category.domain.entity.CategoryV1Closure;
import com.cheese.ArcticFox.v1.category.domain.entity.CategoryV1ClosureId;
import com.cheese.ArcticFox.v1.category.domain.repository.CategoryV1ClosureRepository;
import com.cheese.ArcticFox.v1.category.domain.repository.CategoryV1Repository;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class CategoryV1Service {

    private final CategoryV1Repository categoryV1Repository;
    private final CategoryV1ClosureRepository categoryV1ClosureRepository;

    private static final int MAX_DEPTH = 3;


    private void existsByName(String name) {
        if (categoryV1Repository.existsByName(name)) {
            throw new IllegalArgumentException("Category already exists with name " + name);
        }
    }

    @Transactional
    public CreateCategoryResponse create(CreateCategoryRequest request) {
        // 0) 요청 검증 + 1) 중복 검사
        String trimCategoryName = request.name().trim();
        existsByName(trimCategoryName);

        // 2) 카테고리 저장
        CategoryV1 savedCategory = categoryV1Repository.save(CategoryV1.create(trimCategoryName));

        // 3) 자기 경로 삽입 (N,N,0)
        CategoryV1Closure self = CategoryV1Closure.create(
                CategoryV1ClosureId.create(savedCategory.getId(), savedCategory.getId()),
                0, savedCategory, savedCategory
        );
        CategoryV1Closure savedClosure = categoryV1ClosureRepository.save(self);
        CategoryV1Closure responseClosure = savedClosure;

        // 4) 부모가 있으면: 깊이 체크 → 링크 생성
        if (request.parentId() != null) {
            CategoryV1 parent = categoryV1Repository.findById(request.parentId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Parent category not found with id " + request.parentId()));

            int parentDepth = categoryV1ClosureRepository.findDepthOf(parent.getId());
            int newChildDepth = parentDepth + 1;
            if (newChildDepth > MAX_DEPTH) {
                throw new IllegalStateException(
                        "카테고리 깊이는 최대 "
                                + MAX_DEPTH + "단계까지 허용됩니다. "
                                + "(부모=" + parent.getName()
                                + ", 부모깊이="
                                + parentDepth
                                + ", 새깊이="
                                + newChildDepth
                                + ")"
                );
            }

            // 4-1) 부모의 모든 조상 경로 가져와서 (a, N, depth(a→P)+1) 삽입
            List<CategoryV1Closure> ancestorsOfParent =
                    categoryV1ClosureRepository.findByDescendant_Id(parent.getId());

            List<CategoryV1Closure> newLinks = getCategoryV1Closures(ancestorsOfParent,
                    savedCategory);
            categoryV1ClosureRepository.saveAll(newLinks);

            responseClosure = newLinks.stream()
                    .filter(c -> c.getId().getAncestorId().equals(parent.getId()))
                    .findFirst()
                    .orElse(savedClosure);
        }

        // 5) 응답 생성
        return CreateCategoryResponse.from(savedCategory, responseClosure);
    }

    private static List<CategoryV1Closure> getCategoryV1Closures(
            List<CategoryV1Closure> byDescendantId, CategoryV1 newChild) {

        List<CategoryV1Closure> parents = new ArrayList<>();
        for (CategoryV1Closure categoryV1Closure : byDescendantId) {
            // (a, N, depth(a→P)+1)
            CategoryV1Closure parent = CategoryV1Closure.create(
                    CategoryV1ClosureId.create(
                            categoryV1Closure.getId().getAncestorId(), // a
                            newChild.getId()                           // N
                    ),
                    categoryV1Closure.getLevel() + 1,            // d(a→P)+1
                    categoryV1Closure.getAncestor(),                  // a 엔티티
                    newChild                                          // N 엔티티
            );
            parents.add(parent);
        }
        return parents;
    }

    @Transactional(readOnly = true)
    public GetCategoryResponse get(String path) {
        List<CategoryNodeResponse> roots = mapToNodeResponse(categoryV1Repository.findRoots());

        if (path == null || path.isBlank()) {
            return GetCategoryResponse.of(roots, List.of(), List.of(), List.of());
        }

        String normalized = normalize(path);
        if (normalized.isEmpty()) { // "/", " // " 방어
            return GetCategoryResponse.of(roots, List.of(), List.of(), List.of());
        }

        String[] names = normalized.split("/");
        List<CategoryV1> trail = new ArrayList<>(names.length);
        for (String name : names) {
            CategoryV1 node = categoryV1Repository.findByName(name)
                    .orElseThrow(() -> new IllegalArgumentException("Category not found: " + name));
            trail.add(node);
        }

        // (선택) 부모-자식 직계 검증
        for (int i = 1; i < trail.size(); i++) {
            if (!categoryV1ClosureRepository
                    .existsDirectLink(trail.get(i - 1).getId(), trail.get(i).getId())) {
                throw new IllegalArgumentException(
                        "Invalid path: "
                                + trail.get(i - 1).getName()
                                + " -> "
                                + trail.get(i).getName()
                );
            }
        }

        CategoryV1 first = trail.getFirst();
        List<CategoryNodeResponse> firstChildren =
                mapToNodeResponse(categoryV1ClosureRepository.findDirectChildren(first.getId()));

        List<CategoryNodeResponse> lastChildren =
                (trail.size() >= 2)
                        ? mapToNodeResponse(
                        categoryV1ClosureRepository.findDirectChildren(trail.getLast().getId()))
                        : List.of();

        List<CategoryNodeResponse> breadcrumb = mapToNodeResponse(trail);

        return GetCategoryResponse.of(roots, firstChildren, lastChildren, breadcrumb);
    }


    private static List<CategoryNodeResponse> mapToNodeResponse(List<CategoryV1> list) {
        return list.stream()
                .map(categoryV1 ->
                        CategoryNodeResponse.of(
                                categoryV1.getId(),
                                categoryV1.getName()
                        )
                ).toList();
    }

    private static String normalize(String rowPath) {
        String path = rowPath.trim();
        path = path.replaceAll("^/+|/+$", "");
        path = path.replaceAll("/+", "/");
        return path;
    }

    @Transactional
    public UpdateCategoryResponse update(Long categoryId, UpdateCategoryRequest request) {
        existsByName(request.newName());

        CategoryV1 categoryV1 = categoryV1Repository.findById(categoryId)
                .orElseThrow(
                        () -> new IllegalArgumentException("Category not found: " + categoryId));

        categoryV1Repository.existsByName(request.newName());
        categoryV1.updateName(request.newName());

        return UpdateCategoryResponse.from(categoryV1);
    }

    @Transactional
    public void delete(Long categoryId) {
        CategoryV1 category = categoryV1Repository.findById(categoryId)
                .orElseThrow(
                        () -> new IllegalArgumentException("Category not found: id=" + categoryId));

        if (categoryV1ClosureRepository.existsDirectChild(categoryId)) {
            List<String> childNames = categoryV1ClosureRepository.findDirectChildren(categoryId)
                    .stream().map(CategoryV1::getName).toList();
            throw new IllegalStateException(
                    "Only leaf categories can be deleted. Children: " + String.join(", ",
                            childNames));
        }

        categoryV1ClosureRepository.deleteAllLinksOf(categoryId);
        categoryV1Repository.delete(category);
    }
}
