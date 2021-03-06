package com.synaptix.toast.test.runtime.resource;

import org.junit.Assert;

import com.synaptix.toast.core.adapter.ActionAdapterKind;
import com.synaptix.toast.core.annotation.Action;
import com.synaptix.toast.core.annotation.ActionAdapter;
import com.synaptix.toast.core.report.SuccessResult;
import com.synaptix.toast.dao.domain.api.test.ITestResult;

@ActionAdapter(value = ActionAdapterKind.service, name = "xml-service-adapter")
public class XmlAdapterExample {

	@Action(action = "Intégrer {{com.synaptix.toast.test.runtime.resource.ProjetFlux:value:xml}}", description =
			"Intégrer xml")
	public ITestResult integrerXml(ProjetFlux fluxFromXml) {
		return new SuccessResult();
	}
	
	@Action(id="integrate2",
			action="Integrate {{value:xml}}", 
			description="Intégrer xml avec type non defini")
	public ITestResult integrateNameLessXml(ProjetFlux fluxFromXml){
		return new SuccessResult();
	}
	
	@Action(id="integrate3",
			action="Integrate {{value:xml}} as {{value}}", 
			description="Intégrer xml avec type non defini")
	public ITestResult integrateXmlAsVar(ProjetFlux fluxFromXml, String variable){
		return new SuccessResult();
	}

	@Action(
			id = "compare1",
			action = "{{value}} == {{value}}",
			description = "comparer une valeur à une autre")
	public void compare(String var1, String var2) {

	}

	@Action(id = "echo", action = "echo {{value}}", description = "afficher valeur autre")
	public String returnString(String var1) {
		return var1;
	}

	@Action(
			id = "assertValue",
			action = "assert {{value}}",
			description = "faire une assertion")
	public void assertTest(String var1) {
		Assert.assertNotNull(var1);
	}
	
	@Action(
			id = "errorValue",
			action = "assert not {{value}}",
			description = "faire une assertion")
	public void assertErrorTest(String var1) {
		Assert.assertNull(var1);
	}

	@Action(
			id = "sayHello",
			action = "Say hello",
			description = "dire bonjour")
	public String sayHello() {
		return "Hello";
	}
}
