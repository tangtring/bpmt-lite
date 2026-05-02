package com.riversoft.api.modules.dynamic_tables;

import java.util.List;

public class DynamicTableResponse {
    private String name;
    private String description;
    private Integer cacheFlag;
    private List<DynamicTableRequest.Column> columns;
    private List<DynamicTableRequest.Index> indexes;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getCacheFlag() {
        return cacheFlag;
    }

    public void setCacheFlag(Integer cacheFlag) {
        this.cacheFlag = cacheFlag;
    }

    public List<DynamicTableRequest.Column> getColumns() {
        return columns;
    }

    public void setColumns(List<DynamicTableRequest.Column> columns) {
        this.columns = columns;
    }

    public List<DynamicTableRequest.Index> getIndexes() {
        return indexes;
    }

    public void setIndexes(List<DynamicTableRequest.Index> indexes) {
        this.indexes = indexes;
    }
}
