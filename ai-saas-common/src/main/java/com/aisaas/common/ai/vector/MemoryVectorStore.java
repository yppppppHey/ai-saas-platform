package com.aisaas.common.ai.vector;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 内存向量存储实现
 * 适用于测试和轻量级应用场景
 */
@Slf4j
@Component
public class MemoryVectorStore implements VectorStore {

    // 集合存储: collectionName -> Collection
    private final Map<String, Collection> collections = new ConcurrentHashMap<>();

    // 集合信息
    @Data
    private static class Collection {
        private final String name;
        private final int dimension;
        private final String metricType;
        private final Map<String, VectorData> data = new ConcurrentHashMap<>();

        public Collection(String name, int dimension, String metricType) {
            this.name = name;
            this.dimension = dimension;
            this.metricType = metricType != null ? metricType : "cosine";
        }
    }

    // 向量数据
    @Data
    private static class VectorData {
        private final String id;
        private final List<Double> vector;
        private final Map<String, Object> metadata;
        private final long timestamp;

        public VectorData(String id, List<Double> vector, Map<String, Object> metadata) {
            this.id = id;
            this.vector = new ArrayList<>(vector);
            this.metadata = metadata != null ? new HashMap<>(metadata) : new HashMap<>();
            this.timestamp = System.currentTimeMillis();
        }
    }

    @Override
    public boolean createCollection(String collectionName, int dimension, String metricType) {
        try {
            if (collections.containsKey(collectionName)) {
                log.warn("Collection {} already exists", collectionName);
                return false;
            }

            Collection collection = new Collection(collectionName, dimension, metricType);
            collections.put(collectionName, collection);

            log.info("Created collection: {} with dimension: {} and metric: {}",
                    collectionName, dimension, metricType);
            return true;

        } catch (Exception e) {
            log.error("Failed to create collection: {}", collectionName, e);
            return false;
        }
    }

    @Override
    public boolean deleteCollection(String collectionName) {
        try {
            Collection removed = collections.remove(collectionName);
            if (removed != null) {
                log.info("Deleted collection: {}", collectionName);
                return true;
            }
            return false;

        } catch (Exception e) {
            log.error("Failed to delete collection: {}", collectionName, e);
            return false;
        }
    }

    @Override
    public boolean collectionExists(String collectionName) {
        return collections.containsKey(collectionName);
    }

    @Override
    public boolean insert(String collectionName, String id, List<Double> vector, Map<String, Object> metadata) {
        try {
            Collection collection = collections.get(collectionName);
            if (collection == null) {
                log.error("Collection {} does not exist", collectionName);
                return false;
            }

            // 验证向量维度
            if (vector.size() != collection.getDimension()) {
                log.error("Vector dimension mismatch: expected {}, got {}",
                        collection.getDimension(), vector.size());
                return false;
            }

            VectorData data = new VectorData(id, vector, metadata);
            collection.getData().put(id, data);

            return true;

        } catch (Exception e) {
            log.error("Failed to insert data: {}", id, e);
            return false;
        }
    }

    @Override
    public int insertBatch(String collectionName, List<String> ids, List<List<Double>> vectors,
                           List<Map<String, Object>> metadataList) {
        if (ids == null || vectors == null || ids.size() != vectors.size()) {
            return 0;
        }

        int successCount = 0;
        for (int i = 0; i < ids.size(); i++) {
            String id = ids.get(i);
            List<Double> vector = vectors.get(i);
            Map<String, Object> metadata = metadataList != null && i < metadataList.size()
                    ? metadataList.get(i) : null;

            if (insert(collectionName, id, vector, metadata)) {
                successCount++;
            }
        }

        return successCount;
    }

    @Override
    public List<SearchResult> search(String collectionName, List<Double> queryVector, int topK, double minScore) {
        return searchWithFilter(collectionName, queryVector, topK, minScore, null);
    }

    @Override
    public List<SearchResult> searchWithFilter(String collectionName, List<Double> queryVector, int topK,
                                               double minScore, Map<String, Object> filter) {
        try {
            Collection collection = collections.get(collectionName);
            if (collection == null) {
                log.error("Collection {} does not exist", collectionName);
                return Collections.emptyList();
            }

            if (queryVector.size() != collection.getDimension()) {
                log.error("Query vector dimension mismatch: expected {}, got {}",
                        collection.getDimension(), queryVector.size());
                return Collections.emptyList();
            }

            // 计算相似度并过滤
            List<SearchResultImpl> results = collection.getData().values().stream()
                    .filter(data -> filter == null || matchesFilter(data.getMetadata(), filter))
                    .map(data -> {
                        double score = calculateSimilarity(queryVector, data.getVector(), collection.getMetricType());
                        return new SearchResultImpl(data.getId(), data.getVector(), score, data.getMetadata());
                    })
                    .filter(result -> result.getScore() >= minScore)
                    .sorted((r1, r2) -> Double.compare(r2.getScore(), r1.getScore()))
                    .limit(topK)
                    .collect(Collectors.toList());

            return new ArrayList<>(results);

        } catch (Exception e) {
            log.error("Failed to search collection: {}", collectionName, e);
            return Collections.emptyList();
        }
    }

    /**
     * 检查元数据是否匹配过滤条件
     */
    private boolean matchesFilter(Map<String, Object> metadata, Map<String, Object> filter) {
        for (Map.Entry<String, Object> entry : filter.entrySet()) {
            String key = entry.getKey();
            Object expectedValue = entry.getValue();
            Object actualValue = metadata.get(key);

            if (actualValue == null) {
                return false;
            }

            // 支持基本相等判断
            if (!actualValue.equals(expectedValue)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 计算相似度
     */
    private double calculateSimilarity(List<Double> v1, List<Double> v2, String metricType) {
        switch (metricType.toLowerCase()) {
            case "euclidean":
                return 1 / (1 + euclideanDistance(v1, v2));
            case "dot":
                return dotProduct(v1, v2);
            case "cosine":
            default:
                return cosineSimilarity(v1, v2);
        }
    }

    /**
     * 余弦相似度
     */
    private double cosineSimilarity(List<Double> v1, List<Double> v2) {
        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;

        for (int i = 0; i < v1.size(); i++) {
            double d1 = v1.get(i);
            double d2 = v2.get(i);
            dotProduct += d1 * d2;
            norm1 += d1 * d1;
            norm2 += d2 * d2;
        }

        if (norm1 == 0.0 || norm2 == 0.0) {
            return 0.0;
        }

        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }

    /**
     * 点积
     */
    private double dotProduct(List<Double> v1, List<Double> v2) {
        double sum = 0.0;
        for (int i = 0; i < v1.size(); i++) {
            sum += v1.get(i) * v2.get(i);
        }
        return sum;
    }

    /**
     * 欧几里得距离
     */
    private double euclideanDistance(List<Double> v1, List<Double> v2) {
        double sum = 0.0;
        for (int i = 0; i < v1.size(); i++) {
            double diff = v1.get(i) - v2.get(i);
            sum += diff * diff;
        }
        return Math.sqrt(sum);
    }

    @Override
    public boolean deleteById(String collectionName, String id) {
        try {
            Collection collection = collections.get(collectionName);
            if (collection == null) {
                return false;
            }
            return collection.getData().remove(id) != null;
        } catch (Exception e) {
            log.error("Failed to delete data: {}", id, e);
            return false;
        }
    }

    @Override
    public int deleteByIds(String collectionName, List<String> ids) {
        int count = 0;
        for (String id : ids) {
            if (deleteById(collectionName, id)) {
                count++;
            }
        }
        return count;
    }

    @Override
    public VectorRecord getById(String collectionName, String id) {
        try {
            Collection collection = collections.get(collectionName);
            if (collection == null) {
                return null;
            }
            VectorData data = collection.getData().get(id);
            if (data == null) {
                return null;
            }
            return new VectorRecordImpl(data.getId(), data.getVector(), 1.0, data.getMetadata());
        } catch (Exception e) {
            log.error("Failed to get data: {}", id, e);
            return null;
        }
    }

    @Override
    public List<VectorRecord> getByIds(String collectionName, List<String> ids) {
        return ids.stream()
                .map(id -> getById(collectionName, id))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public long count(String collectionName) {
        Collection collection = collections.get(collectionName);
        if (collection == null) {
            return 0;
        }
        return collection.getData().size();
    }

    @Override
    public boolean clear(String collectionName) {
        try {
            Collection collection = collections.get(collectionName);
            if (collection == null) {
                return false;
            }
            collection.getData().clear();
            return true;
        } catch (Exception e) {
            log.error("Failed to clear collection: {}", collectionName, e);
            return false;
        }
    }

    @Override
    public boolean updateMetadata(String collectionName, String id, Map<String, Object> metadata) {
        try {
            Collection collection = collections.get(collectionName);
            if (collection == null) {
                return false;
            }
            VectorData data = collection.getData().get(id);
            if (data == null) {
                return false;
            }
            data.getMetadata().putAll(metadata);
            return true;
        } catch (Exception e) {
            log.error("Failed to update metadata: {}", id, e);
            return false;
        }
    }

    // 内部实现类

    private static class SearchResultImpl implements SearchResult {
        private final String id;
        private final List<Double> vector;
        private final double score;
        private final Map<String, Object> metadata;

        SearchResultImpl(String id, List<Double> vector, double score, Map<String, Object> metadata) {
            this.id = id;
            this.vector = vector;
            this.score = score;
            this.metadata = metadata != null ? new HashMap<>(metadata) : new HashMap<>();
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public List<Double> getVector() {
            return vector;
        }

        @Override
        public double getScore() {
            return score;
        }

        @Override
        public Map<String, Object> getMetadata() {
            return Collections.unmodifiableMap(metadata);
        }
    }

    private static class VectorRecordImpl extends SearchResultImpl implements VectorRecord {
        VectorRecordImpl(String id, List<Double> vector, double score, Map<String, Object> metadata) {
            super(id, vector, score, metadata);
        }
    }
}
