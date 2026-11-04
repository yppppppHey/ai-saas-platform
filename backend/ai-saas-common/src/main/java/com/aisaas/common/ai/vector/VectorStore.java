package com.aisaas.common.ai.vector;

import java.util.List;
import java.util.Map;

/**
 * 向量存储接口
 * 定义向量数据的存储、检索和管理操作
 */
public interface VectorStore {

    /**
     * 创建集合
     *
     * @param collectionName 集合名称
     * @param dimension      向量维度
     * @param metricType     度量类型（如 cosine, euclidean）
     * @return 是否创建成功
     */
    boolean createCollection(String collectionName, int dimension, String metricType);

    /**
     * 删除集合
     *
     * @param collectionName 集合名称
     * @return 是否删除成功
     */
    boolean deleteCollection(String collectionName);

    /**
     * 检查集合是否存在
     *
     * @param collectionName 集合名称
     * @return 是否存在
     */
    boolean collectionExists(String collectionName);

    /**
     * 插入向量数据
     *
     * @param collectionName 集合名称
     * @param id             数据ID
     * @param vector         向量数据
     * @param metadata       元数据
     * @return 是否插入成功
     */
    boolean insert(String collectionName, String id, List<Double> vector, Map<String, Object> metadata);

    /**
     * 批量插入向量数据
     *
     * @param collectionName 集合名称
     * @param ids            数据ID列表
     * @param vectors        向量数据列表
     * @param metadataList   元数据列表
     * @return 插入成功的数量
     */
    int insertBatch(String collectionName, List<String> ids, List<List<Double>> vectors,
                    List<Map<String, Object>> metadataList);

    /**
     * 向量相似度搜索
     *
     * @param collectionName 集合名称
     * @param queryVector    查询向量
     * @param topK           返回结果数量
     * @param minScore       最小相似度分数
     * @return 搜索结果列表
     */
    List<SearchResult> search(String collectionName, List<Double> queryVector, int topK, double minScore);

    /**
     * 带过滤条件的向量搜索
     *
     * @param collectionName 集合名称
     * @param queryVector    查询向量
     * @param topK           返回结果数量
     * @param minScore       最小相似度分数
     * @param filter         过滤条件
     * @return 搜索结果列表
     */
    List<SearchResult> searchWithFilter(String collectionName, List<Double> queryVector, int topK,
                                        double minScore, Map<String, Object> filter);

    /**
     * 根据ID删除数据
     *
     * @param collectionName 集合名称
     * @param id             数据ID
     * @return 是否删除成功
     */
    boolean deleteById(String collectionName, String id);

    /**
     * 批量删除数据
     *
     * @param collectionName 集合名称
     * @param ids            数据ID列表
     * @return 删除成功的数量
     */
    int deleteByIds(String collectionName, List<String> ids);

    /**
     * 根据ID获取数据
     *
     * @param collectionName 集合名称
     * @param id             数据ID
     * @return 向量记录
     */
    VectorRecord getById(String collectionName, String id);

    /**
     * 批量获取数据
     *
     * @param collectionName 集合名称
     * @param ids            数据ID列表
     * @return 向量记录列表
     */
    List<VectorRecord> getByIds(String collectionName, List<String> ids);

    /**
     * 获取集合中的数据总数
     *
     * @param collectionName 集合名称
     * @return 数据总数
     */
    long count(String collectionName);

    /**
     * 清空集合中的所有数据
     *
     * @param collectionName 集合名称
     * @return 是否清空成功
     */
    boolean clear(String collectionName);

    /**
     * 更新元数据
     *
     * @param collectionName 集合名称
     * @param id             数据ID
     * @param metadata       新的元数据
     * @return 是否更新成功
     */
    boolean updateMetadata(String collectionName, String id, Map<String, Object> metadata);

    /**
     * 搜索返回类型
     */
    interface SearchResult {
        String getId();

        List<Double> getVector();

        double getScore();

        Map<String, Object> getMetadata();
    }

    /**
     * 向量记录
     */
    interface VectorRecord extends SearchResult {
    }
}
