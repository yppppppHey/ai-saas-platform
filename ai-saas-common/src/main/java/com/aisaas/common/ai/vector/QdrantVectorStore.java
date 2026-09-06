package com.aisaas.common.ai.vector;

import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;
import io.qdrant.client.grpc.Collections;
import io.qdrant.client.grpc.JsonWithInt;
import io.qdrant.client.grpc.Points;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Qdrant Vector Store 实现
 * 基于 Qdrant 向量数据库的向量存储实现
 */
@Slf4j
@Component
public class QdrantVectorStore implements VectorStore {

    @Value("${qdrant.host:localhost}")
    private String qdrantHost;

    @Value("${qdrant.port:6334}")
    private int qdrantPort;

    @Value("${qdrant.api-key:}")
    private String apiKey;

    @Value("${qdrant.use-tls:false}")
    private boolean useTls;

    @Value("${qdrant.timeout-seconds:30}")
    private int timeoutSeconds;

    private QdrantClient client;

    @PostConstruct
    public void init() {
        try {
            QdrantGrpcClient.Builder grpcClientBuilder = QdrantGrpcClient.newBuilder(
                    qdrantHost, qdrantPort, useTls);

            if (apiKey != null && !apiKey.isEmpty()) {
                grpcClientBuilder.withApiKey(apiKey);
            }

            client = new QdrantClient(grpcClientBuilder.build());

            // 测试连接
            client.listCollectionsAsync().get(timeoutSeconds, TimeUnit.SECONDS);
            log.info("Successfully connected to Qdrant at {}:{}", qdrantHost, qdrantPort);

        } catch (Exception e) {
            log.error("Failed to connect to Qdrant at {}:{}", qdrantHost, qdrantPort, e);
            throw new RuntimeException("Failed to initialize Qdrant connection", e);
        }
    }

    @PreDestroy
    public void close() {
        if (client != null) {
            try {
                client.close();
                log.info("Qdrant client closed");
            } catch (Exception e) {
                log.error("Error closing Qdrant client", e);
            }
        }
    }

    @Override
    public boolean createCollection(String collectionName, int dimension, String metricType) {
        try {
            // 检查集合是否已存在
            if (collectionExists(collectionName)) {
                log.warn("Collection {} already exists", collectionName);
                return false;
            }

            // 设置距离类型
            Collections.Distance distance = parseDistanceType(metricType);

            // 创建集合配置
            Collections.CreateCollection createCollection = Collections.CreateCollection.newBuilder()
                    .setCollectionName(collectionName)
                    .setVectorsConfig(
                            Collections.VectorsConfig.newBuilder()
                                    .setParams(
                                            Collections.VectorParams.newBuilder()
                                                    .setSize(dimension)
                                                    .setDistance(distance)
                                                    .build()
                                    )
                                    .build()
                    )
                    .build();

            client.createCollectionAsync(createCollection).get(timeoutSeconds, TimeUnit.SECONDS);
            log.info("Created Qdrant collection: {} with dimension: {} and metric: {}",
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
            if (!collectionExists(collectionName)) {
                log.warn("Collection {} does not exist", collectionName);
                return false;
            }

            client.deleteCollectionAsync(collectionName).get(timeoutSeconds, TimeUnit.SECONDS);
            log.info("Deleted Qdrant collection: {}", collectionName);
            return true;

        } catch (Exception e) {
            log.error("Failed to delete collection: {}", collectionName, e);
            return false;
        }
    }

    @Override
    public boolean collectionExists(String collectionName) {
        try {
            List<String> collections = client.listCollectionsAsync()
                    .get(timeoutSeconds, TimeUnit.SECONDS);
            return collections.contains(collectionName);
        } catch (Exception e) {
            log.error("Failed to check collection existence: {}", collectionName, e);
            return false;
        }
    }

    @Override
    public boolean insert(String collectionName, String id, List<Double> vector, Map<String, Object> metadata) {
        try {
            // 转换为Qdrant向量格式
            List<Float> floatVector = vector.stream()
                    .map(Double::floatValue)
                    .collect(Collectors.toList());

            // 构建payload
            Map<String, io.qdrant.client.Value> payload = new HashMap<>();
            if (metadata != null) {
                metadata.forEach((key, value) -> {
                    payload.put(key, convertToValue(value));
                });
            }

            // 创建点
            Points.PointStruct point = Points.PointStruct.newBuilder()
                    .setId(io.qdrant.client.PointIdFactory.id(id))
                    .setVectors(io.qdrant.client.VectorsFactory.vectors(floatVector))
                    .putAllPayload(payload)
                    .build();

            // 插入点
            client.upsertAsync(collectionName, List.of(point))
                    .get(timeoutSeconds, TimeUnit.SECONDS);

            return true;

        } catch (Exception e) {
            log.error("Failed to insert vector with id: {} into collection: {}", id, collectionName, e);
            return false;
        }
    }

    @Override
    public int insertBatch(String collectionName, List<String> ids, List<List<Double>> vectors,
                           List<Map<String, Object>> metadataList) {
        try {
            List<Points.PointStruct> points = new ArrayList<>();

            for (int i = 0; i < ids.size(); i++) {
                String id = ids.get(i);
                List<Double> vector = vectors.get(i);
                Map<String, Object> metadata = metadataList != null && i < metadataList.size()
                        ? metadataList.get(i)
                        : null;

                // 转换为Qdrant向量格式
                List<Float> floatVector = vector.stream()
                        .map(Double::floatValue)
                        .collect(Collectors.toList());

                // 构建payload
                Map<String, io.qdrant.client.Value> payload = new HashMap<>();
                if (metadata != null) {
                    metadata.forEach((key, value) -> {
                        payload.put(key, convertToValue(value));
                    });
                }

                // 创建点
                Points.PointStruct point = Points.PointStruct.newBuilder()
                        .setId(io.qdrant.client.PointIdFactory.id(id))
                        .setVectors(io.qdrant.client.VectorsFactory.vectors(floatVector))
                        .putAllPayload(payload)
                        .build();

                points.add(point);
            }

            // 批量插入
            client.upsertAsync(collectionName, points)
                    .get(timeoutSeconds, TimeUnit.SECONDS);

            return points.size();

        } catch (Exception e) {
            log.error("Failed to batch insert vectors into collection: {}", collectionName, e);
            return 0;
        }
    }

    @Override
    public List<SearchResult> search(String collectionName, List<Double> queryVector, int topK, double minScore) {
        return searchWithFilter(collectionName, queryVector, topK, minScore, null);
    }

    @Override
    public List<SearchResult> searchWithFilter(String collectionName, List<Double> queryVector, int topK,
                                                double minScore, Map<String, Object> filter) {
        try {
            // 转换为Qdrant向量格式
            List<Float> floatVector = queryVector.stream()
                    .map(Double::floatValue)
                    .collect(Collectors.toList());

            // 构建过滤条件
            io.qdrant.client.grpc.Points.Filter.Builder filterBuilder = Points.Filter.newBuilder();
            if (filter != null && !filter.isEmpty()) {
                filter.forEach((key, value) -> {
                    Points.FieldCondition condition = Points.FieldCondition.newBuilder()
                            .setKey(key)
                            .setMatch(Points.Match.newBuilder()
                                    .setKeyword(convertToValue(value).getStringValue())
                                    .build())
                            .build();
                    filterBuilder.addMust(condition);
                });
            }

            // 执行搜索
            List<Points.ScoredPoint> results = client.searchAsync(
                            io.qdrant.client.QueryFactory.nearest(floatVector))
                    .setCollectionName(collectionName)
                    .setLimit(topK)
                    .setFilter(filterBuilder.build())
                    .setScoreThreshold((float) minScore)
                    .execute()
                    .get(timeoutSeconds, TimeUnit.SECONDS);

            // 转换为搜索结果
            return results.stream()
                    .map(point -> new QdrantSearchResult(point))
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Failed to search collection: {}", collectionName, e);
            return new ArrayList<>();
        }
    }

    @Override
    public boolean deleteById(String collectionName, String id) {
        try {
            client.deleteAsync(collectionName, io.qdrant.client.PointIdFactory.id(id))
                    .get(timeoutSeconds, TimeUnit.SECONDS);
            return true;
        } catch (Exception e) {
            log.error("Failed to delete point with id: {} from collection: {}", id, collectionName, e);
            return false;
        }
    }

    @Override
    public int deleteByIds(String collectionName, List<String> ids) {
        int successCount = 0;
        for (String id : ids) {
            if (deleteById(collectionName, id)) {
                successCount++;
            }
        }
        return successCount;
    }

    @Override
    public VectorRecord getById(String collectionName, String id) {
        try {
            List<Points.RetrievedPoint> points = client.retrieveAsync(collectionName)
                    .setIds(io.qdrant.client.PointIdFactory.id(id))
                    .setWithPayload(true)
                    .setWithVectors(true)
                    .execute()
                    .get(timeoutSeconds, TimeUnit.SECONDS);

            if (points.isEmpty()) {
                return null;
            }

            return new QdrantVectorRecord(points.get(0));

        } catch (Exception e) {
            log.error("Failed to get point with id: {} from collection: {}", id, collectionName, e);
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
        try {
            Collections.CollectionInfo collectionInfo = client.getCollectionInfoAsync(collectionName)
                    .get(timeoutSeconds, TimeUnit.SECONDS);
            return collectionInfo.getPointsCount();
        } catch (Exception e) {
            log.error("Failed to count points in collection: {}", collectionName, e);
            return 0;
        }
    }

    @Override
    public boolean clear(String collectionName) {
        try {
            // 删除所有点
            client.deleteAsync(collectionName, new io.qdrant.client.grpc.Points.Filter())
                    .get(timeoutSeconds, TimeUnit.SECONDS);
            return true;
        } catch (Exception e) {
            log.error("Failed to clear collection: {}", collectionName, e);
            return false;
        }
    }

    @Override
    public boolean updateMetadata(String collectionName, String id, Map<String, Object> metadata) {
        try {
            Map<String, io.qdrant.client.Value> payload = new HashMap<>();
            metadata.forEach((key, value) -> {
                payload.put(key, convertToValue(value));
            });

            client.setPayloadAsync(collectionName, payload, io.qdrant.client.PointIdFactory.id(id))
                    .get(timeoutSeconds, TimeUnit.SECONDS);
            return true;
        } catch (Exception e) {
            log.error("Failed to update metadata for id: {} in collection: {}", id, collectionName, e);
            return false;
        }
    }

    // 辅助方法

    private Collections.Distance parseDistanceType(String metricType) {
        if (metricType == null) {
            return Collections.Distance.Cosine;
        }
        switch (metricType.toLowerCase()) {
            case "euclidean":
            case "l2":
                return Collections.Distance.Euclid;
            case "dot":
            case "dotproduct":
                return Collections.Distance.Dot;
            case "cosine":
            default:
                return Collections.Distance.Cosine;
        }
    }

    private io.qdrant.client.Value convertToValue(Object value) {
        if (value == null) {
            return io.qdrant.client.Value.nullValue();
        }
        if (value instanceof String) {
            return io.qdrant.client.Value.stringValue((String) value);
        }
        if (value instanceof Integer) {
            return io.qdrant.client.Value.integerValue((Integer) value);
        }
        if (value instanceof Long) {
            return io.qdrant.client.Value.integerValue((Long) value);
        }
        if (value instanceof Double) {
            return io.qdrant.client.Value.doubleValue((Double) value);
        }
        if (value instanceof Float) {
            return io.qdrant.client.Value.doubleValue((Float) value);
        }
        if (value instanceof Boolean) {
            return io.qdrant.client.Value.boolValue((Boolean) value);
        }
        if (value instanceof List) {
            io.qdrant.client.Value.ListValue.Builder listBuilder = io.qdrant.client.Value.ListValue.newBuilder();
            for (Object item : (List<?>) value) {
                listBuilder.addValues(convertToValue(item));
            }
            return io.qdrant.client.Value.listValue(listBuilder.build());
        }
        if (value instanceof Map) {
            io.qdrant.client.Value.StructValue.Builder structBuilder = io.qdrant.client.Value.StructValue.newBuilder();
            ((Map<?, ?>) value).forEach((k, v) -> {
                if (k instanceof String) {
                    structBuilder.putFields((String) k, convertToValue(v));
                }
            });
            return io.qdrant.client.Value.structValue(structBuilder.build());
        }
        return io.qdrant.client.Value.stringValue(value.toString());
    }

    // 内部结果类

    private static class QdrantSearchResult implements SearchResult {
        private final String id;
        private final List<Double> vector;
        private final double score;
        private final Map<String, Object> metadata;

        QdrantSearchResult(Points.ScoredPoint point) {
            this.id = point.getId().hasUuid()
                    ? point.getId().getUuid()
                    : String.valueOf(point.getId().getNum());

            this.vector = point.getVectors().hasVector()
                    ? point.getVectors().getVector().getDataList().stream()
                    .map(Float::doubleValue)
                    .collect(Collectors.toList())
                    : null;

            this.score = point.getScore();

            // 解析元数据
            this.metadata = new HashMap<>();
            point.getPayloadMap().forEach((key, value) -> {
                this.metadata.put(key, convertFromValue(value));
            });
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

    private static class QdrantVectorRecord extends QdrantSearchResult implements VectorRecord {
        QdrantVectorRecord(Points.RetrievedPoint point) {
            super(convertToScoredPoint(point));
        }
    }

    private static Points.ScoredPoint convertToScoredPoint(Points.RetrievedPoint point) {
        Points.ScoredPoint.Builder builder = Points.ScoredPoint.newBuilder()
                .setId(point.getId())
                .setScore(1.0f);

        if (point.hasVectors()) {
            builder.setVectors(point.getVectors());
        }

        builder.putAllPayload(point.getPayloadMap());

        return builder.build();
    }

    private static Object convertFromValue(io.qdrant.client.Value value) {
        if (value == null || value.hasNullValue()) {
            return null;
        }
        if (value.hasStringValue()) {
            return value.getStringValue();
        }
        if (value.hasIntegerValue()) {
            return value.getIntegerValue();
        }
        if (value.hasDoubleValue()) {
            return value.getDoubleValue();
        }
        if (value.hasBoolValue()) {
            return value.getBoolValue();
        }
        if (value.hasListValue()) {
            return value.getListValue().getValuesList().stream()
                    .map(QdrantVectorStore::convertFromValue)
                    .collect(Collectors.toList());
        }
        if (value.hasStructValue()) {
            Map<String, Object> map = new HashMap<>();
            value.getStructValue().getFieldsMap().forEach((k, v) -> {
                map.put(k, convertFromValue(v));
            });
            return map;
        }
        return value.toString();
    }
}
