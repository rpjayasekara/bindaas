package edu.emory.cci.bindaas.framework.model;

import java.util.List;
import java.util.Map;

import com.google.gson.JsonObject;
import com.google.gson.annotations.Expose;

public class QueryEndpoint extends Entity {

	
	
	@Expose private String queryTemplate;
	@Expose private JsonObject metaData;
	@Expose private List<String> tags;
	@Expose private Map<String,BindVariable> bindVariables;
	@Expose private Stage stage;
	@Expose private JsonObject outputFormat;
	@Expose private ModifierEntry queryModifiers;
	@Expose private ModifierEntry queryResultModifiers;
	@Expose private String description;
	
	
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public String getQueryTemplate() {
		return queryTemplate;
	}
	public void setQueryTemplate(String queryTemplate) {
		this.queryTemplate = queryTemplate;
	}
	public JsonObject getMetaData() {
		return metaData;
	}
	public void setMetaData(JsonObject metaData) {
		this.metaData = metaData;
	}
	public List<String> getTags() {
		return tags;
	}
	public void setTags(List<String> tags) {
		this.tags = tags;
	}
	public Map<String, BindVariable> getBindVariables() {
		return bindVariables;
	}
	public void setBindVariables(Map<String, BindVariable> bindVariables) {
		this.bindVariables = bindVariables;
	}
	public Stage getStage() {
		return stage;
	}
	public void setStage(Stage stage) {
		this.stage = stage;
	}
	public JsonObject getOutputFormat() {
		return outputFormat;
	}
	public void setOutputFormat(JsonObject outputFormat) {
		this.outputFormat = outputFormat;
	}
	public ModifierEntry getQueryModifiers() {
		return queryModifiers;
	}
	public void setQueryModifiers(ModifierEntry queryModifiers) {
		this.queryModifiers = queryModifiers;
	}
	public ModifierEntry getQueryResultModifiers() {
		return queryResultModifiers;
	}
	public void setQueryResultModifiers(ModifierEntry queryResultModifiers) {
		this.queryResultModifiers = queryResultModifiers;
	}
	
	
	
	
}
