package com.aisaas.task.constant;

import lombok.Getter;

@Getter
public enum TaskTypeEnum {
    SUMMARY("summary", "长文本总结", "对长文档进行智能摘要"),
    EXTRACT("extract", "内容提取", "从文档中提取结构化信息"),
    REPORT("report", "报告生成", "基于数据生成分析报告"),
    GENERATE("generate", "内容生成", "基于提示生成内容"),
    ANALYZE("analyze", "文档分析", "深度分析文档内容"),
    TRANSLATE("translate", "文档翻译", "多语言文档翻译"),
    CLASSIFY("classify", "内容分类", "对内容进行智能分类"),
    QA("qa", "问答生成", "基于文档生成问答对");

    private final String code;
    private final String name;
    private final String description;

    TaskTypeEnum(String code, String name, String description) {
        this.code = code;
        this.name = name;
        this.description = description;
    }

    public static TaskTypeEnum fromCode(String code) {
        for (TaskTypeEnum type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        return null;
    }
}
