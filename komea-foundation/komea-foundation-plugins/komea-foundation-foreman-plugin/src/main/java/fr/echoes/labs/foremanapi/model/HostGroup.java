package fr.echoes.labs.foremanapi.model;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class HostGroup {

	@JsonProperty("name") 				public String name;
	@JsonProperty("id") 				public String id;
	@JsonProperty("domain_id") 			public String domain_id;
	@JsonProperty("environment_id") 	public Integer environmentId;
	@JsonProperty("ancestry") 			public String ancestry;
	@JsonProperty("title") 				public String title;
//	@JsonProperty("parameters")       	public ArrayList<Map<String, String>> parameters;
	@JsonProperty("puppetclass_ids") 	public Integer[] puppetclassIds;
/*	@JsonProperty("subnet_name") 		public String subnet_name = "KSF ADS";*/
	@JsonProperty("ptable_id")          public String ptable_id;
	@JsonProperty("subnet_id")          public String subnet_id;
	@JsonProperty("medium_id")          public String medium_id;
	@JsonProperty("realm_id")           public String realm_id;
	@JsonProperty("architecture_id")    public String architecture_id;
	@JsonProperty("operatingsystem_id") public String operatingsystem_id;



	@Override
	public String toString() {

		return "HostGroup [name=" + this.name +
				         ",id=" + this.id +
						 ",domain_id=" + this.domain_id +
						 ",environmentId=" + this.environmentId +
						 ",ancestry=" + this.ancestry +
						 ",title=" + this.title +
						 ",puppetclassIds=" + this.puppetclassIds +
						 ",ptable_id=" + this.ptable_id +
						 ",subnet_id=" + this.subnet_id +
						 ",medium_id=" + this.medium_id +
						 ",realm_id=" + this.realm_id +
						 ",architecture_id=" + this.architecture_id +
						 ",operatingsystem_id=" + this.operatingsystem_id + "]";
	}
}
