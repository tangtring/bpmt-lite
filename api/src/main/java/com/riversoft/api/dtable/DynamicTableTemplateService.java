package com.riversoft.api.dtable;

import com.riversoft.api.http.ApiException;
import com.riversoft.platform.db.DTableLoader;

import java.util.Set;

public class DynamicTableTemplateService {
    public Set<String> listTemplates() {
        return DTableLoader.getInstance().getNames();
    }

    public void assertTemplateExists(String templateName) {
        if (!listTemplates().contains(templateName)) {
            throw new ApiException(404, "DYNAMIC_TABLE_TEMPLATE_NOT_FOUND", "动态表模板不存在。");
        }
    }
}
