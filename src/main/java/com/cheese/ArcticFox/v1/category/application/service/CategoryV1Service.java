package com.cheese.ArcticFox.v1.category.application.service;

import com.cheese.ArcticFox.v1.category.application.dto.request.CreateCategoryRequest;
import com.cheese.ArcticFox.v1.category.application.dto.response.CreateCategoryResponse;
import com.cheese.ArcticFox.v1.category.application.dto.response.GetCategoryResponse;
import com.cheese.ArcticFox.v1.category.application.dto.response.GetCategoryResponse.CategoryNodeResponse;
import com.cheese.ArcticFox.v1.category.domain.entity.CategoryV1;
import com.cheese.ArcticFox.v1.category.domain.entity.CategoryV1Closure;
import com.cheese.ArcticFox.v1.category.domain.entity.CategoryV1ClosureId;
import com.cheese.ArcticFox.v1.category.domain.repository.CategoryV1ClosureRepository;
import com.cheese.ArcticFox.v1.category.domain.repository.CategoryV1Repository;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class CategoryV1Service {

    private final CategoryV1Repository categoryV1Repository;
    private final CategoryV1ClosureRepository categoryV1ClosureRepository;

    private void existsByName(String name) {
        if (categoryV1Repository.existsByName(name)) {
            throw new IllegalArgumentException("Category already exists with name " + name);
        }
    }

    @Transactional
    public CreateCategoryResponse create(CreateCategoryRequest request) {
        // 카테고리명 공백, null, 중복 여부 검사
        String trimCategoryName = request.name().trim();
        existsByName(trimCategoryName);

        // 카테고리 save()
        CategoryV1 savedCategory = categoryV1Repository.save(CategoryV1.create(trimCategoryName));

        // 자기 자신의 경로 생성
        CategoryV1Closure closure = CategoryV1Closure.create(
                CategoryV1ClosureId.create(
                        savedCategory.getId(),
                        savedCategory.getId()
                ),
                0,
                savedCategory,
                savedCategory
        );

        // closure 저장
        CategoryV1Closure savedClosure = categoryV1ClosureRepository.save(closure);
        CategoryV1Closure responseClosure = savedClosure; // 기본값: (N,N,0)

        if (request.parentId() != null) {
            CategoryV1 parent = categoryV1Repository.findById(request.parentId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Parent category not found with id " + request.parentId()));

            List<CategoryV1Closure> ancestorsOfParent =
                    categoryV1ClosureRepository.findByDescendant_Id(parent.getId());

            List<CategoryV1Closure> newLinks = getCategoryV1Closures(ancestorsOfParent,
                    savedCategory);
            categoryV1ClosureRepository.saveAll(newLinks);

            responseClosure = newLinks.stream()
                    .filter(c -> c.getId().getAncestorId().equals(parent.getId()))
                    .findFirst()
                    .orElse(savedClosure); // 안전장치
        }

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

        if (roots.isEmpty() || path == null) {
            return GetCategoryResponse.of(
                    roots,
                    Collections.emptyList(),
                    Collections.emptyList(),
                    Collections.emptyList()
            );
        }

        String[] names = normalize(path).split("/");
        List<CategoryV1> trail = new ArrayList<>();

        for (String name : names) {
            Optional<CategoryV1> byName = categoryV1Repository.findByName(name);

            if (byName.isEmpty()) {
                throw new IllegalArgumentException("Category not found with name " + name);
            }
            trail.add(byName.get());
        }

        CategoryV1 trailFirst = trail.getFirst();
        List<CategoryNodeResponse> directChildren = mapToNodeResponse(
                categoryV1ClosureRepository.findDirectChildren(trailFirst.getId()));
        List<CategoryNodeResponse> descendants;

        if (trail.size() >= 2) {
            CategoryV1 last = trail.getLast();
            descendants = mapToNodeResponse(
                    categoryV1ClosureRepository.findDirectChildren(last.getId()));
        } else {
            descendants = Collections.emptyList();
        }
        List<CategoryNodeResponse> breadcrumb = mapToNodeResponse(trail);

        return GetCategoryResponse.of(roots, directChildren, descendants, breadcrumb);
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
}
