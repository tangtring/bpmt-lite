package com.riversoft.platform.web;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.activiti.editor.constants.ModelDataJsonConstants;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.repository.Model;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.PNGTranscoder;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.riversoft.flow.FlowFactory;

@SuppressWarnings("serial")
public class ModelerServiceServlet extends HttpServlet implements ModelDataJsonConstants {
	private static final Logger logger = LoggerFactory.getLogger(ModelerServiceServlet.class);
	private static final String UTF_8 = "UTF-8";

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String path = normalizePath(request.getPathInfo());
		try {
			if ("/editor".equals(path)) {
				writeClasspathResource(response, "editor.html", "application/xhtml+xml;charset=UTF-8");
			} else if ("/editor/plugins".equals(path)) {
				writeClasspathResource(response, "plugins.xml", "application/xml;charset=UTF-8");
			} else if (path.startsWith("/editor_stencilset") || path.startsWith("/editor/stencilset")) {
				writeClasspathResource(response, "stencilset.json", "application/json;charset=UTF-8");
			} else if (path.startsWith("/model/") && path.endsWith("/json")) {
				writeModelJson(response, extractModelId(path, "/json"));
			} else {
				response.sendError(HttpServletResponse.SC_NOT_FOUND);
			}
		} catch (Exception e) {
			logger.error("Error handling modeler GET request: {}", path, e);
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
	}

	@Override
	protected void doPut(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String path = normalizePath(request.getPathInfo());
		try {
			if (path.startsWith("/model/") && path.endsWith("/save")) {
				saveModel(request, response, extractModelId(path, "/save"));
			} else {
				response.sendError(HttpServletResponse.SC_NOT_FOUND);
			}
		} catch (Exception e) {
			logger.error("Error handling modeler PUT request: {}", path, e);
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String path = normalizePath(request.getPathInfo());
		try {
			if ("/model/new".equals(path)) {
				createModel(request, response);
			} else {
				doPut(request, response);
			}
		} catch (Exception e) {
			logger.error("Error handling modeler POST request: {}", path, e);
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
	}

	private String normalizePath(String pathInfo) {
		return pathInfo == null || pathInfo.length() == 0 ? "/" : pathInfo;
	}

	private String extractModelId(String path, String suffix) {
		return path.substring("/model/".length(), path.length() - suffix.length());
	}

	private void writeClasspathResource(HttpServletResponse response, String resourceName, String contentType)
			throws IOException {
		InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourceName);
		if (inputStream == null) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}

		response.setContentType(contentType);
		try (InputStream in = inputStream; ServletOutputStream out = response.getOutputStream()) {
			byte[] buffer = new byte[8192];
			int len;
			while ((len = in.read(buffer)) != -1) {
				out.write(buffer, 0, len);
			}
		}
	}

	private void writeModelJson(HttpServletResponse response, String modelId) throws IOException {
		RepositoryService repositoryService = FlowFactory.getRepositoryService();
		Model model = repositoryService.getModel(modelId);
		if (model == null) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}

		ObjectNode modelNode;
		if (StringUtils.isNotEmpty(model.getMetaInfo())) {
			modelNode = (ObjectNode) objectMapper.readTree(model.getMetaInfo());
		} else {
			modelNode = objectMapper.createObjectNode();
			modelNode.put(MODEL_NAME, model.getName());
		}
		modelNode.put(MODEL_ID, model.getId());
		byte[] editorSource = repositoryService.getModelEditorSource(model.getId());
		if (editorSource != null) {
			ObjectNode editorJsonNode = (ObjectNode) objectMapper.readTree(new String(editorSource, UTF_8));
			modelNode.put("model", editorJsonNode);
		}

		response.setContentType("application/json;charset=UTF-8");
		response.getWriter().write(modelNode.toString());
	}

	private void saveModel(HttpServletRequest request, HttpServletResponse response, String modelId) throws Exception {
		RepositoryService repositoryService = FlowFactory.getRepositoryService();
		Model model = repositoryService.getModel(modelId);
		if (model == null) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}
		saveModelData(request, repositoryService, model);

		response.setContentType("application/json;charset=UTF-8");
		response.getWriter().write("{\"modelId\":\"" + model.getId() + "\"}");
	}

	private void createModel(HttpServletRequest request, HttpServletResponse response) throws Exception {
		RepositoryService repositoryService = FlowFactory.getRepositoryService();
		Model model = repositoryService.newModel();
		String name = valueOrDefault(request.getParameter("name"), "untitled");
		String description = valueOrDefault(request.getParameter("description"), "");

		ObjectNode modelJson = objectMapper.createObjectNode();
		modelJson.put(MODEL_NAME, name);
		modelJson.put(MODEL_DESCRIPTION, description);
		modelJson.put(MODEL_REVISION, 1);
		model.setMetaInfo(modelJson.toString());
		model.setName(name);
		model.setKey(name);
		repositoryService.saveModel(model);
		saveModelData(request, repositoryService, model);

		response.setContentType("application/json;charset=UTF-8");
		response.getWriter().write("{\"modelId\":\"" + model.getId() + "\",\"href\":\"model/" + model.getId() + "\"}");
	}

	private void saveModelData(HttpServletRequest request, RepositoryService repositoryService, Model model)
			throws Exception {
		Map<String, String> bodyParameters = readBodyParameters(request);
		ObjectNode modelJson = StringUtils.isNotEmpty(model.getMetaInfo())
				? (ObjectNode) objectMapper.readTree(model.getMetaInfo())
				: objectMapper.createObjectNode();
		modelJson.put(MODEL_NAME, valueOrDefault(requestValue(request, bodyParameters, "name"), model.getName()));
		modelJson.put(MODEL_DESCRIPTION, valueOrDefault(requestValue(request, bodyParameters, "description"), ""));
		model.setMetaInfo(modelJson.toString());
		model.setName(valueOrDefault(requestValue(request, bodyParameters, "name"), model.getName()));
		repositoryService.saveModel(model);

		String jsonXml = valueOrDefault(requestValue(request, bodyParameters, "json_xml"), "{}");
		repositoryService.addModelEditorSource(model.getId(), jsonXml.getBytes(UTF_8));

		String svgXml = requestValue(request, bodyParameters, "svg_xml");
		if (StringUtils.isNotEmpty(svgXml)) {
			repositoryService.addModelEditorSourceExtra(model.getId(), transcodeSvgToPng(svgXml));
		}
	}

	private String requestValue(HttpServletRequest request, Map<String, String> bodyParameters, String key) {
		String value = request.getParameter(key);
		return value != null ? value : bodyParameters.get(key);
	}

	private Map<String, String> readBodyParameters(HttpServletRequest request) throws IOException {
		Map<String, String> parameters = new HashMap<String, String>();
		if (!"PUT".equalsIgnoreCase(request.getMethod())
				|| !StringUtils.startsWithIgnoreCase(request.getContentType(), "application/x-www-form-urlencoded")) {
			return parameters;
		}

		StringBuilder body = new StringBuilder();
		String line;
		while ((line = request.getReader().readLine()) != null) {
			body.append(line);
		}
		for (String pair : body.toString().split("&")) {
			if (pair.length() == 0) {
				continue;
			}
			int separator = pair.indexOf('=');
			String key = separator >= 0 ? pair.substring(0, separator) : pair;
			String value = separator >= 0 ? pair.substring(separator + 1) : "";
			parameters.put(URLDecoder.decode(key, UTF_8), URLDecoder.decode(value, UTF_8));
		}
		return parameters;
	}

	private byte[] transcodeSvgToPng(String svgXml) throws Exception {
		ByteArrayInputStream svgStream = new ByteArrayInputStream(svgXml.getBytes(UTF_8));
		TranscoderInput input = new TranscoderInput(svgStream);
		PNGTranscoder transcoder = new PNGTranscoder();
		ByteArrayOutputStream outStream = new ByteArrayOutputStream();
		TranscoderOutput output = new TranscoderOutput(outStream);
		transcoder.transcode(input, output);
		outStream.close();
		return outStream.toByteArray();
	}

	private String valueOrDefault(String value, String defaultValue) {
		return StringUtils.isEmpty(value) ? defaultValue : value;
	}
}
