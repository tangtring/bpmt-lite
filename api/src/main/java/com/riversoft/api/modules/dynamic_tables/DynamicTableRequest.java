package com.riversoft.api.modules.dynamic_tables;

import java.util.ArrayList;
import java.util.List;

public class DynamicTableRequest {
    private String name;
    private String description;
    private Integer cacheFlag;
    private List<Column> columns = new ArrayList<Column>();
    private List<Index> indexes = new ArrayList<Index>();

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

    public List<Column> getColumns() {
        return columns;
    }

    public void setColumns(List<Column> columns) {
        this.columns = columns;
    }

    public List<Index> getIndexes() {
        return indexes;
    }

    public void setIndexes(List<Index> indexes) {
        this.indexes = indexes;
    }

    public static class Column {
        private String name;
        private String description;
        private String type;
        private Integer totalSize;
        private Integer scale;
        private boolean primaryKey;
        private boolean autoIncrement;
        private boolean required;
        private String defaultValue;
        private String memo;

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

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public Integer getTotalSize() {
            return totalSize;
        }

        public void setTotalSize(Integer totalSize) {
            this.totalSize = totalSize;
        }

        public Integer getScale() {
            return scale;
        }

        public void setScale(Integer scale) {
            this.scale = scale;
        }

        public boolean isPrimaryKey() {
            return primaryKey;
        }

        public void setPrimaryKey(boolean primaryKey) {
            this.primaryKey = primaryKey;
        }

        public boolean isAutoIncrement() {
            return autoIncrement;
        }

        public void setAutoIncrement(boolean autoIncrement) {
            this.autoIncrement = autoIncrement;
        }

        public boolean isRequired() {
            return required;
        }

        public void setRequired(boolean required) {
            this.required = required;
        }

        public String getDefaultValue() {
            return defaultValue;
        }

        public void setDefaultValue(String defaultValue) {
            this.defaultValue = defaultValue;
        }

        public String getMemo() {
            return memo;
        }

        public void setMemo(String memo) {
            this.memo = memo;
        }
    }

    public static class Index {
        private String name;
        private List<String> columns = new ArrayList<String>();

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public List<String> getColumns() {
            return columns;
        }

        public void setColumns(List<String> columns) {
            this.columns = columns;
        }
    }
}
