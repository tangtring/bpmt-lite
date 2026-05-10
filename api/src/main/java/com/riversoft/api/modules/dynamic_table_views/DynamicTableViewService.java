package com.riversoft.api.modules.dynamic_table_views;

import com.riversoft.api.http.ApiJson;
import com.riversoft.platform.po.VwUrl;
import org.apache.commons.lang3.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DynamicTableViewService {
    private static final String DYN_VIEW_CLASS = "dyn";
    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 100;

    private final DynamicTableViewRepository repository;
    private final DynamicTableViewMapper mapper;
    private final DynamicTableViewDefaults defaults;
    private final DynamicTableViewPermissionService permissionService;
    private final DynamicTableViewValidator validator;

    public DynamicTableViewService() {
        this(new OrmDynamicTableViewRepository());
    }

    DynamicTableViewService(DynamicTableViewRepository repository) {
        this.repository = repository;
        this.mapper = new DynamicTableViewMapper();
        this.defaults = new DynamicTableViewDefaults();
        this.permissionService = new DynamicTableViewPermissionService();
        this.validator = new DynamicTableViewValidator(repository);
    }

    public Map<String, Object> list(String start, String limit) {
        int parsedStart = parseStart(start);
        int parsedLimit = parseLimit(limit);
        List<VwUrl> urls = repository.listDynUrls(parsedStart, parsedLimit);
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("start", Integer.valueOf(parsedStart));
        result.put("limit", Integer.valueOf(parsedLimit));
        result.put("totalRecord", Integer.valueOf(repository.countDynUrls()));
        result.put("items", listItems(urls));
        return result;
    }

    public Map<String, Object> export(String viewKey) {
        VwUrl url = requireDynUrl(viewKey);
        Map<String, Object> table = repository.findTable(viewKey);
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("snapshot", mapper.toSnapshot(url, table));
        return result;
    }

    public Map<String, Object> validate(DynamicTableViewSnapshot snapshot) {
        DynamicTableViewSnapshot normalized = defaults.normalize(snapshot, repository);
        permissionService.apply(normalized.getViewKey(), null, normalized);
        DynamicTableViewValidationResult result = validator.validate(normalized);
        return DynamicTableViewResponse.validation(result.isValid(), result.getWarnings(), result.getErrors(),
                result.getNormalizedSnapshot());
    }

    public Map<String, Object> create(DynamicTableViewSnapshot snapshot, boolean dryRun) {
        DynamicTableViewSnapshot normalized = defaults.normalizeForCreate(snapshot, repository);
        if (repository.findUrl(normalized.getViewKey()) != null) {
            throw DynamicTableViewErrors.alreadyExists(normalized.getViewKey());
        }
        DynamicTableViewResponse.WritePlan plan = permissionService.apply(normalized.getViewKey(), null, normalized);
        plan.setDryRun(dryRun);
        plan.getCreates().add("VW_URL");
        plan.getCreates().add("VW_DYN_TABLE");
        DynamicTableViewValidationResult validation = validator.validate(normalized);
        if (!validation.isValid()) {
            throw DynamicTableViewErrors.invalidSnapshot(validation);
        }
        if (!dryRun) {
            repository.createViewConfig(toUrl(normalized, null), mapper.toTableMap(normalized), plan);
            repository.flushAndClearViewCache(normalized.getViewKey());
        }
        return DynamicTableViewResponse.write(normalized, validation.getWarnings(), plan);
    }

    public Map<String, Object> replace(String viewKey, DynamicTableViewSnapshot snapshot, boolean dryRun) {
        VwUrl existingUrl = requireDynUrl(viewKey);
        DynamicTableViewSnapshot oldSnapshot = mapper.toSnapshot(existingUrl, repository.findTable(viewKey));
        DynamicTableViewSnapshot normalized = defaults.normalize(snapshot, repository);
        normalized.setViewKey(viewKey);
        DynamicTableViewResponse.WritePlan plan = permissionService.apply(viewKey, oldSnapshot, normalized);
        plan.setDryRun(dryRun);
        plan.getUpdates().add("VW_URL");
        plan.getUpdates().add("VW_DYN_TABLE");
        plan.getDeletes().add("VW_DYN_TABLE_CHILDREN");
        DynamicTableViewValidationResult validation = validator.validate(normalized);
        if (!validation.isValid()) {
            throw DynamicTableViewErrors.invalidSnapshot(validation);
        }
        if (!dryRun) {
            repository.replaceViewConfig(toUrl(normalized, existingUrl), mapper.toTableMap(normalized), plan);
            repository.flushAndClearViewCache(viewKey);
        }
        return DynamicTableViewResponse.write(normalized, validation.getWarnings(), plan);
    }

    public Map<String, Object> patch(String viewKey,
                                     DynamicTableViewSection section,
                                     Object body,
                                     boolean dryRun) {
        VwUrl existingUrl = requireDynUrl(viewKey);
        DynamicTableViewSnapshot oldSnapshot = mapper.toSnapshot(existingUrl, repository.findTable(viewKey));
        DynamicTableViewSnapshot current = mapper.toSnapshot(existingUrl, repository.findTable(viewKey));
        applySection(current, section, body);
        DynamicTableViewSnapshot normalized = defaults.normalize(current, repository);
        normalized.setViewKey(viewKey);
        DynamicTableViewResponse.WritePlan plan = permissionService.apply(viewKey, oldSnapshot, normalized);
        plan.setDryRun(dryRun);
        plan.getUpdates().add("VW_DYN_TABLE_" + section.name());
        plan.getUpdatedSections().add(section.value());
        DynamicTableViewValidationResult validation = validator.validate(normalized);
        if (!validation.isValid()) {
            throw DynamicTableViewErrors.invalidSnapshot(validation);
        }
        if (!dryRun) {
            repository.patchViewConfig(toUrl(normalized, existingUrl), section, mapper.toTableMap(normalized), plan);
            repository.flushAndClearViewCache(viewKey);
        }
        return DynamicTableViewResponse.write(normalized, validation.getWarnings(), plan);
    }

    public Map<String, Object> delete(String viewKey, String confirmViewKey) {
        if (!StringUtils.equals(viewKey, confirmViewKey)) {
            throw DynamicTableViewErrors.confirmRequired();
        }
        VwUrl existingUrl = requireDynUrl(viewKey);
        DynamicTableViewSnapshot oldSnapshot = mapper.toSnapshot(existingUrl, repository.findTable(viewKey));
        DynamicTableViewResponse.WritePlan plan = permissionService.apply(viewKey, oldSnapshot, null);
        plan.getDeletes().add("VW_URL");
        plan.getDeletes().add("VW_DYN_TABLE");
        plan.getDeletes().add("VW_DYN_TABLE_CHILDREN");
        repository.removeViewConfig(viewKey, plan);
        repository.flushAndClearViewCache(viewKey);
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("viewKey", viewKey);
        result.put("deleted", Boolean.TRUE);
        result.put("businessTableDeleted", Boolean.FALSE);
        result.put("businessDataDeleted", Boolean.FALSE);
        return result;
    }

    private VwUrl requireDynUrl(String viewKey) {
        VwUrl url = repository.findUrl(viewKey);
        if (url == null) {
            throw DynamicTableViewErrors.notFound(viewKey);
        }
        if (!DYN_VIEW_CLASS.equals(url.getViewClass())) {
            throw DynamicTableViewErrors.notDyn(viewKey);
        }
        return url;
    }

    private List<Map<String, Object>> listItems(List<VwUrl> urls) {
        if (urls == null || urls.isEmpty()) {
            return Collections.emptyList();
        }
        java.util.ArrayList<Map<String, Object>> items = new java.util.ArrayList<Map<String, Object>>();
        for (VwUrl url : urls) {
            Map<String, Object> item = new LinkedHashMap<String, Object>();
            item.put("viewKey", url.getViewKey());
            item.put("description", url.getDescription());
            item.put("url", url.getUrl());
            item.put("loginRequired", Boolean.valueOf(url.getLoginType() == null || url.getLoginType().intValue() == 1));
            items.add(item);
        }
        return items;
    }

    private VwUrl toUrl(DynamicTableViewSnapshot snapshot, VwUrl existing) {
        Date now = new Date();
        VwUrl url = existing == null ? new VwUrl() : existing;
        if (url.getCreateDate() == null) {
            url.setCreateDate(now);
        }
        url.setUpdateDate(now);
        url.setViewKey(snapshot.getViewKey());
        url.setViewClass(DYN_VIEW_CLASS);
        url.setDescription(snapshot.getDescription());
        url.setLoginType(snapshot.isLoginRequired() ? Integer.valueOf(1) : Integer.valueOf(0));
        url.setLockFlag(Integer.valueOf(0));
        if (StringUtils.isBlank(url.getCreateUid())) {
            url.setCreateUid("admin");
        }
        return url;
    }

    private void applySection(DynamicTableViewSnapshot snapshot, DynamicTableViewSection section, Object body) {
        switch (section) {
            case BASE:
                snapshot.setBase(convert(body, DynamicTableViewSnapshot.Base.class));
                return;
            case FIELDS:
                snapshot.setFields(convert(body, DynamicTableViewSnapshot.Fields.class));
                return;
            case QUERIES:
                snapshot.setQueries(convert(body, DynamicTableViewSnapshot.Queries.class));
                return;
            case LIMITS:
                snapshot.setLimits(Arrays.asList(convert(body, DynamicTableViewSnapshot.Limit[].class)));
                return;
            case PROCESSORS:
                snapshot.setProcessors(convert(body, DynamicTableViewSnapshot.Processors.class));
                return;
            case VARIABLES:
                snapshot.setVariables(convert(body, DynamicTableViewSnapshot.Variables.class));
                return;
            case SUBVIEWS:
                snapshot.setSubviews(convert(body, DynamicTableViewSnapshot.Subviews.class));
                return;
            case BUTTONS:
                snapshot.setButtons(convert(body, DynamicTableViewSnapshot.Buttons.class));
                return;
            case WEIXIN:
                snapshot.setWeixin(convert(body, DynamicTableViewSnapshot.Weixin.class));
                return;
            case SCRIPTS:
                snapshot.setScripts(convert(body, DynamicTableViewSnapshot.Scripts.class));
                return;
            default:
                throw DynamicTableViewErrors.invalidSnapshot("不支持的动态表视图区块：" + section);
        }
    }

    private <T> T convert(Object value, Class<T> type) {
        try {
            return ApiJson.fromJson(new ByteArrayInputStream(ApiJson.toJson(value).getBytes("UTF-8")), type);
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }

    private int parseStart(String value) {
        if (StringUtils.isBlank(value)) {
            return 0;
        }
        int start = parseInt(value);
        if (start < 0) {
            throw new com.riversoft.api.http.ApiException(400, "API_INVALID_PARAMETER", "分页参数 start 不能小于 0。");
        }
        return start;
    }

    private int parseLimit(String value) {
        if (StringUtils.isBlank(value)) {
            return DEFAULT_LIMIT;
        }
        int limit = parseInt(value);
        if (limit < 1 || limit > MAX_LIMIT) {
            throw new com.riversoft.api.http.ApiException(400, "API_INVALID_PARAMETER", "分页参数 limit 范围是 1 到 100。");
        }
        return limit;
    }

    private int parseInt(String value) {
        try {
            return Integer.parseInt(StringUtils.trim(value));
        } catch (NumberFormatException e) {
            throw new com.riversoft.api.http.ApiException(400, "API_INVALID_PARAMETER", "分页参数无效。");
        }
    }
}
