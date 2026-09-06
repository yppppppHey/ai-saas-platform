package com.aisaas.common.ai.vector;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MemoryVectorStore 单元测试
 */
class MemoryVectorStoreTest {

    private MemoryVectorStore vectorStore;

    @BeforeEach
    void setUp() {
        vectorStore = new MemoryVectorStore();
    }

    @Test
    void testCreateCollection() {
        // 创建集合
        boolean result = vectorStore.createCollection("test_collection", 1536, "cosine");
        assertTrue(result);

        // 验证集合存在
        assertTrue(vectorStore.collectionExists("test_collection"));

        // 重复创建应该失败
        boolean duplicateResult = vectorStore.createCollection("test_collection", 1536, "cosine");
        assertFalse(duplicateResult);
    }

    @Test
    void testDeleteCollection() {
        // 创建并删除集合
        vectorStore.createCollection("delete_test", 1536, "cosine");
        assertTrue(vectorStore.collectionExists("delete_test"));

        boolean result = vectorStore.deleteCollection("delete_test");
        assertTrue(result);
        assertFalse(vectorStore.collectionExists("delete_test"));

        // 删除不存在的集合应该返回false
        boolean notExistResult = vectorStore.deleteCollection("not_exist");
        assertFalse(notExistResult);
    }

    @Test
    void testInsert() {
        vectorStore.createCollection("insert_test", 3, "cosine");

        // 创建测试向量
        List<Double> vector = Arrays.asList(0.1, 0.2, 0.3);
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("content", "test content");
        metadata.put("documentId", "doc123");

        // 插入数据
        boolean result = vectorStore.insert("insert_test", "id1", vector, metadata);
        assertTrue(result);

        // 验证数据存在
        VectorRecord record = vectorStore.getById("insert_test", "id1");
        assertNotNull(record);
        assertEquals("id1", record.getId());
        assertEquals("test content", record.getMetadata().get("content"));
    }

    @Test
    void testInsertDimensionMismatch() {
        vectorStore.createCollection("dimension_test", 3, "cosine");

        // 创建维度不匹配的向量
        List<Double> vector = Arrays.asList(0.1, 0.2, 0.3, 0.4); // 4维

        boolean result = vectorStore.insert("dimension_test", "id1", vector, null);
        assertFalse(result);
    }

    @Test
    void testSearch() {
        vectorStore.createCollection("search_test", 3, "cosine");

        // 插入测试数据
        List<Double> vector1 = Arrays.asList(1.0, 0.0, 0.0);
        List<Double> vector2 = Arrays.asList(0.0, 1.0, 0.0);
        List<Double> vector3 = Arrays.asList(0.0, 0.0, 1.0);

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("content", "test");

        vectorStore.insert("search_test", "id1", vector1, metadata);
        vectorStore.insert("search_test", "id2", vector2, metadata);
        vectorStore.insert("search_test", "id3", vector3, metadata);

        // 搜索最相似的向量
        List<Double> queryVector = Arrays.asList(0.9, 0.1, 0.0); // 应该最接近 vector1

        List<VectorStore.SearchResult> results = vectorStore.search("search_test", queryVector, 3, 0.0);

        assertNotNull(results);
        assertFalse(results.isEmpty());
        assertEquals("id1", results.get(0).getId()); // 最相似的应该是 id1
        assertTrue(results.get(0).getScore() > 0.9); // 相似度应该很高
    }

    @Test
    void testBatchOperations() {
        vectorStore.createCollection("batch_test", 3, "cosine");

        // 批量插入
        List<String> ids = Arrays.asList("id1", "id2", "id3");
        List<List<Double>> vectors = Arrays.asList(
                Arrays.asList(1.0, 0.0, 0.0),
                Arrays.asList(0.0, 1.0, 0.0),
                Arrays.asList(0.0, 0.0, 1.0)
        );
        List<Map<String, Object>> metadataList = Arrays.asList(
                new HashMap<String, Object>() {{ put("content", "content1"); }},
                new HashMap<String, Object>() {{ put("content", "content2"); }},
                new HashMap<String, Object>() {{ put("content", "content3"); }}
        );

        int insertedCount = vectorStore.insertBatch("batch_test", ids, vectors, metadataList);
        assertEquals(3, insertedCount);

        // 验证数量
        assertEquals(3, vectorStore.count("batch_test"));

        // 批量获取
        List<VectorRecord> records = vectorStore.getByIds("batch_test", Arrays.asList("id1", "id2"));
        assertEquals(2, records.size());

        // 批量删除
        int deletedCount = vectorStore.deleteByIds("batch_test", Arrays.asList("id1", "id2"));
        assertEquals(2, deletedCount);
        assertEquals(1, vectorStore.count("batch_test"));

        // 清空
        boolean cleared = vectorStore.clear("batch_test");
        assertTrue(cleared);
        assertEquals(0, vectorStore.count("batch_test"));
    }

    @Test
    void testUpdateMetadata() {
        vectorStore.createCollection("metadata_test", 3, "cosine");

        // 插入初始数据
        List<Double> vector = Arrays.asList(1.0, 0.0, 0.0);
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("content", "initial content");
        metadata.put("category", "test");

        vectorStore.insert("metadata_test", "id1", vector, metadata);

        // 更新元数据
        Map<String, Object> newMetadata = new HashMap<>();
        newMetadata.put("content", "updated content");
        newMetadata.put("category", "updated");
        newMetadata.put("newField", "newValue");

        boolean updated = vectorStore.updateMetadata("metadata_test", "id1", newMetadata);
        assertTrue(updated);

        // 验证更新
        VectorRecord record = vectorStore.getById("metadata_test", "id1");
        assertNotNull(record);
        assertEquals("updated content", record.getMetadata().get("content"));
        assertEquals("updated", record.getMetadata().get("category"));
        assertEquals("newValue", record.getMetadata().get("newField"));
    }
}
