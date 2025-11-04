package com.cheese.ArcticFox.v1.product.application.service;

import com.cheese.ArcticFox.v1.category.application.dto.request.CreateCategoryRequest;
import com.cheese.ArcticFox.v1.category.application.service.CategoryV1Service;
import com.cheese.ArcticFox.v1.category.domain.entity.CategoryV1;
import com.cheese.ArcticFox.v1.category.domain.repository.CategoryV1Repository;
import com.cheese.ArcticFox.v1.product.domain.entitiy.ProductV1;
import com.cheese.ArcticFox.v1.product.domain.repository.ProductV1Repository;
import jakarta.persistence.EntityManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.LongAdder;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;


@Slf4j
@SpringBootTest
public class ProductBulkInsertTest {

    private static final String[] PATHS = {
            "의류/상의/맨투맨",
            "의류/상의/셔츠",
            "식품/가공제품/치즈",
            "식품/음료/커피"
    };

    private static final int PER_CATEGORY = 1;
    private static final int THREADS = 32;
    private static final int TASK_CHUNK = 1;
    private static final int FLUSH_UNIT = 1;

    @Autowired
    private CategoryV1Service categoryV1Service;
    @Autowired
    private CategoryV1Repository categoryV1Repository;
    @Autowired
    private ProductV1Repository productV1Repository;
    @Autowired
    private PlatformTransactionManager tm;
    @Autowired
    private EntityManager em;

    private final Map<String, LongAdder> categoryInserted = new ConcurrentHashMap<>();

    @DisplayName("카테고리별 삽입 결과 + 전체 소요시간만 요약 출력")
    @Test
    void bulkInsert_parallel_minimal_summary() throws InterruptedException {
        List<String> leafNames = ensureCategoryPaths();

        List<Runnable> tasks = new ArrayList<>();
        for (String leaf : leafNames) {
            int total = PER_CATEGORY;
            int loops = (int) Math.ceil((double) total / TASK_CHUNK);
            for (int i = 0; i < loops; i++) {
                final int startIndex = i * TASK_CHUNK;
                final int count = Math.min(TASK_CHUNK, total - startIndex);
                tasks.add(() -> {
                    boolean ok = writeBatchWithNewTx(leaf, startIndex, count);
                    if (ok) {
                        categoryInserted.computeIfAbsent(leaf, k -> new LongAdder()).add(count);
                    }
                });
            }
        }

        long t0 = System.nanoTime();

        ExecutorService pool = Executors.newFixedThreadPool(THREADS);
        CountDownLatch latch = new CountDownLatch(tasks.size());
        for (Runnable task : tasks) {
            pool.submit(() -> {
                try {
                    task.run();
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await();
        pool.shutdown();

        long elapsedMs = (System.nanoTime() - t0) / 1_000_000L;

        // === 최종 요약: 카테고리별 committed/dbCount + 총 소요시간만 ===
        StringBuilder sb = new StringBuilder(256);
        sb.append("\n-- Per Category --\n");
        for (String leaf : leafNames) {
            long committed = categoryInserted.getOrDefault(leaf, new LongAdder()).sum();
            long dbCount = countProductsByCategoryName(leaf);
            sb.append(
                    String.format(" - [%s] committed=%d, dbCount=%d%n", leaf, committed, dbCount));
        }
        sb.append(String.format("%nTotal elapsed: %d ms%n", elapsedMs));

        log.info("sb.toString(): {}", sb);
    }

    // ===== 이하 유틸/배치 로직은 그대로 =====

    private List<String> ensureCategoryPaths() {
        List<String> leaves = new ArrayList<>(PATHS.length);
        for (String raw : PATHS) {
            String[] segs = normalize(raw).split("/");
            Long parentId = null;
            for (String name : segs) {
                Optional<CategoryV1> found = categoryV1Repository.findByName(name);
                if (found.isPresent()) {
                    parentId = found.get().getId();
                } else {
                    parentId = categoryV1Service.create(new CreateCategoryRequest(name, parentId))
                            .id();
                }
            }
            leaves.add(segs[segs.length - 1]);
        }
        return leaves;
    }

    private static String normalize(String s) {
        String p = s.trim();
        p = p.replaceAll("^/+|/+$", "");
        p = p.replaceAll("/+", "/");
        return p;
    }

    private boolean writeBatchWithNewTx(String leafCategoryName, int startIndex, int count) {
        TransactionTemplate tt = new TransactionTemplate(tm);
        tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return Boolean.TRUE.equals(tt.execute(status -> {
            doWriteBatch(leafCategoryName, startIndex, count);
            return true;
        }));
    }

    private void doWriteBatch(String leafCategoryName, int startIndex, int count) {
        CategoryV1 category = categoryV1Repository.findByName(leafCategoryName)
                .orElseThrow(() -> new IllegalArgumentException(
                        "category not found: " + leafCategoryName));

        List<ProductV1> buffer = new ArrayList<>(Math.min(count, FLUSH_UNIT));
        int end = startIndex + count;

        for (int i = startIndex; i < end; i++) {
            ProductV1 p = ProductV1.create(
                    "상품-" + leafCategoryName + "-" + i,
                    "대량삽입(" + leafCategoryName + ") 테스트 " + i,
                    1000 + (i % 100),
                    100 + (i % 50)
            );
            p.addCategory(category);
            buffer.add(p);

            if (buffer.size() >= FLUSH_UNIT) {
                productV1Repository.saveAll(buffer);
                em.flush();
                em.clear();
                buffer.clear();
            }
        }
        if (!buffer.isEmpty()) {
            productV1Repository.saveAll(buffer);
            em.flush();
            em.clear();
            buffer.clear();
        }
    }

    private long countProductsByCategoryName(String categoryName) {
        return em.createQuery("""
                        select count(p)
                          from ProductV1 p
                          join p.productCategoriesV1 pc
                          join pc.category c
                         where c.name = :name
                        """, Long.class)
                .setParameter("name", categoryName)
                .getSingleResult();
    }
}
