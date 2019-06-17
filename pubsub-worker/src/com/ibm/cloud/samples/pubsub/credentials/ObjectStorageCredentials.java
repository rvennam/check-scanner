package com.ibm.cloud.samples.pubsub.credentials;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({ "apikey", "endpoints", "iam_apikey_description", "iam_apikey_name", "iam_role_crn",
		"iam_serviceid_crn", "resource_instance_id" })
public class ObjectStorageCredentials {

	@JsonProperty("apikey")
	private String apikey;
	@JsonProperty("endpoints")
	private String endpoints;
	@JsonProperty("iam_apikey_description")
	private String iamApikeyDescription;
	@JsonProperty("iam_apikey_name")
	private String iamApikeyName;
	@JsonProperty("iam_role_crn")
	private String iamRoleCrn;
	@JsonProperty("iam_serviceid_crn")
	private String iamServiceidCrn;
	@JsonProperty("resource_instance_id")
	private String resourceInstanceId;
	@JsonIgnore
	private Map<String, Object> additionalProperties = new HashMap<String, Object>();

	@JsonProperty("apikey")
	public String getApikey() {
		return apikey;
	}

	@JsonProperty("apikey")
	public void setApikey(String apikey) {
		this.apikey = apikey;
	}

	@JsonProperty("endpoints")
	public String getEndpoints() {
		return endpoints;
	}

	@JsonProperty("endpoints")
	public void setEndpoints(String endpoints) {
		this.endpoints = endpoints;
	}

	@JsonProperty("iam_apikey_description")
	public String getIamApikeyDescription() {
		return iamApikeyDescription;
	}

	@JsonProperty("iam_apikey_description")
	public void setIamApikeyDescription(String iamApikeyDescription) {
		this.iamApikeyDescription = iamApikeyDescription;
	}

	@JsonProperty("iam_apikey_name")
	public String getIamApikeyName() {
		return iamApikeyName;
	}

	@JsonProperty("iam_apikey_name")
	public void setIamApikeyName(String iamApikeyName) {
		this.iamApikeyName = iamApikeyName;
	}

	@JsonProperty("iam_role_crn")
	public String getIamRoleCrn() {
		return iamRoleCrn;
	}

	@JsonProperty("iam_role_crn")
	public void setIamRoleCrn(String iamRoleCrn) {
		this.iamRoleCrn = iamRoleCrn;
	}

	@JsonProperty("iam_serviceid_crn")
	public String getIamServiceidCrn() {
		return iamServiceidCrn;
	}

	@JsonProperty("iam_serviceid_crn")
	public void setIamServiceidCrn(String iamServiceidCrn) {
		this.iamServiceidCrn = iamServiceidCrn;
	}

	@JsonProperty("resource_instance_id")
	public String getResourceInstanceId() {
		return resourceInstanceId;
	}

	@JsonProperty("resource_instance_id")
	public void setResourceInstanceId(String resourceInstanceId) {
		this.resourceInstanceId = resourceInstanceId;
	}

	@JsonAnyGetter
	public Map<String, Object> getAdditionalProperties() {
		return this.additionalProperties;
	}

	@JsonAnySetter
	public void setAdditionalProperty(String name, Object value) {
		this.additionalProperties.put(name, value);
	}

}