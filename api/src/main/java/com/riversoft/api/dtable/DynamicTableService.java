package com.riversoft.api.dtable;

import com.riversoft.api.http.ApiException;
import com.riversoft.core.BeanFactory;
import com.riversoft.core.db.DataCondition;
import com.riversoft.core.db.DataPackage;
import com.riversoft.core.db.JdbcService;
import com.riversoft.platform.db.Types;
import com.riversoft.platform.po.TbColumn;
import com.riversoft.platform.po.TbIndex;
import com.riversoft.platform.po.TbIndexedColumn;
import com.riversoft.platform.po.TbTable;
import com.riversoft.platform.service.TableService;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class DynamicTableService {
    private final TableService tableService;

    public DynamicTableService() {
        this(BeanFactory.getInstance().getBean(TableService.class));
    }

    public DynamicTableService(TableService tableService) {
        this.tableService = tableService;
    }

    public DataPackage list(int start, int limit) {
        return tableService.queryPackage(TbTable.class.getName(), start, limit, new DataCondition().toEntity());
    }

    public TbTable detail(String name) {
        DynamicTableValidator.validateName(name);
        String tableName = StringUtils.trim(name);
        TbTable table = (TbTable) tableService.findByPk(TbTable.class.getName(), tableName);
        if (table == null) {
            throw new ApiException(404, "DYNAMIC_TABLE_NOT_FOUND", "动态表不存在。");
        }
        return table;
    }

    public TbTable create(DynamicTableRequest request) {
        DynamicTableValidator.validateForCreate(request);
        String tableName = StringUtils.trim(request.getName());
        if (tableService.checkTableExists(tableName)) {
            throw new ApiException(409, "DYNAMIC_TABLE_ALREADY_EXISTS", "表[" + tableName + "]已存在。");
        }
        TbTable table = toTbTable(request);
        tableService.executeCreateTable(table);
        return detail(tableName);
    }

    public TbTable update(String name, DynamicTableRequest request) {
        if (request == null) {
            throw new ApiException(400, "DYNAMIC_TABLE_NAME_REQUIRED", "表名不能为空。");
        }
        String tableName = StringUtils.trim(name);
        request.setName(tableName);
        DynamicTableValidator.validateForCreate(request);
        TbTable table = toTbTable(request);
        Number count = (Number) JdbcService.getInstance().findSQL("select count(1) T from " + table.getName()).get("T");
        boolean isSafe = count == null || count.intValue() < 1;
        tableService.executeUpdateTable(table, isSafe);
        return detail(tableName);
    }

    public void syncDdl(String name) {
        detail(name);
        tableService.executeSyncTable(StringUtils.trim(name), true);
    }

    public static TbTable toTbTable(DynamicTableRequest request) {
        DynamicTableValidator.validateForCreate(request);
        TbTable table = new TbTable();
        table.setName(StringUtils.trim(request.getName()));
        table.setDescription(request.getDescription());
        table.setCacheFlag(request.getCacheFlag() == null ? 0 : request.getCacheFlag());
        table.setTbColumns(toTbColumns(request));
        table.setTbIndexes(toTbIndexes(request));
        return table;
    }

    public static DynamicTableResponse toResponse(TbTable table) {
        if (table == null) {
            return null;
        }
        DynamicTableResponse response = new DynamicTableResponse();
        response.setName(table.getName());
        response.setDescription(table.getDescription());
        response.setCacheFlag(table.getCacheFlag());
        response.setColumns(toResponseColumns(table.getTbColumns()));
        response.setIndexes(toResponseIndexes(table.getTbIndexes()));
        return response;
    }

    private static Set<TbColumn> toTbColumns(DynamicTableRequest request) {
        Set<TbColumn> columns = new LinkedHashSet<TbColumn>();
        int sort = 0;
        for (DynamicTableRequest.Column source : request.getColumns()) {
            TbColumn column = new TbColumn();
            column.setTableName(StringUtils.trim(request.getName()));
            column.setName(StringUtils.trim(source.getName()));
            column.setDescription(source.getDescription());
            column.setMappedTypeCode(resolveTypeCode(source.getType()));
            column.setScale(source.getScale() == null ? 0 : source.getScale());
            column.setTotalSize(source.getTotalSize() == null ? 100 : source.getTotalSize());
            column.setPrimaryKey(source.isPrimaryKey());
            column.setAutoIncrement(source.isAutoIncrement());
            column.setRequired(source.isRequired());
            column.setDefaultValue(StringUtils.defaultIfBlank(source.getDefaultValue(), null));
            column.setMemo(source.getMemo());
            column.setSort(sort++);
            columns.add(column);
        }
        return columns;
    }

    private static Set<TbIndex> toTbIndexes(DynamicTableRequest request) {
        if (request.getIndexes() == null || request.getIndexes().isEmpty()) {
            return null;
        }
        Set<TbIndex> indexes = new LinkedHashSet<TbIndex>();
        for (DynamicTableRequest.Index source : request.getIndexes()) {
            if (source == null || StringUtils.isBlank(source.getName())) {
                continue;
            }
            TbIndex index = new TbIndex();
            index.setTableName(StringUtils.trim(request.getName()));
            index.setName(StringUtils.trim(source.getName()));
            index.setUnique(false);
            Set<TbIndexedColumn> indexedColumns = new LinkedHashSet<TbIndexedColumn>();
            int position = 0;
            if (source.getColumns() != null) {
                for (String columnName : source.getColumns()) {
                    if (StringUtils.isBlank(columnName)) {
                        continue;
                    }
                    TbIndexedColumn indexedColumn = new TbIndexedColumn();
                    indexedColumn.setTableName(StringUtils.trim(request.getName()));
                    indexedColumn.setIndexName(index.getName());
                    indexedColumn.setName(StringUtils.trim(columnName));
                    indexedColumn.setOrdinalPosition(position++);
                    indexedColumn.setPrimaryKey(false);
                    indexedColumns.add(indexedColumn);
                }
            }
            index.setIndexedColumns(indexedColumns);
            indexes.add(index);
        }
        return indexes;
    }

    private static int resolveTypeCode(String type) {
        String typeName = StringUtils.defaultIfBlank(type, "String");
        try {
            return ((Integer) Types.valueOf(typeName).getCode()).intValue();
        } catch (IllegalArgumentException e) {
            throw new ApiException(422, "DYNAMIC_TABLE_COLUMN_TYPE_INVALID", "字段类型不支持。");
        }
    }

    private static List<DynamicTableRequest.Column> toResponseColumns(Set<TbColumn> sourceColumns) {
        List<DynamicTableRequest.Column> columns = new ArrayList<DynamicTableRequest.Column>();
        if (sourceColumns == null) {
            return columns;
        }
        for (TbColumn source : sourceColumns) {
            DynamicTableRequest.Column column = new DynamicTableRequest.Column();
            column.setName(source.getName());
            column.setDescription(source.getDescription());
            column.setType(Types.findByCode(source.getMappedTypeCode()).name());
            column.setTotalSize(source.getTotalSize());
            column.setScale(source.getScale());
            column.setPrimaryKey(source.isPrimaryKey());
            column.setAutoIncrement(source.isAutoIncrement());
            column.setRequired(source.isRequired());
            column.setDefaultValue(source.getDefaultValue());
            column.setMemo(source.getMemo());
            columns.add(column);
        }
        return columns;
    }

    private static List<DynamicTableRequest.Index> toResponseIndexes(Set<TbIndex> sourceIndexes) {
        List<DynamicTableRequest.Index> indexes = new ArrayList<DynamicTableRequest.Index>();
        if (sourceIndexes == null) {
            return indexes;
        }
        for (TbIndex source : sourceIndexes) {
            DynamicTableRequest.Index index = new DynamicTableRequest.Index();
            index.setName(source.getName());
            List<String> columns = new ArrayList<String>();
            if (source.getIndexedColumns() != null) {
                for (TbIndexedColumn indexedColumn : source.getIndexedColumns()) {
                    columns.add(indexedColumn.getName());
                }
            }
            index.setColumns(columns);
            indexes.add(index);
        }
        return indexes;
    }
}
