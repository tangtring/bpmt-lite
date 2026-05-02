package com.riversoft.api.dtable;

import com.riversoft.api.http.ApiException;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;

public class DynamicTableValidatorTest {
    @Test(expected = ApiException.class)
    public void rejectsSystemTablePrefix() {
        DynamicTableRequest request = new DynamicTableRequest();
        request.setName("TB_BAD");
        request.setColumns(Arrays.asList(primaryKey("ID")));
        DynamicTableValidator.validateForCreate(request);
    }

    @Test(expected = ApiException.class)
    public void rejectsMissingPrimaryKey() {
        DynamicTableRequest request = new DynamicTableRequest();
        request.setName("RV_API_TEST");
        request.setColumns(Arrays.asList(column("NAME")));
        DynamicTableValidator.validateForCreate(request);
    }

    @Test
    public void acceptsNormalTableWithPrimaryKey() {
        DynamicTableRequest request = new DynamicTableRequest();
        request.setName("RV_API_TEST");
        request.setColumns(Arrays.asList(primaryKey("ID"), column("NAME")));
        DynamicTableValidator.validateForCreate(request);
        assertEquals("RV_API_TEST", request.getName());
    }

    @Test
    public void rejectsDuplicatedColumnNameIgnoringCase() {
        DynamicTableRequest request = new DynamicTableRequest();
        request.setName("RV_API_TEST");
        request.setColumns(Arrays.asList(primaryKey("ID"), column("name"), column("NAME")));
        try {
            DynamicTableValidator.validateForCreate(request);
        } catch (ApiException e) {
            assertEquals("DYNAMIC_TABLE_COLUMN_DUPLICATED", e.getCode());
            return;
        }
        throw new AssertionError("Expected duplicated column name rejection");
    }

    private DynamicTableRequest.Column column(String name) {
        DynamicTableRequest.Column column = new DynamicTableRequest.Column();
        column.setName(name);
        column.setType("String");
        column.setTotalSize(100);
        column.setRequired(false);
        return column;
    }

    private DynamicTableRequest.Column primaryKey(String name) {
        DynamicTableRequest.Column column = column(name);
        column.setPrimaryKey(true);
        column.setRequired(true);
        return column;
    }
}
