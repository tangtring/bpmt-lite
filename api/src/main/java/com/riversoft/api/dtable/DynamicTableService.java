package com.riversoft.api.dtable;

import com.riversoft.api.http.ApiException;
import com.riversoft.core.BeanFactory;
import com.riversoft.core.db.DataCondition;
import com.riversoft.core.db.DataPackage;
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
import java.util.Map;
import java.util.Set;

public class DynamicTableService {
    private static final String DEFAULT_SORT = "createDate";
    private static final String DEFAULT_ORDER = "desc";

    private final TableService tableService;

    public DynamicTableService() {
        this(BeanFactory.getInstance().getBean(TableService.class));
    }

    public DynamicTableService(TableService tableService) {
        this.tableService = tableService;
    }

    public DataPackage list(int start, int limit) {
        return list(start, limit, null, null);
    }

    public DataPackage list(int start, int limit, String sort, String order) {
        String normalizedSort = normalizeSort(sort);
        String normalizedOrder = normalizeOrder(normalizedSort, order);
        DataCondition condition = new DataCondition().setOrderBy(normalizedSort, normalizedOrder);
        return queryTables(start, limit, condition.toEntity());
    }

    public TbTable detail(String name) {
        DynamicTableValidator.validateName(name);
        String tableName = StringUtils.trim(name);
        TbTable table = findDynamicTable(tableName);
        if (table == null) {
            throw new ApiException(404, "DYNAMIC_TABLE_NOT_FOUND", "动态表不存在。");
        }
        return table;
    }

    public TbTable create(DynamicTableRequest request) {
        DynamicTableValidator.validateForCreate(request);
        String tableName = StringUtils.trim(request.getName());
        if (physicalTableExists(tableName)) {
            throw new ApiException(409, "DYNAMIC_TABLE_ALREADY_EXISTS", "表[" + tableName + "]已存在。");
        }
        TbTable table = toTbTable(request);
        executeCreateTable(table);
        return detail(tableName);
    }

    public TbTable update(String name, DynamicTableRequest request) {
        if (request == null) {
            throw new ApiException(400, "DYNAMIC_TABLE_NAME_REQUIRED", "表名不能为空。");
        }
        String tableName = StringUtils.trim(name);
        detail(tableName);
        request.setName(tableName);
        DynamicTableValidator.validateForCreate(request);
        TbTable table = toTbTable(request);
        executeUpdateTable(table, false);
        return detail(tableName);
    }

    public void syncDdl(String name) {
        String tableName = StringUtils.trim(name);
        detail(tableName);
        executeSyncTable(tableName, false);
    }

    public static String normalizeSort(String sort) {
        String value = StringUtils.defaultIfBlank(sort, DEFAULT_SORT);
        value = StringUtils.trim(value);
        if ("name".equals(value)
                || "description".equals(value)
                || "createDate".equals(value)
                || "updateDate".equals(value)
                || "cacheFlag".equals(value)) {
            return value;
        }
        throw new ApiException(400, "API_INVALID_PARAMETER", "排序字段不支持。");
    }

    public static String normalizeOrder(String sort, String order) {
        if (StringUtils.isBlank(order)) {
            return DEFAULT_SORT.equals(sort) ? DEFAULT_ORDER : "asc";
        }
        String value = StringUtils.trim(order).toLowerCase();
        if ("asc".equals(value) || "desc".equals(value)) {
            return value;
        }
        throw new ApiException(400, "API_INVALID_PARAMETER", "排序方向仅支持 asc 或 desc。");
    }

    protected DataPackage queryTables(int start, int limit, Map<String, ?> queryMap) {
        return tableService.queryPackage(TbTable.class.getName(), start, limit, queryMap);
    }

    protected TbTable findDynamicTable(String tableName) {
        return (TbTable) tableService.findByPk(TbTable.class.getName(), tableName);
    }

    protected boolean physicalTableExists(String tableName) {
        return tableService.checkTableExists(tableName);
    }

    protected void executeCreateTable(TbTable table) {
        tableService.executeCreateTable(table);
    }

    protected void executeUpdateTable(TbTable table, boolean unsafeMode) {
        tableService.executeUpdateTable(table, unsafeMode);
    }

    protected void executeSyncTable(String tableName, boolean unsafeMode) {
        tableService.executeSyncTable(tableName, unsafeMode);
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
